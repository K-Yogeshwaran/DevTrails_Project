package com.devtrails.backend.policy;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PolicyController {

    private final PolicyService policyService;


    @PostMapping
    public ResponseEntity<?> createPolicy(
            @Valid @RequestBody PolicyDTO.CreateRequest request) {
        try {
            PolicyDTO.PolicyResponse response = policyService.createPolicy(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/{workerId}/current")
    public ResponseEntity<?> getCurrentPolicy(@PathVariable String workerId) {
        try {
            return ResponseEntity.ok(policyService.getCurrentPolicy(workerId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/{workerId}/history")
    public ResponseEntity<List<PolicyDTO.PolicySummary>> getPolicyHistory(
            @PathVariable String workerId) {
        return ResponseEntity.ok(policyService.getWorkerPolicies(workerId));
    }


    @GetMapping("/{workerId}/coverage-check")
    public ResponseEntity<PolicyDTO.CoverageCheck> checkCoverage(
            @PathVariable String workerId,
            // ?date=2026-03-18 — defaults to today if not provided
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate checkDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(policyService.checkCoverage(workerId, checkDate));
    }

    @PostMapping("/expire-old")
    public ResponseEntity<Map<String, String>> expireOldPolicies() {
        policyService.expireOldPolicies();
        return ResponseEntity.ok(Map.of(
                "message", "Old policies expired successfully"
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "policy-service"
        ));
    }
}
