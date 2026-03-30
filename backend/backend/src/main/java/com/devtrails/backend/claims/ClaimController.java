package com.devtrails.backend.claims;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ClaimController {

    private final ClaimService claimService;

    @GetMapping("/{workerId}")
    public ResponseEntity<List<ClaimDTO.ClaimSummary>> getWorkerClaims(
            @PathVariable String workerId) {
        return ResponseEntity.ok(claimService.getWorkerClaims(workerId));
    }

    @GetMapping("/detail/{claimId}")
    public ResponseEntity<?> getClaimById(@PathVariable String claimId) {
        try {
            return ResponseEntity.ok(claimService.getClaimById(claimId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/process-now")
    public ResponseEntity<Map<String, String>> processNow() {
        claimService.pollAndProcessTriggers();
        return ResponseEntity.ok(Map.of(
                "message", "Claim processing cycle triggered manually"
        ));
    }

    // ── GET /api/claims/analytics ─────────────────────────
    // Summary stats for admin dashboard
    // Total claims, approval rate, fraud rate, total paid out
    @GetMapping("/analytics")
    public ResponseEntity<ClaimDTO.Analytics> getAnalytics() {
        return ResponseEntity.ok(claimService.getAnalytics());
    }

    // ── GET /api/claims/health ────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "claims-service"
        ));
    }
}
