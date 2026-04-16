package com.devtrails.backend.claims;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ClaimDTO {

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

        public ClaimResponse() {}

        public ClaimResponse(String claimId, String workerId, String policyNumber, String triggerType,
                             BigDecimal triggerValue, String zoneId, BigDecimal disruptedHours,
                             BigDecimal payoutAmount, BigDecimal fraudScore, String status,
                             LocalDateTime triggeredAt, LocalDateTime processedAt, String message) {
            this.claimId = claimId;
            this.workerId = workerId;
            this.policyNumber = policyNumber;
            this.triggerType = triggerType;
            this.triggerValue = triggerValue;
            this.zoneId = zoneId;
            this.disruptedHours = disruptedHours;
            this.payoutAmount = payoutAmount;
            this.fraudScore = fraudScore;
            this.status = status;
            this.triggeredAt = triggeredAt;
            this.processedAt = processedAt;
            this.message = message;
        }

        public String getClaimId() { return claimId; }
        public void setClaimId(String claimId) { this.claimId = claimId; }
        public String getWorkerId() { return workerId; }
        public void setWorkerId(String workerId) { this.workerId = workerId; }
        public String getPolicyNumber() { return policyNumber; }
        public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
        public String getTriggerType() { return triggerType; }
        public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
        public BigDecimal getTriggerValue() { return triggerValue; }
        public void setTriggerValue(BigDecimal triggerValue) { this.triggerValue = triggerValue; }
        public String getZoneId() { return zoneId; }
        public void setZoneId(String zoneId) { this.zoneId = zoneId; }
        public BigDecimal getDisruptedHours() { return disruptedHours; }
        public void setDisruptedHours(BigDecimal disruptedHours) { this.disruptedHours = disruptedHours; }
        public BigDecimal getPayoutAmount() { return payoutAmount; }
        public void setPayoutAmount(BigDecimal payoutAmount) { this.payoutAmount = payoutAmount; }
        public BigDecimal getFraudScore() { return fraudScore; }
        public void setFraudScore(BigDecimal fraudScore) { this.fraudScore = fraudScore; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getTriggeredAt() { return triggeredAt; }
        public void setTriggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; }
        public LocalDateTime getProcessedAt() { return processedAt; }
        public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class ClaimSummary {
        private String claimId;
        private String triggerType;
        private BigDecimal payoutAmount;
        private String status;
        private LocalDateTime triggeredAt;

        public ClaimSummary() {}

        public ClaimSummary(String claimId, String triggerType, BigDecimal payoutAmount,
                            String status, LocalDateTime triggeredAt) {
            this.claimId = claimId;
            this.triggerType = triggerType;
            this.payoutAmount = payoutAmount;
            this.status = status;
            this.triggeredAt = triggeredAt;
        }

        public String getClaimId() { return claimId; }
        public void setClaimId(String claimId) { this.claimId = claimId; }
        public String getTriggerType() { return triggerType; }
        public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
        public BigDecimal getPayoutAmount() { return payoutAmount; }
        public void setPayoutAmount(BigDecimal payoutAmount) { this.payoutAmount = payoutAmount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getTriggeredAt() { return triggeredAt; }
        public void setTriggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; }
    }

    public static class Analytics {
        private long totalClaims;
        private long approvedClaims;
        private long rejectedClaims;
        private long flaggedClaims;
        private BigDecimal totalPaidOut;
        private double approvalRate;
        private double fraudRate;

        public Analytics() {}

        public Analytics(long totalClaims, long approvedClaims, long rejectedClaims,
                         long flaggedClaims, BigDecimal totalPaidOut,
                         double approvalRate, double fraudRate) {
            this.totalClaims = totalClaims;
            this.approvedClaims = approvedClaims;
            this.rejectedClaims = rejectedClaims;
            this.flaggedClaims = flaggedClaims;
            this.totalPaidOut = totalPaidOut;
            this.approvalRate = approvalRate;
            this.fraudRate = fraudRate;
        }

        public long getTotalClaims() { return totalClaims; }
        public void setTotalClaims(long totalClaims) { this.totalClaims = totalClaims; }
        public long getApprovedClaims() { return approvedClaims; }
        public void setApprovedClaims(long approvedClaims) { this.approvedClaims = approvedClaims; }
        public long getRejectedClaims() { return rejectedClaims; }
        public void setRejectedClaims(long rejectedClaims) { this.rejectedClaims = rejectedClaims; }
        public long getFlaggedClaims() { return flaggedClaims; }
        public void setFlaggedClaims(long flaggedClaims) { this.flaggedClaims = flaggedClaims; }
        public BigDecimal getTotalPaidOut() { return totalPaidOut; }
        public void setTotalPaidOut(BigDecimal totalPaidOut) { this.totalPaidOut = totalPaidOut; }
        public double getApprovalRate() { return approvalRate; }
        public void setApprovalRate(double approvalRate) { this.approvalRate = approvalRate; }
        public double getFraudRate() { return fraudRate; }
        public void setFraudRate(double fraudRate) { this.fraudRate = fraudRate; }
    }
}
