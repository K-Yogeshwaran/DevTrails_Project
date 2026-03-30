package com.devtrails.backend.claims;



import com.devtrails.backend.policy.PolicyDTO;
import com.devtrails.backend.policy.PolicyService;
import com.devtrails.backend.shared.TriggerEngineClient;
import com.devtrails.backend.worker.Worker;
import com.devtrails.backend.worker.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final WorkerRepository workerRepository;
    private final PolicyService policyService;
    private final TriggerEngineClient triggerEngineClient;

    private static final Map<String, Double> PERSONA_SENSITIVITY = Map.of(
            "food",      1.20,
            "grocery",   1.00,
            "ecommerce", 0.85
    );

    private static final Map<String, Double> DISRUPTED_HOURS = Map.of(
            "rainfall",         4.0,  // heavy rain stops deliveries for ~4 hours
            "aqi",              6.0,  // poor AQI affects the whole working day
            "heat",             5.0,  // extreme heat, midday hours lost
            "platform_downtime",2.0,  // platform outage affects ~2 hrs on avg
            "curfew",           8.0   // curfew can affect the whole day
    );


    private static final double FRAUD_THRESHOLD = 0.70;

    @Scheduled(fixedDelay = 60000)
    public void pollAndProcessTriggers() {
        if (!triggerEngineClient.isHealthy()) {
            log.warn("Trigger engine unreachable — skipping poll cycle");
            return;
        }

        List<Map<String, Object>> pendingTriggers =
                triggerEngineClient.fetchPendingTriggers();

        if (pendingTriggers.isEmpty()) {
            return;
        }

        log.info("Processing {} pending trigger events", pendingTriggers.size());

        for (Map<String, Object> event : pendingTriggers) {
            try {
                processTriggerEvent(event);
            } catch (Exception e) {
                log.error("Failed to process trigger event {}: {}",
                        event.get("event_id"), e.getMessage());
            }
        }
    }

    @Transactional
    public void processTriggerEvent(Map<String, Object> event) {
        String workerId    = (String) event.get("worker_id");
        String triggerType = (String) event.get("trigger_type");
        String eventId     = (String) event.get("event_id");
        String zoneId      = (String) event.get("zone_id");

        log.info("Processing event: {} | Worker: {} | Type: {}",
                eventId, workerId, triggerType);


        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd   = dayStart.plusDays(1);

        boolean isDuplicate = claimRepository.existsDuplicateClaim(
                workerId, triggerType, dayStart, dayEnd
        );

        if (isDuplicate) {
            log.info("Duplicate claim rejected: {} already claimed {} today",
                    workerId, triggerType);
            return;
        }

        PolicyDTO.CoverageCheck coverage =
                policyService.checkCoverage(workerId, LocalDate.now());

        if (!coverage.isCovered()) {
            log.info("No active policy for worker {}: {}", workerId, coverage.getReason());
            return;
        }

        Worker worker = workerRepository.findByWorkerId(workerId)
                .orElse(null);

        if (worker == null) {
            log.warn("Worker {} not found in database", workerId);
            return;
        }

        double fraudScore = calculateFraudScore(worker, triggerType, zoneId);


        BigDecimal payoutAmount = calculatePayout(worker, triggerType);

        if (payoutAmount.compareTo(coverage.getCoverageRemaining()) > 0) {
            payoutAmount = coverage.getCoverageRemaining();
            log.info("Payout capped at remaining coverage: ₹{}", payoutAmount);
        }

        String claimStatus;
        if (fraudScore > FRAUD_THRESHOLD) {
            claimStatus = "flagged";  // needs manual review
            log.warn("Claim flagged for fraud review: worker={} score={}",
                    workerId, fraudScore);
        } else {
            claimStatus = "approved";
        }

        String claimId = "CLM-" +
                workerId.replace("GS-", "") + "-" +
                System.currentTimeMillis();

        BigDecimal triggerValue = BigDecimal.ZERO;
        Object rawValue = event.get("value");
        if (rawValue != null) {
            try {
                triggerValue = new BigDecimal(rawValue.toString())
                        .setScale(2, RoundingMode.HALF_UP);
            } catch (Exception e) {
                // value might be a map (platform_downtime) — use 0
                triggerValue = BigDecimal.ZERO;
            }
        }

        double disruptedHoursVal = DISRUPTED_HOURS.getOrDefault(triggerType, 4.0);

        Claim claim = new Claim();
        claim.setClaimId(claimId);
        claim.setWorkerId(workerId);
        claim.setPolicyNumber(coverage.getPolicyNumber());
        claim.setTriggerType(triggerType);
        claim.setTriggerValue(triggerValue);
        claim.setZoneId(zoneId);
        claim.setDisruptedHours(BigDecimal.valueOf(disruptedHoursVal));
        claim.setPayoutAmount(payoutAmount);
        claim.setFraudScore(BigDecimal.valueOf(fraudScore));
        claim.setStatus(claimStatus);
        claim.setTriggeredAt(LocalDateTime.now());
        claim.setProcessedAt(LocalDateTime.now());

        claimRepository.save(claim);

        // ── STEP 9: DEDUCT COVERAGE ────────────────────────
        // Only deduct if approved — flagged claims wait for manual review
        if ("approved".equals(claimStatus)) {
            policyService.deductCoverage(coverage.getPolicyNumber(), payoutAmount);
            log.info("Claim APPROVED: {} | Worker: {} | Payout: ₹{}",
                    claimId, workerId, payoutAmount);
        }
    }

    private double calculateFraudScore(Worker worker, String triggerType, String zoneId) {
        double score = 0.0;

        // Rule 1: Zone mismatch
        // Worker's registered zone should match the disrupted zone
        if (!worker.getZoneId().equals(zoneId)) {
            score += 0.50;  // heavy penalty — strong fraud signal
            log.warn("Zone mismatch: worker zone={} event zone={}",
                    worker.getZoneId(), zoneId);
        }

        // Rule 2: Velocity check
        // More than 5 claims in last 7 days = suspicious
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        long recentClaimCount =
                claimRepository.countRecentClaims(worker.getWorkerId(), oneWeekAgo);

        if (recentClaimCount > 5) {
            score += 0.30;  // too many claims recently
            log.warn("High claim velocity: {} claims in 7 days for {}",
                    recentClaimCount, worker.getWorkerId());
        } else if (recentClaimCount > 3) {
            score += 0.15;  // moderately high
        }

        // Rule 3: Inactive worker claiming
        // Worker must be active to receive payouts
        if (!worker.getIsActive()) {
            score += 0.50;
        }

        // Rule 4: Implausible earnings
        // Daily earnings > ₹3000 is unusual for a gig worker
        if (worker.getDailyEarnings() > 3000) {
            score += 0.20;
        }

        // Cap at 1.0
        return Math.min(score, 1.0);
    }

    // ── PAYOUT CALCULATION ────────────────────────────────
    // Formula: Hourly Rate × Disrupted Hours × Persona Factor
    private BigDecimal calculatePayout(Worker worker, String triggerType) {
        // Hourly rate
        double hourlyRate = (double) worker.getDailyEarnings() / worker.getActiveHours();

        // Disrupted hours for this trigger type
        double disruptedHours = DISRUPTED_HOURS.getOrDefault(triggerType, 4.0);

        // Persona sensitivity multiplier
        double personaFactor = PERSONA_SENSITIVITY.getOrDefault(
                worker.getPersona(), 1.0
        );

        // Payout = hourlyRate × disruptedHours × personaFactor
        double payout = hourlyRate * disruptedHours * personaFactor;

        // Round to 2 decimal places
        return BigDecimal.valueOf(payout).setScale(2, RoundingMode.HALF_UP);
    }

    // ── GET WORKER CLAIMS ─────────────────────────────────
    public List<ClaimDTO.ClaimSummary> getWorkerClaims(String workerId) {
        return claimRepository
                .findByWorkerIdOrderByCreatedAtDesc(workerId)
                .stream()
                .map(c -> new ClaimDTO.ClaimSummary(
                        c.getClaimId(), c.getTriggerType(),
                        c.getPayoutAmount(), c.getStatus(), c.getTriggeredAt()
                ))
                .collect(Collectors.toList());
    }

    // ── GET CLAIM BY ID ───────────────────────────────────
    public ClaimDTO.ClaimResponse getClaimById(String claimId) {
        Claim c = claimRepository.findByClaimId(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found: " + claimId));
        return buildResponse(c, "Claim details");
    }

    // ── GET ANALYTICS ─────────────────────────────────────
    public ClaimDTO.Analytics getAnalytics() {
        List<Claim> all         = claimRepository.findAll();
        long total              = all.size();
        long approved           = all.stream().filter(c -> "approved".equals(c.getStatus())).count();
        long rejected           = all.stream().filter(c -> "rejected".equals(c.getStatus())).count();
        long flagged            = all.stream().filter(c -> "flagged".equals(c.getStatus())).count();
        BigDecimal totalPaidOut = claimRepository.totalPaidOut();
        if (totalPaidOut == null) totalPaidOut = BigDecimal.ZERO;

        double approvalRate = total > 0 ? (double) approved / total * 100 : 0;
        double fraudRate    = total > 0 ? (double) flagged  / total * 100 : 0;

        return new ClaimDTO.Analytics(
                total, approved, rejected, flagged,
                totalPaidOut, approvalRate, fraudRate
        );
    }

    // ── HELPER ────────────────────────────────────────────
    private ClaimDTO.ClaimResponse buildResponse(Claim c, String message) {
        return new ClaimDTO.ClaimResponse(
                c.getClaimId(), c.getWorkerId(), c.getPolicyNumber(),
                c.getTriggerType(), c.getTriggerValue(), c.getZoneId(),
                c.getDisruptedHours(), c.getPayoutAmount(), c.getFraudScore(),
                c.getStatus(), c.getTriggeredAt(), c.getProcessedAt(), message
        );
    }
}