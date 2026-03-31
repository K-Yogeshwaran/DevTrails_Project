package com.devtrails.backend.admin;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdminDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlaggedClaim {
        private String        claimId;
        private String        workerId;
        private String        workerName;
        private String        workerPhone;
        private String        triggerType;
        private String        zoneId;
        private BigDecimal    payoutAmount;
        private BigDecimal    fraudScore;
        private LocalDateTime triggeredAt;
        private LocalDateTime processedAt;
    }
}
