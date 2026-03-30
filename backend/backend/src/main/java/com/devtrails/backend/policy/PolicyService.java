package com.devtrails.backend.policy;


import com.devtrails.backend.policy.PremiumCalculatorClient;
import com.devtrails.backend.worker.Worker;
import com.devtrails.backend.worker.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final WorkerRepository workerRepository;
    private final PremiumCalculatorClient premiumClient;

    // Coverage caps per tier — must match config.py in ML module
    private static final Map<String, BigDecimal> COVERAGE_CAPS = Map.of(
            "basic",    new BigDecimal("2000"),
            "standard", new BigDecimal("4500"),
            "premium",  new BigDecimal("8000")
    );

    // ── CREATE POLICY ─────────────────────────────────────
    @Transactional
    public PolicyDTO.PolicyResponse createPolicy(PolicyDTO.CreateRequest request) {

        // Step 1: Check worker exists
        Worker worker = workerRepository.findByWorkerId(request.getWorkerId())
                .orElseThrow(() -> new RuntimeException("Worker not found: " + request.getWorkerId()));

        // Step 2: Calculate week boundaries
        // Insurance week runs Monday to Sunday
        LocalDate today     = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd   = today.with(DayOfWeek.SUNDAY);

        // Step 3: Check if worker already has a policy this week
        if (policyRepository.existsActivePolicyForWeek(request.getWorkerId(), weekStart)) {
            throw new RuntimeException(
                    "Worker already has an active policy for this week. " +
                            "Policy renews every Monday."
            );
        }

        // Step 4: Validate tier
        String tier = request.getTier().toLowerCase();
        if (!COVERAGE_CAPS.containsKey(tier)) {
            throw new RuntimeException("Invalid tier: " + tier);
        }

        // Step 5: Call ML API to get dynamic premium
        // This is where XGBoost model runs for this specific worker
        BigDecimal weeklyPremium = premiumClient.calculatePremium(worker, request.getSeason());

        // Step 6: Get coverage cap for chosen tier
        BigDecimal coverageCap = COVERAGE_CAPS.get(tier);

        // Step 7: Generate unique policy number
        // Format: POL-{WORKERID}-{DATE}
        // e.g. POL-GS3F4A8B-20260318
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String policyNumber = "POL-" +
                worker.getWorkerId().replace("GS-", "") + "-" + dateStr;

        // Step 8: Build and save the policy
        Policy policy = new Policy();
        policy.setPolicyNumber(policyNumber);
        policy.setWorkerId(request.getWorkerId());
        policy.setTier(tier);
        policy.setWeeklyPremium(weeklyPremium);
        policy.setCoverageCap(coverageCap);
        policy.setCoverageUsed(BigDecimal.ZERO);
        policy.setSeason(request.getSeason());
        policy.setStatus("active");
        policy.setWeekStart(weekStart);
        policy.setWeekEnd(weekEnd);

        policyRepository.save(policy);
        log.info("Policy created: {} for worker {} | Tier: {} | Premium: ₹{}",
                policyNumber, request.getWorkerId(), tier, weeklyPremium);

        return buildResponse(policy, "Policy created successfully. Coverage active until " + weekEnd);
    }

    // ── CHECK COVERAGE ────────────────────────────────────
    // Called by ClaimsService before processing any payout
    // Returns whether the worker is covered and how much is remaining
    public PolicyDTO.CoverageCheck checkCoverage(String workerId, LocalDate date) {

        return policyRepository.findActivePolicy(workerId, date)
                .map(policy -> {
                    // Calculate remaining coverage
                    BigDecimal remaining = policy.getCoverageCap()
                            .subtract(policy.getCoverageUsed());

                    return new PolicyDTO.CoverageCheck(
                            true,
                            policy.getPolicyNumber(),
                            remaining,
                            null  // no reason — worker IS covered
                    );
                })
                // If no active policy found — return not covered
                .orElse(new PolicyDTO.CoverageCheck(
                        false,
                        null,
                        BigDecimal.ZERO,
                        "No active policy found for this week"
                ));
    }

    // ── DEDUCT COVERAGE ───────────────────────────────────
    // Called after a payout is approved
    // Reduces the remaining coverage cap by the payout amount
    @Transactional
    public void deductCoverage(String policyNumber, BigDecimal amount) {
        Policy policy = policyRepository.findByPolicyNumber(policyNumber)
                .orElseThrow(() -> new RuntimeException("Policy not found: " + policyNumber));

        // Add payout amount to coverage used
        BigDecimal newUsed = policy.getCoverageUsed().add(amount);
        policy.setCoverageUsed(newUsed);

        // If coverage is fully used up, mark as exhausted
        if (newUsed.compareTo(policy.getCoverageCap()) >= 0) {
            policy.setStatus("exhausted");
            log.info("Policy {} coverage exhausted", policyNumber);
        }

        policyRepository.save(policy);
        log.info("Coverage deducted: ₹{} from policy {} | Used: ₹{}/₹{}",
                amount, policyNumber, newUsed, policy.getCoverageCap());
    }

    // ── GET WORKER POLICIES ───────────────────────────────
    public List<PolicyDTO.PolicySummary> getWorkerPolicies(String workerId) {
        return policyRepository
                .findByWorkerIdOrderByCreatedAtDesc(workerId)
                .stream()
                .map(this::buildSummary)
                .collect(Collectors.toList());
    }

    // ── GET CURRENT POLICY ────────────────────────────────
    public PolicyDTO.PolicyResponse getCurrentPolicy(String workerId) {
        Policy policy = policyRepository
                .findActivePolicy(workerId, LocalDate.now())
                .orElseThrow(() -> new RuntimeException(
                        "No active policy this week. Subscribe to get coverage."
                ));
        return buildResponse(policy, "Active policy");
    }

    // ── EXPIRE OLD POLICIES ───────────────────────────────
    // Called by a scheduled job every Monday to expire last week's policies
    @Transactional
    public void expireOldPolicies() {
        LocalDate today = LocalDate.now();
        List<Policy> activePolicies = policyRepository.findByStatus("active");

        int expiredCount = 0;
        for (Policy policy : activePolicies) {
            // If policy's end date is before today, it's expired
            if (policy.getWeekEnd().isBefore(today)) {
                policy.setStatus("expired");
                policyRepository.save(policy);
                expiredCount++;
            }
        }
        if (expiredCount > 0) {
            log.info("Expired {} old policies", expiredCount);
        }
    }

    // ── HELPERS ───────────────────────────────────────────
    private PolicyDTO.PolicyResponse buildResponse(Policy p, String message) {
        BigDecimal remaining = p.getCoverageCap().subtract(p.getCoverageUsed());
        return new PolicyDTO.PolicyResponse(
                p.getPolicyNumber(), p.getWorkerId(), p.getTier(),
                p.getWeeklyPremium(), p.getCoverageCap(), p.getCoverageUsed(),
                remaining, p.getSeason(), p.getStatus(),
                p.getWeekStart(), p.getWeekEnd(), p.getCreatedAt(), message
        );
    }

    private PolicyDTO.PolicySummary buildSummary(Policy p) {
        return new PolicyDTO.PolicySummary(
                p.getPolicyNumber(), p.getTier(), p.getWeeklyPremium(),
                p.getCoverageCap(), p.getCoverageUsed(), p.getStatus(),
                p.getWeekStart(), p.getWeekEnd()
        );
    }
}
