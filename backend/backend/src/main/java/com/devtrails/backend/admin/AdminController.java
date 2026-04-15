package com.devtrails.backend.admin;

import com.devtrails.backend.claims.Claim;
import com.devtrails.backend.claims.ClaimRepository;
import com.devtrails.backend.claims.ClaimService;
import com.devtrails.backend.claims.ClaimDTO;
import com.devtrails.backend.config.JwtUtil;
import com.devtrails.backend.policy.PolicyRepository;
import com.devtrails.backend.worker.WorkerRepository;
import com.devtrails.backend.triggerevent.TriggerEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AdminController {

    private final JwtUtil              jwtUtil;
    private final ClaimRepository      claimRepo;
    private final ClaimService         claimService;
    private final WorkerRepository     workerRepo;
    private final PolicyRepository     policyRepo;
    private final TriggerEventRepository triggerEventRepo;

    // Hardcoded admin credentials — no DB table needed for hackathon
    private static final Map<String, String> ADMINS = Map.of(
        "admin",      "gigshield@admin2026",
        "devtrails",  "devtrails@2026",
        "yogesh",     "yogesh@admin",
        "sriram",     "sriram@admin"
    );

    // ── POST /api/admin/login ─────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {
        String username = req.get("username");
        String password = req.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username and password required"));
        }

        String expected = ADMINS.get(username.toLowerCase());
        if (expected == null || !expected.equals(password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid admin credentials"));
        }

        // Generate JWT with admin prefix so it's distinguishable
        String token = jwtUtil.generateToken("ADMIN-" + username.toUpperCase());
        log.info("Admin login: {}", username);

        return ResponseEntity.ok(Map.of(
            "token",    token,
            "username", username,
            "role",     "admin",
            "message",  "Welcome, " + username
        ));
    }

    // ── GET /api/admin/stats ──────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long totalWorkers   = workerRepo.count();
        long activeWorkers  = workerRepo.findByIsActive(true).size();
        long totalClaims    = claimRepo.count();
        long approvedClaims = claimRepo.findAll().stream()
                .filter(c -> "approved".equals(c.getStatus())).count();
        long flaggedClaims  = claimRepo.findAll().stream()
                .filter(c -> "flagged".equals(c.getStatus())).count();
        long activePolicies = policyRepo.findByStatus("active").size();
        long activeTriggers = triggerEventRepo.findByStatus("active").size();

        BigDecimal totalPaid = claimRepo.totalPaidOut();
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;

        // Claims today
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long claimsToday = claimRepo.findAll().stream()
                .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().isAfter(todayStart))
                .count();

        double approvalRate = totalClaims > 0
                ? (double) approvedClaims / totalClaims * 100 : 0;

        return ResponseEntity.ok(Map.of(
            "totalWorkers",   totalWorkers,
            "activeWorkers",  activeWorkers,
            "totalClaims",    totalClaims,
            "approvedClaims", approvedClaims,
            "flaggedClaims",  flaggedClaims,
            "claimsToday",    claimsToday,
            "activePolicies", activePolicies,
            "activeTriggers", activeTriggers,
            "totalPaidOut",   totalPaid,
            "approvalRate",   Math.round(approvalRate * 10.0) / 10.0
        ));
    }

    // ── GET /api/admin/claims/flagged ─────────────────────────
    @GetMapping("/claims/flagged")
    public ResponseEntity<List<AdminDTO.FlaggedClaim>> getFlaggedClaims() {
        List<Claim> flagged = claimRepo.findByStatus("flagged");
        List<AdminDTO.FlaggedClaim> result = flagged.stream().map(c -> {
            String workerName = workerRepo.findByWorkerId(c.getWorkerId())
                    .map(w -> w.getName()).orElse("Unknown");
            String workerPhone = workerRepo.findByWorkerId(c.getWorkerId())
                    .map(w -> w.getPhone()).orElse("—");
            return new AdminDTO.FlaggedClaim(
                c.getClaimId(), c.getWorkerId(), workerName, workerPhone,
                c.getTriggerType(), c.getZoneId(), c.getPayoutAmount(),
                c.getFraudScore(), c.getTriggeredAt(), c.getProcessedAt()
            );
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ── POST /api/admin/claims/{claimId}/approve ──────────────
    @PostMapping("/claims/{claimId}/approve")
    @Transactional
    public ResponseEntity<?> approveClaim(@PathVariable String claimId) {
        Claim claim = claimRepo.findByClaimId(claimId).orElse(null);
        if (claim == null) {
            return ResponseEntity.notFound().build();
        }
        if (!"flagged".equals(claim.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only flagged claims can be manually approved"));
        }
        claim.setStatus("approved");
        claimRepo.save(claim);
        log.info("Admin manually approved claim: {}", claimId);
        return ResponseEntity.ok(Map.of(
            "message", "Claim " + claimId + " approved successfully",
            "claimId", claimId
        ));
    }

    // ── POST /api/admin/claims/{claimId}/reject ───────────────
    @PostMapping("/claims/{claimId}/reject")
    @Transactional
    public ResponseEntity<?> rejectClaim(@PathVariable String claimId) {
        Claim claim = claimRepo.findByClaimId(claimId).orElse(null);
        if (claim == null) {
            return ResponseEntity.notFound().build();
        }
        claim.setStatus("rejected");
        claimRepo.save(claim);
        log.info("Admin rejected claim: {}", claimId);
        return ResponseEntity.ok(Map.of(
            "message", "Claim " + claimId + " rejected",
            "claimId", claimId
        ));
    }

    // ── GET /api/admin/claims/all ─────────────────────────────
    @GetMapping("/claims/all")
    public ResponseEntity<List<AdminDTO.FlaggedClaim>> getAllClaims() {
        List<AdminDTO.FlaggedClaim> result = claimRepo.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(c -> {
                    String workerName = workerRepo.findByWorkerId(c.getWorkerId())
                            .map(w -> w.getName()).orElse("Unknown");
                    String workerPhone = workerRepo.findByWorkerId(c.getWorkerId())
                            .map(w -> w.getPhone()).orElse("—");
                    return new AdminDTO.FlaggedClaim(
                        c.getClaimId(), c.getWorkerId(), workerName, workerPhone,
                        c.getTriggerType(), c.getZoneId(), c.getPayoutAmount(),
                        c.getFraudScore(), c.getTriggeredAt(), c.getProcessedAt()
                    );
                }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ── GET /api/admin/workers ────────────────────────────────
    @GetMapping("/workers")
    public ResponseEntity<Map<String, Object>> getWorkerStats() {
        long total    = workerRepo.count();
        long active   = workerRepo.findByIsActive(true).size();
        long inactive = total - active;

        Map<String, Long> byPersona = workerRepo.findByIsActive(true).stream()
                .collect(Collectors.groupingBy(
                    w -> w.getPersona() != null ? w.getPersona() : "unknown",
                    Collectors.counting()
                ));

        Map<String, Long> byZone = workerRepo.findByIsActive(true).stream()
                .collect(Collectors.groupingBy(
                    w -> w.getZoneId() != null ? w.getZoneId() : "unknown",
                    Collectors.counting()
                ));

        return ResponseEntity.ok(Map.of(
            "total",     total,
            "active",    active,
            "inactive",  inactive,
            "byPersona", byPersona,
            "byZone",    byZone
        ));
    }

    // ── GET /api/admin/policies ───────────────────────────────
    @GetMapping("/policies")
    public ResponseEntity<Map<String, Object>> getPolicyStats() {
        long active    = policyRepo.findByStatus("active").size();
        long expired   = policyRepo.findByStatus("expired").size();
        long exhausted = policyRepo.findByStatus("exhausted").size();

        Map<String, Long> byTier = policyRepo.findByStatus("active").stream()
                .collect(Collectors.groupingBy(
                    p -> p.getTier() != null ? p.getTier() : "unknown",
                    Collectors.counting()
                ));

        BigDecimal totalPremium = policyRepo.findByStatus("active").stream()
                .map(p -> p.getWeeklyPremium() != null ? p.getWeeklyPremium() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(Map.of(
            "active",       active,
            "expired",      expired,
            "exhausted",    exhausted,
            "byTier",       byTier,
            "totalPremium", totalPremium
        ));
    }

    // ── GET /api/admin/analytics/loss-ratio ───────────────────
    @GetMapping("/analytics/loss-ratio")
    public ResponseEntity<AdminDTO.LossRatioInfo> getLossRatioInfo() {
        BigDecimal totalPremiums = policyRepo.findAll().stream()
                .map(p -> p.getWeeklyPremium() != null ? p.getWeeklyPremium() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalClaimsPaid = claimRepo.totalPaidOut();
        if (totalClaimsPaid == null) totalClaimsPaid = BigDecimal.ZERO;

        double lossRatio = totalPremiums.compareTo(BigDecimal.ZERO) > 0
                ? totalClaimsPaid.divide(totalPremiums, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100
                : 0;

        java.util.List<AdminDTO.MonthlyStats> history = java.util.List.of(
            new AdminDTO.MonthlyStats("Jan", new BigDecimal("45000"), new BigDecimal("12000")),
            new AdminDTO.MonthlyStats("Feb", new BigDecimal("52000"), new BigDecimal("18000")),
            new AdminDTO.MonthlyStats("Mar", new BigDecimal("48000"), new BigDecimal("15000")),
            new AdminDTO.MonthlyStats("Apr", totalPremiums, totalClaimsPaid)
        );

        return ResponseEntity.ok(new AdminDTO.LossRatioInfo(totalPremiums, totalClaimsPaid, lossRatio, history));
    }

    // ── GET /api/admin/analytics/predictive ───────────────────
    @GetMapping("/analytics/predictive")
    public ResponseEntity<AdminDTO.PredictiveAnalytics> getPredictiveAnalytics() {
        // Mocking next week's predictions for demo purposes
        java.util.List<AdminDTO.ZoneRisk> risks = java.util.List.of(
            new AdminDTO.ZoneRisk("zone_bangalore_central", "Bangalore Central", "Heavy Rain", 0.65, 420),
            new AdminDTO.ZoneRisk("zone_mumbai_andheri", "Mumbai Andheri", "Heatwave", 0.82, 650),
            new AdminDTO.ZoneRisk("zone_delhi_rohini", "Delhi Rohini", "AQI Spike", 0.45, 310),
            new AdminDTO.ZoneRisk("zone_chennai_adyar", "Chennai Adyar", "Low Risk", 0.12, 180)
        );

        BigDecimal estPayout = new BigDecimal("45000");

        return ResponseEntity.ok(new AdminDTO.PredictiveAnalytics(risks, estPayout));
    }

    // ── GET /api/admin/health ─────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "admin"));
    }
}
