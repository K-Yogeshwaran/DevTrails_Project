package com.devtrails.backend.policy;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PolicyDTO {

    public record CreateRequest(
        @NotBlank(message = "Worker ID is required")
        String workerId,

        @NotBlank(message = "Tier is required")
        @Pattern(regexp = "basic|standard|premium",
                message = "Tier must be: basic, standard, or premium")
        String tier,

        @NotBlank(message = "Season is required")
        @Pattern(regexp = "summer|monsoon|winter|spring",
                message = "Season must be: summer, monsoon, winter, or spring")
        String season
    ) {}

    public record PolicyResponse(
        String policyNumber,
        String workerId,
        String tier,
        BigDecimal weeklyPremium,    // ML-calculated actual premium
        BigDecimal coverageCap,      // max payout this week
        BigDecimal coverageUsed,     // how much has been paid out
        BigDecimal coverageRemaining,// cap minus used
        String season,
        String status,
        LocalDate weekStart,
        LocalDate weekEnd,
        LocalDateTime createdAt,
        String message
    ) {}

    public record PolicySummary(
        String policyNumber,
        String tier,
        BigDecimal weeklyPremium,
        BigDecimal coverageCap,
        BigDecimal coverageUsed,
        String status,
        LocalDate weekStart,
        LocalDate weekEnd
    ) {}

    public record CoverageCheck(
        boolean isCovered,
        String policyNumber,
        BigDecimal coverageRemaining,
        String reason // why not covered, if applicable
    ) {}
}