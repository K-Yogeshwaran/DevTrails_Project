package com.devtrails.backend.wallet;

import com.devtrails.backend.policy.Policy;
import com.devtrails.backend.policy.PolicyRepository;
import com.devtrails.backend.policy.PolicyService;
import com.devtrails.backend.policy.PolicyDTO;
import com.devtrails.backend.worker.Worker;
import com.devtrails.backend.worker.WorkerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class WeeklyPremiumScheduler {

    private static final Logger log = LoggerFactory.getLogger(WeeklyPremiumScheduler.class);

    private final PolicyRepository  policyRepo;
    private final PolicyService     policyService;
    private final WorkerRepository  workerRepo;
    private final WalletService     walletService;

    public WeeklyPremiumScheduler(PolicyRepository policyRepo,
                                  PolicyService policyService,
                                  WorkerRepository workerRepo,
                                  WalletService walletService) {
        this.policyRepo = policyRepo;
        this.policyService = policyService;
        this.workerRepo = workerRepo;
        this.walletService = walletService;
    }

    private static final Map<String, BigDecimal> TIER_PREMIUMS = Map.of(
            "basic",    new BigDecimal("49"),
            "standard", new BigDecimal("89"),
            "premium",  new BigDecimal("149")
    );

    private static final Map<String, BigDecimal> COVERAGE_CAPS = Map.of(
            "basic",    new BigDecimal("2000"),
            "standard", new BigDecimal("4500"),
            "premium",  new BigDecimal("8000")
    );

    // Runs every Monday at 00:01 AM
    // Cron: second minute hour day month weekday
    @Scheduled(cron = "0 1 0 * * MON")
    public void autoRenewPolicies() {
        log.info("WeeklyPremiumScheduler: Starting auto-renewal cycle");

        // First expire all old policies
        policyService.expireOldPolicies();

        // Find all workers who had an active policy last week
        LocalDate today     = LocalDate.now();
        LocalDate lastWeek  = today.minusWeeks(1);

        List<Policy> expiredPolicies = policyRepo.findByStatus("expired");
        int renewed = 0;
        int failed  = 0;

        for (Policy oldPolicy : expiredPolicies) {
            // Only auto-renew if it expired this week (last Monday)
            if (oldPolicy.getWeekEnd() == null ||
                !oldPolicy.getWeekEnd().isAfter(lastWeek.with(DayOfWeek.SUNDAY).minusDays(1))) {
                continue;
            }

            String workerId = oldPolicy.getWorkerId();
            String tier     = oldPolicy.getTier();
            String season   = oldPolicy.getSeason();

            try {
                autoRenewForWorker(workerId, tier, season, today);
                renewed++;
            } catch (Exception e) {
                log.warn("Auto-renewal failed for worker {}: {}", workerId, e.getMessage());
                failed++;
            }
        }

        log.info("WeeklyPremiumScheduler: Renewed={} Failed={}", renewed, failed);
    }

    @Transactional
    public void autoRenewForWorker(String workerId, String tier, String season, LocalDate today) {
        // Check if worker already has a policy this week (manual subscription)
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        if (policyRepo.existsActivePolicyForWeek(workerId, weekStart)) {
            log.info("Worker {} already has policy this week - skipping auto-renewal", workerId);
            return;
        }

        BigDecimal premium = TIER_PREMIUMS.getOrDefault(tier, new BigDecimal("89"));

        // Check wallet balance
        if (!walletService.hasSufficientBalance(workerId, premium)) {
            log.warn("Auto-renewal failed for worker {} - insufficient balance (need Rs.{})", workerId, premium);
            return;
        }

        // Create new policy
        LocalDate weekEnd = today.with(DayOfWeek.SUNDAY);
        String dateStr    = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String policyNumber = "POL-" + workerId.replace("GS-", "") + "-" + dateStr;

        com.devtrails.backend.policy.Policy policy = new com.devtrails.backend.policy.Policy();
        policy.setPolicyNumber(policyNumber);
        policy.setWorkerId(workerId);
        policy.setTier(tier);
        policy.setWeeklyPremium(premium);
        policy.setCoverageCap(COVERAGE_CAPS.getOrDefault(tier, new BigDecimal("4500")));
        policy.setCoverageUsed(BigDecimal.ZERO);
        policy.setSeason(season);
        policy.setStatus("active");
        policy.setWeekStart(weekStart);
        policy.setWeekEnd(weekEnd);
        policyRepo.save(policy);

        // Debit premium from wallet
        walletService.debit(
                workerId,
                premium,
                "Auto-renewal - " + tier + " plan (Rs." + premium + "/week)",
                policyNumber,
                "auto_renewal"
        );

        log.info("Auto-renewed policy {} for worker {} | tier={} | premium=Rs.{}",
                policyNumber, workerId, tier, premium);
    }
}
