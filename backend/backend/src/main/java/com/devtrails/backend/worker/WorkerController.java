package com.devtrails.backend.worker;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class WorkerController {

    private final WorkerService workerService;

    @PostMapping("/register")
    public ResponseEntity<WorkerDTO.AuthResponse> register(
            @Valid @RequestBody WorkerDTO.RegisterRequest request) {
        WorkerDTO.AuthResponse response = workerService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<WorkerDTO.AuthResponse> login(
            @Valid @RequestBody WorkerDTO.LoginRequest request) {
        return ResponseEntity.ok(workerService.login(request));
    }

    @GetMapping("/{workerId}")
    public ResponseEntity<WorkerDTO.ProfileResponse> getProfile(
            @PathVariable String workerId) {
        return ResponseEntity.ok(workerService.getProfile(workerId));
    }

    @PutMapping("/{workerId}")
    public ResponseEntity<WorkerDTO.ProfileResponse> updateProfile(
            @PathVariable String workerId,
            @RequestBody WorkerDTO.UpdateRequest request) {
        return ResponseEntity.ok(workerService.updateProfile(workerId, request));
    }

    @GetMapping("/zone/{zoneId}")
    public ResponseEntity<List<WorkerDTO.ProfileResponse>> getWorkersByZone(
            @PathVariable String zoneId) {
        return ResponseEntity.ok(workerService.getActiveWorkersByZone(zoneId));
    }

    @GetMapping
    public ResponseEntity<List<WorkerDTO.ProfileResponse>> getAllActive() {
        return ResponseEntity.ok(workerService.getAllActiveWorkers());
    }

    @DeleteMapping("/{workerId}")
    public ResponseEntity<Map<String, String>> deactivate(
            @PathVariable String workerId) {
        workerService.deactivateWorker(workerId);
        return ResponseEntity.ok(Map.of(
                "message", "Worker deactivated successfully",
                "workerId", workerId
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "worker-service",
                "port", "8080"
        ));
    }
}
