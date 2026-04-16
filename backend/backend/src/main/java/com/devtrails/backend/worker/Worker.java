package com.devtrails.backend.worker;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "workers")
public class Worker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "worker_id", unique = true, nullable = false, length = 20)
    @NotBlank(message = "Worker ID is required")
    private String workerId;

    @Column(name = "name", nullable = false, length = 100)
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be 2–100 characters")
    private String name;

    @Column(name = "phone", unique = true, nullable = false, length = 15)
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
    private String phone;

    @Column(name = "email", unique = true, length = 100)
    @Email(message = "Enter a valid email address")
    private String email;

    @Column(name = "zone_id", nullable = false, length = 60)
    @NotBlank(message = "Zone is required")
    private String zoneId;

    @Column(name = "persona", nullable = false, length = 20)
    @NotBlank(message = "Persona is required")
    private String persona;

    @Column(name = "daily_earnings", nullable = false)
    @Min(value = 100, message = "Daily earnings must be at least ₹100")
    @Max(value = 10000, message = "Daily earnings cannot exceed ₹10,000")
    private Integer dailyEarnings;

    @Column(name = "active_hours", nullable = false)
    @Min(value = 1, message = "Active hours must be at least 1")
    @Max(value = 16, message = "Active hours cannot exceed 16")
    private Integer activeHours;

    @Column(name = "experience_months")
    @Min(value = 0, message = "Experience cannot be negative")
    private Integer experienceMonths = 0;

    @Column(name = "days_per_week")
    @Min(value = 1, message = "Must work at least 1 day per week")
    @Max(value = 7, message = "Cannot work more than 7 days per week")
    private Integer daysPerWeek = 6;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Worker() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }
    public String getPersona() { return persona; }
    public void setPersona(String persona) { this.persona = persona; }
    public Integer getDailyEarnings() { return dailyEarnings; }
    public void setDailyEarnings(Integer dailyEarnings) { this.dailyEarnings = dailyEarnings; }
    public Integer getActiveHours() { return activeHours; }
    public void setActiveHours(Integer activeHours) { this.activeHours = activeHours; }
    public Integer getExperienceMonths() { return experienceMonths; }
    public void setExperienceMonths(Integer experienceMonths) { this.experienceMonths = experienceMonths; }
    public Integer getDaysPerWeek() { return daysPerWeek; }
    public void setDaysPerWeek(Integer daysPerWeek) { this.daysPerWeek = daysPerWeek; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}