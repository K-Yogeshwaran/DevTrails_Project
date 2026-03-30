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
        try {
            WorkerDTO.AuthResponse response = workerService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody WorkerDTO.LoginRequest request) {
        try {
            WorkerDTO.AuthResponse response = workerService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{workerId}")
    public ResponseEntity<WorkerDTO.ProfileResponse> getProfile(
            @PathVariable String workerId) {
        try {
            return ResponseEntity.ok(workerService.getProfile(workerId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{workerId}")
    public ResponseEntity<WorkerDTO.ProfileResponse> updateProfile(
            @PathVariable String workerId,
            @RequestBody WorkerDTO.UpdateRequest request) {
        try {
            return ResponseEntity.ok(workerService.updateProfile(workerId, request));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
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
        try {
            workerService.deactivateWorker(workerId);
            return ResponseEntity.ok(Map.of(
                    "message", "Worker deactivated successfully",
                    "workerId", workerId
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
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