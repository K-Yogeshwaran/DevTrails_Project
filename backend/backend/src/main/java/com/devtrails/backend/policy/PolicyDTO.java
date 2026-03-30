package com.devtrails.backend.policy;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PolicyDTO {


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {

        @NotBlank(message = "Worker ID is required")
        private String workerId;


        @NotBlank(message = "Tier is required")
        @Pattern(regexp = "basic|standard|premium",
                message = "Tier must be: basic, standard, or premium")
        private String tier;

        @NotBlank(message = "Season is required")
        @Pattern(regexp = "summer|monsoon|winter|spring",
                message = "Season must be: summer, monsoon, winter, or spring")
        private String season;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PolicyResponse {
        private String policyNumber;
        private String workerId;
        private String tier;
        private BigDecimal weeklyPremium;    // ML-calculated actual premium
        private BigDecimal coverageCap;      // max payout this week
        private BigDecimal coverageUsed;     // how much has been paid out
        private BigDecimal coverageRemaining;// cap minus used
        private String season;
        private String status;
        private LocalDate weekStart;
        private LocalDate weekEnd;
        private LocalDateTime createdAt;
        private String message;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PolicySummary {
        private String policyNumber;
        private String tier;
        private BigDecimal weeklyPremium;
        private BigDecimal coverageCap;
        private BigDecimal coverageUsed;
        private String status;
        private LocalDate weekStart;
        private LocalDate weekEnd;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoverageCheck {
        private boolean isCovered;
        private String policyNumber;
        private BigDecimal coverageRemaining;
        private String reason; // why not covered, if applicable
    }
}