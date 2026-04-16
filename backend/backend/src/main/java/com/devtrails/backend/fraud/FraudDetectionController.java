package com.devtrails.backend.fraud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fraud")
@CrossOrigin(origins = "*")
public class FraudDetectionController {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionController.class);

    private final FraudDetectionLogRepository fraudLogRepository;

    public FraudDetectionController(FraudDetectionLogRepository fraudLogRepository) {
        this.fraudLogRepository = fraudLogRepository;
    }

    // GET /api/fraud/logs/{claimId}
    @GetMapping("/logs/{claimId}")
    public ResponseEntity<List<FraudDetectionLog>> getClaimFraudLogs(
            @PathVariable String claimId) {
        return ResponseEntity.ok(fraudLogRepository.findByClaimIdOrderByCreatedAtDesc(claimId));
    }

    // GET /api/fraud/worker/{workerId}
    @GetMapping("/worker/{workerId}")
    public ResponseEntity<List<FraudDetectionLog>> getWorkerFraudLogs(
            @PathVariable String workerId) {
        return ResponseEntity.ok(fraudLogRepository.findByWorkerIdOrderByCreatedAtDesc(workerId));
    }

    // GET /api/fraud/type/{detectionType}
    @GetMapping("/type/{detectionType}")
    public ResponseEntity<List<FraudDetectionLog>> getFraudLogsByType(
            @PathVariable String detectionType) {
        return ResponseEntity.ok(fraudLogRepository.findByDetectionTypeOrderByCreatedAtDesc(detectionType));
    }

    // GET /api/fraud/risk/{riskLevel}
    @GetMapping("/risk/{riskLevel}")
    public ResponseEntity<List<FraudDetectionLog>> getFraudLogsByRiskLevel(
            @PathVariable String riskLevel) {
        return ResponseEntity.ok(fraudLogRepository.findByRiskLevelOrderByCreatedAtDesc(riskLevel));
    }

    // GET /api/fraud/high-risk
    @GetMapping("/high-risk")
    public ResponseEntity<List<FraudDetectionLog>> getHighRiskFraudLogs() {
        return ResponseEntity.ok(fraudLogRepository.findHighRiskFraudLogs(0.7));
    }

    // GET /api/fraud/analytics
    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getFraudAnalytics() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);

        // Detection type analytics
        List<Object[]> detectionTypes = fraudLogRepository.countByDetectionTypeSince(oneWeekAgo);
        Map<String, Long> detectionTypeCounts = detectionTypes.stream()
                .collect(java.util.stream.Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));

        // Risk level analytics
        List<Object[]> riskLevels = fraudLogRepository.countByRiskLevelSince(oneWeekAgo);
        Map<String, Long> riskLevelCounts = riskLevels.stream()
                .collect(java.util.stream.Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));

        // Total counts
        long totalWeekLogs = fraudLogRepository.countRecentFraudLogs("SYSTEM", oneWeekAgo);
        long totalMonthLogs = fraudLogRepository.countRecentFraudLogs("SYSTEM", oneMonthAgo);

        return ResponseEntity.ok(Map.of(
                "lastWeek", Map.of(
                        "totalLogs", totalWeekLogs,
                        "byDetectionType", detectionTypeCounts,
                        "byRiskLevel", riskLevelCounts
                ),
                "lastMonth", Map.of(
                        "totalLogs", totalMonthLogs
                )
        ));
    }

    // GET /api/fraud/health
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "fraud-detection-service"));
    }
}
