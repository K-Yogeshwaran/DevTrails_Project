package com.devtrails.backend.policy;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/policies")
@CrossOrigin(origins = "*")
public class PolicyController {

    private static final Logger log = LoggerFactory.getLogger(PolicyController.class);

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @PostMapping
    public ResponseEntity<PolicyDTO.PolicyResponse> createPolicy(
            @Valid @RequestBody PolicyDTO.CreateRequest request) {
        PolicyDTO.PolicyResponse response = policyService.createPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{workerId}/current")
    public ResponseEntity<PolicyDTO.PolicyResponse> getCurrentPolicy(
            @PathVariable String workerId) {
        return ResponseEntity.ok(policyService.getCurrentPolicy(workerId));
    }

    @GetMapping("/{workerId}/history")
    public ResponseEntity<List<PolicyDTO.PolicySummary>> getPolicyHistory(
            @PathVariable String workerId) {
        return ResponseEntity.ok(policyService.getWorkerPolicies(workerId));
    }

    @GetMapping("/{workerId}/coverage-check")
    public ResponseEntity<PolicyDTO.CoverageCheck> checkCoverage(
            @PathVariable String workerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate checkDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(policyService.checkCoverage(workerId, checkDate));
    }

    @PostMapping("/expire-old")
    public ResponseEntity<Map<String, String>> expireOldPolicies() {
        policyService.expireOldPolicies();
        return ResponseEntity.ok(Map.of("message", "Old policies expired successfully"));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "policy-service"
        ));
    }
}
