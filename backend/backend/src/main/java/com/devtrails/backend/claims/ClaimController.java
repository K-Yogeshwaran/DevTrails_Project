package com.devtrails.backend.claims;

import com.devtrails.backend.claimlog.ClaimProcessingLog;
import com.devtrails.backend.claimlog.ClaimProcessingLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/claims")
@CrossOrigin(origins = "*")
public class ClaimController {

    private static final Logger log = LoggerFactory.getLogger(ClaimController.class);

    private final ClaimService claimService;
    private final ClaimProcessingLogRepository logRepo;
    private final ClaimRepository claimRepo;

    public ClaimController(ClaimService claimService, ClaimProcessingLogRepository logRepo,
                           ClaimRepository claimRepo) {
        this.claimService = claimService;
        this.logRepo = logRepo;
        this.claimRepo = claimRepo;
    }

    // GET /api/claims/worker/{workerId}
    @GetMapping("/worker/{workerId}")
    public ResponseEntity<List<ClaimDTO.ClaimSummary>> getWorkerClaims(
            @PathVariable String workerId) {
        return ResponseEntity.ok(claimService.getWorkerClaims(workerId));
    }

    // GET /api/claims/detail/{claimId}
    @GetMapping("/detail/{claimId}")
    public ResponseEntity<ClaimDTO.ClaimResponse> getClaimById(
            @PathVariable String claimId) {
        return ResponseEntity.ok(claimService.getClaimById(claimId));
    }

    // GET /api/claims/logs/{claimId} — processing stage logs
    @GetMapping("/logs/{claimId}")
    public ResponseEntity<List<ClaimProcessingLog>> getClaimLogs(
            @PathVariable String claimId) {
        return ResponseEntity.ok(logRepo.findByClaimIdOrderByCreatedAtAsc(claimId));
    }

    // GET /api/claims/event/{eventId} — find claim by trigger event_id
    @GetMapping("/event/{eventId}")
    public ResponseEntity<?> getClaimByEventId(@PathVariable String eventId) {
        Optional<Claim> claim = claimRepo.findByEventId(eventId);
        if (claim.isEmpty()) {
            return ResponseEntity.ok(Map.of("status", "queued", "eventId", eventId));
        }
        return ResponseEntity.ok(claimService.getClaimById(claim.get().getClaimId()));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ClaimDTO.Analytics> getAnalytics(
            @RequestParam(required = false) String workerId) {
        return ResponseEntity.ok(
            workerId != null
                ? claimService.getAnalyticsForWorker(workerId)
                : claimService.getAnalytics()
        );
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "claims-service"));
    }
}
