package com.devtrails.backend.claims;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ClaimDTO {


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClaimResponse {
        private String claimId;
        private String workerId;
        private String policyNumber;
        private String triggerType;
        private BigDecimal triggerValue;
        private String zoneId;
        private BigDecimal disruptedHours;
        private BigDecimal payoutAmount;
        private BigDecimal fraudScore;
        private String status;
        private LocalDateTime triggeredAt;
        private LocalDateTime processedAt;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClaimSummary {
        private String claimId;
        private String triggerType;
        private BigDecimal payoutAmount;
        private String status;
        private LocalDateTime triggeredAt;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Analytics {
        private long totalClaims;
        private long approvedClaims;
        private long rejectedClaims;
        private long flaggedClaims;
        private BigDecimal totalPaidOut;
        private double approvalRate;
        private double fraudRate;
    }
}
