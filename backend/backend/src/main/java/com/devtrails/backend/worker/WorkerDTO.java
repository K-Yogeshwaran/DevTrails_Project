package com.devtrails.backend.worker;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

public class WorkerDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
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

        @NotBlank(message = "Shift is required")
        private String shift;

        private Integer experienceMonths = 0;
        private Integer daysPerWeek = 6;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {

        @NotBlank(message = "Phone is required")
        private String phone;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private String workerId;
        private String name;
        private String phone;
        private String zoneId;
        private String persona;
        private Integer dailyEarnings;
        private String token;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileResponse {
        private String workerId;
        private String name;
        private String phone;
        private String email;
        private String zoneId;
        private String persona;
        private Integer dailyEarnings;
        private Integer activeHours;
        private String shift;
        private Integer experienceMonths;
        private Integer daysPerWeek;
        private Boolean isActive;
        private LocalDateTime createdAt;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private Integer dailyEarnings;
        private Integer activeHours;
        private String shift;
        private String zoneId;
        private Integer daysPerWeek;
    }
}
