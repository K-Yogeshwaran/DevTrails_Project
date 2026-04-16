package com.devtrails.backend.worker;

import com.devtrails.backend.config.ApiException;
import com.devtrails.backend.wallet.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.devtrails.backend.config.JwtUtil;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class WorkerService {

    private static final Logger log = LoggerFactory.getLogger(WorkerService.class);

    private final WorkerRepository workerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final WalletService walletService;

    public WorkerService(WorkerRepository workerRepository, PasswordEncoder passwordEncoder,
                         JwtUtil jwtUtil, WalletService walletService) {
        this.workerRepository = workerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.walletService = walletService;
    }

    @Transactional
    public WorkerDTO.AuthResponse register(WorkerDTO.RegisterRequest request) {
        if (workerRepository.existsByPhone(request.getPhone())) {
            throw new ApiException(HttpStatus.CONFLICT, "Phone number already registered");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && workerRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email address already registered");
        }

        String workerId = "GS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        Worker worker = new Worker();
        worker.setWorkerId(workerId);
        worker.setName(request.getName());
        worker.setPhone(request.getPhone());
        worker.setEmail(request.getEmail());
        worker.setZoneId(request.getZoneId());
        worker.setPersona(request.getPersona());
        worker.setDailyEarnings(request.getDailyEarnings());
        worker.setActiveHours(request.getActiveHours());
        worker.setExperienceMonths(request.getExperienceMonths());
        worker.setDaysPerWeek(request.getDaysPerWeek());
        worker.setPasswordHash(hashedPassword);
        worker.setIsActive(true);

        workerRepository.save(worker);
        log.info("Worker registered: {} ({})", worker.getName(), workerId);

        // Create wallet with ₹1000 initial balance
        walletService.createWallet(workerId);
        log.info("Wallet created for worker {}", workerId);


        String token = jwtUtil.generateToken(workerId);


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

    public WorkerDTO.AuthResponse login(WorkerDTO.LoginRequest request) {

        Worker worker = workerRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No account found with this phone number"));

        if (!worker.getIsActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is deactivated. Please contact support.");
        }

        if (!passwordEncoder.matches(request.getPassword(), worker.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Incorrect password. Please try again.");
        }

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

    public WorkerDTO.ProfileResponse getProfile(String workerId) {
        Worker worker = workerRepository.findByWorkerId(workerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Worker not found with ID: " + workerId));

        WorkerDTO.ProfileResponse profile = new WorkerDTO.ProfileResponse();
        profile.setWorkerId(worker.getWorkerId());
        profile.setName(worker.getName());
        profile.setPhone(worker.getPhone());
        profile.setEmail(worker.getEmail());
        profile.setZoneId(worker.getZoneId());
        profile.setPersona(worker.getPersona());
        profile.setDailyEarnings(worker.getDailyEarnings());
        profile.setActiveHours(worker.getActiveHours());
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
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Worker not found with ID: " + workerId));

        // Only update fields that were actually sent
        // null check prevents overwriting with empty values
        if (request.getDailyEarnings() != null) worker.setDailyEarnings(request.getDailyEarnings());
        if (request.getActiveHours()   != null) worker.setActiveHours(request.getActiveHours());
        if (request.getZoneId()        != null) worker.setZoneId(request.getZoneId());
        if (request.getDaysPerWeek()   != null) worker.setDaysPerWeek(request.getDaysPerWeek());

        workerRepository.save(worker);
        log.info("Worker profile updated: {}", workerId);
        return getProfile(workerId);
    }

    public List<WorkerDTO.ProfileResponse> getActiveWorkersByZone(String zoneId) {
        return workerRepository
                .findByZoneIdAndIsActive(zoneId, true)
                .stream()
                .map(w -> getProfile(w.getWorkerId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deactivateWorker(String workerId) {
        Worker worker = workerRepository.findByWorkerId(workerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Worker not found with ID: " + workerId));
        worker.setIsActive(false);
        workerRepository.save(worker);
        log.info("Worker deactivated: {}", workerId);
    }

    public List<WorkerDTO.ProfileResponse> getAllActiveWorkers() {
        return workerRepository.findByIsActive(true)
                .stream()
                .map(w -> getProfile(w.getWorkerId()))
                .collect(Collectors.toList());
    }
}
