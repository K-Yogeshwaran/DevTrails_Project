package com.devtrails.backend.triggerevent;

import com.devtrails.backend.claimlog.ClaimProcessingLog;
import com.devtrails.backend.claimlog.ClaimProcessingLogRepository;
import com.devtrails.backend.claims.Claim;
import com.devtrails.backend.claims.ClaimRepository;
import com.devtrails.backend.wallet.WalletService;
import com.devtrails.backend.policy.PayoutCalculatorClient;
import com.devtrails.backend.policy.Policy;
import com.devtrails.backend.policy.PolicyRepository;
import com.devtrails.backend.policy.PolicyService;
import com.devtrails.backend.policy.PolicyDTO;
import com.devtrails.backend.worker.Worker;
import com.devtrails.backend.worker.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TriggerListenerService {

    private final TriggerEventRepository       triggerEventRepo;
    private final WorkerRepository             workerRepo;
    private final PolicyService                policyService;
    private final PolicyRepository             policyRepo;
    private final ClaimRepository              claimRepo;
    private final ClaimProcessingLogRepository logRepo;
    private final PayoutCalculatorClient       payoutClient;
    private final WalletService                walletService;

    private static final double FRAUD_THRESHOLD = 0.70;

    @Scheduled(fixedDelay = 30000)
    public void processResolvedTriggers() {
        List<TriggerEvent> resolved = triggerEventRepo.findAllResolved();
        if (resolved.isEmpty()) return;

        log.info("TriggerListener: {} resolved events to process", resolved.size());
        for (TriggerEvent event : resolved) {
            try {
                processResolvedEvent(event);
                event.setStatus("claims_processed");
                triggerEventRepo.save(event);
                log.info("Event {} marked as claims_processed", event.getEventId());
            } catch (Exception e) {
                log.error("Failed to process resolved event {}: {}", event.getEventId(), e.getMessage(), e);
            }
        }
    }

    public void processResolvedEvent(TriggerEvent event) {
        String affectedRaw = event.getAffectedWorkerIds();
        if (affectedRaw == null || affectedRaw.isBlank()) {
            log.info("No affected workers for event {} - skipping", event.getEventId());
            return;
        }
        for (String workerId : Arrays.asList(affectedRaw.split(","))) {
            try {
                createClaimForWorker(workerId.trim(), event);
            } catch (Exception e) {
                log.error("Claim creation failed for worker {}: {}", workerId, e.getMessage());
            }
        }
    }

    @Transactional
    public void createClaimForWorker(String workerId, TriggerEvent event) {

        String claimId = "CLM-" + workerId.replace("GS-", "") + "-" + System.currentTimeMillis();

        // Stage 1 - Queued
        saveLog(claimId, "queued", "done",
                "Claim queued | Trigger: " + event.getTriggerType()
                + " in " + event.getZoneName()
                + " | Duration: " + formatHours(event.getDisruptedHours()));
        sleep(1500);

        // Stage 2 - Duplicate check
        saveLog(claimId, "duplicate_check", "processing", "Checking for duplicate claims today...");
        sleep(1200);
        boolean isDuplicate = claimRepo.existsDuplicateClaim(
                workerId, event.getTriggerType(),
                event.getStartedAt(), event.getStartedAt().plusDays(1));
        if (isDuplicate) {
            saveLog(claimId, "duplicate_check", "failed",
                    "Duplicate rejected - already claimed " + event.getTriggerType() + " today");
            return;
        }
        saveLog(claimId, "duplicate_check", "done", "No duplicate found - proceeding");
        sleep(1000);

        // Stage 3 - Policy check
        saveLog(claimId, "policy_check", "processing", "Verifying active policy coverage...");
        sleep(1500);
        PolicyDTO.CoverageCheck coverage = policyService.checkCoverage(workerId, LocalDate.now());
        if (!coverage.isCovered()) {
            saveLog(claimId, "policy_check", "failed",
                    "No active policy found for this week - claim rejected");
            return;
        }
        saveLog(claimId, "policy_check", "done",
                "Active policy: " + coverage.getPolicyNumber()
                + " | Remaining: Rs." + coverage.getCoverageRemaining());

        Worker worker = workerRepo.findByWorkerId(workerId).orElse(null);
        if (worker == null) {
            saveLog(claimId, "policy_check", "failed", "Worker record not found");
            return;
        }
        sleep(1000);

        String season = policyRepo.findActivePolicy(workerId, LocalDate.now())
                .map(Policy::getSeason).orElse("summer");

        double disruptedHours = event.getDisruptedHours() != null
                ? Math.max(event.getDisruptedHours().doubleValue(), 0.5)
                : 4.0;

        // Stage 4 - ML calculation
        saveLog(claimId, "ml_calculation", "processing", "Running XGBoost payout model...");
        sleep(2000);
        double hourlyRate = (double) worker.getDailyEarnings() / worker.getActiveHours();
        BigDecimal payoutAmount = payoutClient.calculatePayout(
                worker, event.getTriggerType(), disruptedHours, season);
        saveLog(claimId, "ml_calculation", "done",
                "Hourly rate: Rs." + String.format("%.0f", hourlyRate) + "/hr"
                + " | Disrupted: " + formatHours(event.getDisruptedHours())
                + " | Season: " + season
                + " | Persona: " + getPersonaFactor(worker.getPersona()) + "x"
                + " | ML payout: Rs." + payoutAmount);
        sleep(1000);

        // Stage 5 - Coverage cap
        saveLog(claimId, "coverage_cap", "processing", "Checking against weekly coverage cap...");
        sleep(1200);
        if (payoutAmount.compareTo(coverage.getCoverageRemaining()) > 0) {
            payoutAmount = coverage.getCoverageRemaining();
            saveLog(claimId, "coverage_cap", "done",
                    "Payout capped at remaining coverage: Rs." + payoutAmount);
        } else {
            saveLog(claimId, "coverage_cap", "done",
                    "Rs." + payoutAmount + " within Rs." + coverage.getCoverageRemaining()
                    + " cap - full amount approved");
        }
        sleep(1000);

        // Stage 6 - Fraud check
        saveLog(claimId, "fraud_check", "processing", "Running fraud detection checks...");
        sleep(2000);
        double fraudScore = calculateFraudScore(worker, event.getZoneId());
        String claimStatus = fraudScore > FRAUD_THRESHOLD ? "flagged" : "approved";
        saveLog(claimId, "fraud_check", "done", buildFraudDetail(worker, event.getZoneId(), fraudScore));
        sleep(1000);

        // Stage 7 - Final decision
        saveLog(claimId, claimStatus, "processing",
                "approved".equals(claimStatus)
                        ? "Approving claim and crediting wallet..."
                        : "Flagging claim for manual review...");
        sleep(1500);

        Claim claim = new Claim();
        claim.setClaimId(claimId);
        claim.setWorkerId(workerId);
        claim.setEventId(event.getEventId());
        claim.setPolicyNumber(coverage.getPolicyNumber());
        claim.setTriggerType(event.getTriggerType());
        claim.setTriggerValue(event.getTriggerValue() != null ? event.getTriggerValue() : BigDecimal.ZERO);
        claim.setZoneId(event.getZoneId());
        claim.setDisruptedHours(BigDecimal.valueOf(disruptedHours));
        claim.setPayoutAmount(payoutAmount);
        claim.setFraudScore(BigDecimal.valueOf(fraudScore));
        claim.setStatus(claimStatus);
        claim.setTriggeredAt(event.getStartedAt());
        claim.setProcessedAt(LocalDateTime.now());
        claimRepo.save(claim);

        if ("approved".equals(claimStatus)) {
            policyService.deductCoverage(coverage.getPolicyNumber(), payoutAmount);
            // Credit payout to worker wallet
            walletService.credit(
                    workerId,
                    payoutAmount,
                    "Claim payout - " + event.getTriggerType().replace("_", " ") + " in " + event.getZoneName(),
                    claimId,
                    "claim_credit"
            );
            saveLog(claimId, claimStatus, "done",
                    "Claim approved - Rs." + payoutAmount + " credited to your GigShield wallet");
            log.info("Claim APPROVED: {} | worker={} | payout=Rs.{}", claimId, workerId, payoutAmount);
        } else {
            saveLog(claimId, claimStatus, "done",
                    "Flagged for manual review | Fraud score: "
                    + String.format("%.0f", fraudScore * 100) + "%");
            log.warn("Claim FLAGGED: {} | worker={} | score={}", claimId, workerId, fraudScore);
        }
    }

    // ── HELPERS ──────────────────────────────────────────────────

    private void saveLog(String claimId, String stage, String status, String detail) {
        logRepo.save(ClaimProcessingLog.of(claimId, stage, status, detail));
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private String formatHours(BigDecimal hours) {
        if (hours == null) return "~4hrs";
        double h = hours.doubleValue();
        if (h < 1) return String.format("%.0f min", h * 60);
        return String.format("%.1f hrs", h);
    }

    private String getPersonaFactor(String persona) {
        return switch (persona) {
            case "food"      -> "1.20";
            case "ecommerce" -> "0.85";
            default          -> "1.00";
        };
    }

    private String buildFraudDetail(Worker worker, String zoneId, double score) {
        StringBuilder sb = new StringBuilder();
        sb.append("Score: ").append(String.format("%.0f", score * 100)).append("% | ");
        sb.append(worker.getZoneId().equals(zoneId) ? "Zone match" : "Zone mismatch (+50%)").append(" | ");
        sb.append(worker.getIsActive() ? "Active account" : "Inactive (+50%)").append(" | ");
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        long recent = claimRepo.countRecentClaims(worker.getWorkerId(), oneWeekAgo);
        if (recent > 5)      sb.append("High velocity: ").append(recent).append(" claims/7d (+30%)");
        else if (recent > 3) sb.append("Moderate: ").append(recent).append(" claims/7d (+15%)");
        else                 sb.append("Normal: ").append(recent).append(" claims/7d");
        sb.append(" | ").append(score > FRAUD_THRESHOLD ? "FLAGGED" : "PASSED");
        return sb.toString();
    }

    private double calculateFraudScore(Worker worker, String zoneId) {
        double score = 0.0;
        if (!worker.getZoneId().equals(zoneId))     score += 0.50;
        if (!worker.getIsActive())                   score += 0.50;
        if (worker.getDailyEarnings() > 3000)        score += 0.20;
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        long recent = claimRepo.countRecentClaims(worker.getWorkerId(), oneWeekAgo);
        if (recent > 5)      score += 0.30;
        else if (recent > 3) score += 0.15;
        return Math.min(score, 1.0);
    }
}
