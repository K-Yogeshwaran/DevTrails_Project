package com.devtrails.backend.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.devtrails.backend.config.JwtUtil;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// @Service tells Spring this is a business logic component
@Service

// @RequiredArgsConstructor (Lombok): generates a constructor
// for all final fields — this is how Spring injects dependencies
// Instead of writing @Autowired on each field, we declare them
// as final and Lombok + Spring handle the rest
@RequiredArgsConstructor

// @Slf4j (Lombok): gives us a log object for free
// We can use log.info(), log.error() etc. without declaring a logger
@Slf4j
public class WorkerService {

    // These are injected by Spring automatically
    // because of @RequiredArgsConstructor
    private final WorkerRepository workerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // ── REGISTER ──────────────────────────────────────────

    // @Transactional means: if anything fails inside this method,
    // the entire database operation is rolled back — no partial saves
    @Transactional
    public WorkerDTO.AuthResponse register(WorkerDTO.RegisterRequest request) {

        // Step 1: Check if phone already registered
        if (workerRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone number already registered");
        }

        // Step 2: Check if email already registered (if provided)
        if (request.getEmail() != null && workerRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Step 3: Generate unique worker ID
        // Format: GS-XXXX where XXXX is first 8 chars of a UUID
        // e.g. GS-3f4a8b2c
        String workerId = "GS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Step 4: Hash the password
        // NEVER store plain text passwords
        // passwordEncoder uses BCrypt — a one-way hash
        // BCrypt automatically handles salting (prevents rainbow table attacks)
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // Step 5: Build the Worker entity
        Worker worker = new Worker();
        worker.setWorkerId(workerId);
        worker.setName(request.getName());
        worker.setPhone(request.getPhone());
        worker.setEmail(request.getEmail());
        worker.setZoneId(request.getZoneId());
        worker.setPersona(request.getPersona());
        worker.setDailyEarnings(request.getDailyEarnings());
        worker.setActiveHours(request.getActiveHours());
        worker.setShift(request.getShift());
        worker.setExperienceMonths(request.getExperienceMonths());
        worker.setDaysPerWeek(request.getDaysPerWeek());
        worker.setPasswordHash(hashedPassword);
        worker.setIsActive(true);

        // Step 6: Save to database
        workerRepository.save(worker);
        log.info("Worker registered: {} ({})", worker.getName(), workerId);

        // Step 7: Generate JWT token so the worker is logged in immediately
        String token = jwtUtil.generateToken(workerId);

        // Step 8: Return response (never include passwordHash)
        return new WorkerDTO.AuthResponse(
                workerId,
                worker.getName(),
                worker.getPhone(),
                worker.getZoneId(),
                worker.getPersona(),
                worker.getDailyEarnings(),
                token,
                "Registration successful. Welcome to GigShield!"
        );
    }

    // ── LOGIN ─────────────────────────────────────────────

    public WorkerDTO.AuthResponse login(WorkerDTO.LoginRequest request) {

        // Step 1: Find worker by phone number
        // orElseThrow: if not found, throw exception with message
        Worker worker = workerRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new RuntimeException("Phone number not registered"));

        // Step 2: Check if account is active
        if (!worker.getIsActive()) {
            throw new RuntimeException("Account is deactivated. Contact support.");
        }

        // Step 3: Verify password
        // passwordEncoder.matches() hashes the input and compares with stored hash
        // Returns true if they match
        if (!passwordEncoder.matches(request.getPassword(), worker.getPasswordHash())) {
            throw new RuntimeException("Incorrect password");
        }

        // Step 4: Generate JWT token
        String token = jwtUtil.generateToken(worker.getWorkerId());
        log.info("Worker logged in: {}", worker.getWorkerId());

        return new WorkerDTO.AuthResponse(
                worker.getWorkerId(),
                worker.getName(),
                worker.getPhone(),
                worker.getZoneId(),
                worker.getPersona(),
                worker.getDailyEarnings(),
                token,
                "Login successful"
        );
    }

    // ── GET PROFILE ───────────────────────────────────────

    public WorkerDTO.ProfileResponse getProfile(String workerId) {
        Worker worker = workerRepository.findByWorkerId(workerId)
                .orElseThrow(() -> new RuntimeException("Worker not found: " + workerId));

        // Map Worker entity → ProfileResponse DTO
        // We do this manually to control exactly what fields are exposed
        WorkerDTO.ProfileResponse profile = new WorkerDTO.ProfileResponse();
        profile.setWorkerId(worker.getWorkerId());
        profile.setName(worker.getName());
        profile.setPhone(worker.getPhone());
        profile.setEmail(worker.getEmail());
        profile.setZoneId(worker.getZoneId());
        profile.setPersona(worker.getPersona());
        profile.setDailyEarnings(worker.getDailyEarnings());
        profile.setActiveHours(worker.getActiveHours());
        profile.setShift(worker.getShift());
        profile.setExperienceMonths(worker.getExperienceMonths());
        profile.setDaysPerWeek(worker.getDaysPerWeek());
        profile.setIsActive(worker.getIsActive());
        profile.setCreatedAt(worker.getCreatedAt());
        return profile;
    }

    // ── UPDATE PROFILE ────────────────────────────────────

    @Transactional
    public WorkerDTO.ProfileResponse updateProfile(String workerId, WorkerDTO.UpdateRequest request) {
        Worker worker = workerRepository.findByWorkerId(workerId)
                .orElseThrow(() -> new RuntimeException("Worker not found: " + workerId));

        // Only update fields that were actually sent
        // null check prevents overwriting with empty values
        if (request.getDailyEarnings() != null) worker.setDailyEarnings(request.getDailyEarnings());
        if (request.getActiveHours()   != null) worker.setActiveHours(request.getActiveHours());
        if (request.getShift()         != null) worker.setShift(request.getShift());
        if (request.getZoneId()        != null) worker.setZoneId(request.getZoneId());
        if (request.getDaysPerWeek()   != null) worker.setDaysPerWeek(request.getDaysPerWeek());

        workerRepository.save(worker);
        log.info("Worker profile updated: {}", workerId);
        return getProfile(workerId);
    }

    // ── GET WORKERS BY ZONE ───────────────────────────────
    // Used internally by Claims Service to find affected workers

    public List<WorkerDTO.ProfileResponse> getActiveWorkersByZone(String zoneId) {
        return workerRepository
                .findByZoneIdAndIsActive(zoneId, true)
                .stream()
                // Convert each Worker entity to ProfileResponse DTO
                // map() applies the function to every item in the list
                .map(w -> getProfile(w.getWorkerId()))
                .collect(Collectors.toList());
    }

    // ── DEACTIVATE ────────────────────────────────────────

    @Transactional
    public void deactivateWorker(String workerId) {
        Worker worker = workerRepository.findByWorkerId(workerId)
                .orElseThrow(() -> new RuntimeException("Worker not found: " + workerId));
        worker.setIsActive(false);
        workerRepository.save(worker);
        log.info("Worker deactivated: {}", workerId);
    }

    // ── GET ALL ACTIVE WORKERS ────────────────────────────
    // Used by admin dashboard

    public List<WorkerDTO.ProfileResponse> getAllActiveWorkers() {
        return workerRepository.findByIsActive(true)
                .stream()
                .map(w -> getProfile(w.getWorkerId()))
                .collect(Collectors.toList());
    }
}
