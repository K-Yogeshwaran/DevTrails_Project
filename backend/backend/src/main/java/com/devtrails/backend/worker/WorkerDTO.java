package com.devtrails.backend.worker;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public class WorkerDTO {

    public static class RegisterRequest {

        @NotBlank(message = "Name is required")
        private String name;

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter valid 10-digit mobile number")
        private String phone;

        @Email(message = "Enter a valid email")
        private String email;

        @NotBlank(message = "Zone is required")
        private String zoneId;

        @NotBlank(message = "Persona is required")
        private String persona;

        @NotNull(message = "Daily earnings is required")
        @Min(value = 100, message = "Daily earnings must be at least ₹100")
        @Max(value = 10000, message = "Daily earnings cannot exceed ₹10,000")
        private Integer dailyEarnings;

        @NotNull(message = "Active hours is required")
        @Min(value = 1) @Max(value = 16)
        private Integer activeHours;

        private Integer experienceMonths = 0;
        private Integer daysPerWeek = 6;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        public RegisterRequest() {}

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
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LoginRequest {

        @NotBlank(message = "Phone is required")
        private String phone;

        @NotBlank(message = "Password is required")
        private String password;

        public LoginRequest() {}

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class AuthResponse {
        private String workerId;
        private String name;
        private String phone;
        private String zoneId;
        private String persona;
        private Integer dailyEarnings;
        private String token;
        private String message;

        public AuthResponse() {}

        public AuthResponse(String workerId, String name, String phone, String zoneId,
                            String persona, Integer dailyEarnings, String token, String message) {
            this.workerId = workerId;
            this.name = name;
            this.phone = phone;
            this.zoneId = zoneId;
            this.persona = persona;
            this.dailyEarnings = dailyEarnings;
            this.token = token;
            this.message = message;
        }

        public String getWorkerId() { return workerId; }
        public void setWorkerId(String workerId) { this.workerId = workerId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getZoneId() { return zoneId; }
        public void setZoneId(String zoneId) { this.zoneId = zoneId; }
        public String getPersona() { return persona; }
        public void setPersona(String persona) { this.persona = persona; }
        public Integer getDailyEarnings() { return dailyEarnings; }
        public void setDailyEarnings(Integer dailyEarnings) { this.dailyEarnings = dailyEarnings; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class ProfileResponse {
        private String workerId;
        private String name;
        private String phone;
        private String email;
        private String zoneId;
        private String persona;
        private Integer dailyEarnings;
        private Integer activeHours;
        private Integer experienceMonths;
        private Integer daysPerWeek;
        private Boolean isActive;
        private LocalDateTime createdAt;

        public ProfileResponse() {}

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
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class UpdateRequest {
        private Integer dailyEarnings;
        private Integer activeHours;
        private String zoneId;
        private Integer daysPerWeek;

        public UpdateRequest() {}

        public Integer getDailyEarnings() { return dailyEarnings; }
        public void setDailyEarnings(Integer dailyEarnings) { this.dailyEarnings = dailyEarnings; }
        public Integer getActiveHours() { return activeHours; }
        public void setActiveHours(Integer activeHours) { this.activeHours = activeHours; }
        public String getZoneId() { return zoneId; }
        public void setZoneId(String zoneId) { this.zoneId = zoneId; }
        public Integer getDaysPerWeek() { return daysPerWeek; }
        public void setDaysPerWeek(Integer daysPerWeek) { this.daysPerWeek = daysPerWeek; }
    }
}
