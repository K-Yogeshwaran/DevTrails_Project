/* package com.devtrails.backend.payout.gateway;

import java.math.BigDecimal;

public interface PaymentGateway {
    
    String getGatewayName();
    
    PayoutResult initiatePayout(PayoutRequest request);
    
    PayoutStatus checkPayoutStatus(String transactionId);
    
    boolean validateWebhookSignature(String payload, String signature);
    
    RefundResult refundPayout(String transactionId, BigDecimal amount, String reason);
    
    // DTOs
    class PayoutRequest {
        private String workerId;
        private String claimId;
        private String referenceId;
        private BigDecimal amount;
        private String upiId;
        private String accountNumber;
        private String ifscCode;
        private String beneficiaryName;
        
        // Constructors
        public PayoutRequest() {}
        
        public PayoutRequest(String workerId, String claimId, String referenceId, 
                          BigDecimal amount, String upiId, String accountNumber, 
                          String ifscCode, String beneficiaryName) {
            this.workerId = workerId;
            this.claimId = claimId;
            this.referenceId = referenceId;
            this.amount = amount;
            this.upiId = upiId;
            this.accountNumber = accountNumber;
            this.ifscCode = ifscCode;
            this.beneficiaryName = beneficiaryName;
        }
        
        // Getters and setters
        public String getWorkerId() { return workerId; }
        public void setWorkerId(String workerId) { this.workerId = workerId; }
        public String getClaimId() { return claimId; }
        public void setClaimId(String claimId) { this.claimId = claimId; }
        public String getReferenceId() { return referenceId; }
        public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getUpiId() { return upiId; }
        public void setUpiId(String upiId) { this.upiId = upiId; }
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
        public String getIfscCode() { return ifscCode; }
        public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
        public String getBeneficiaryName() { return beneficiaryName; }
        public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }
    }
    
    class PayoutResult {
        private boolean success;
        private String transactionId;
        private String status;
        private String message;
        private String gatewayResponse;
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private boolean success;
            private String transactionId;
            private String status;
            private String message;
            private String gatewayResponse;
            
            public Builder success(boolean success) { this.success = success; return this; }
            public Builder transactionId(String transactionId) { this.transactionId = transactionId; return this; }
            public Builder status(String status) { this.status = status; return this; }
            public Builder message(String message) { this.message = message; return this; }
            public Builder gatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; return this; }
            public PayoutResult build() {
                PayoutResult result = new PayoutResult();
                result.success = this.success;
                result.transactionId = this.transactionId;
                result.status = this.status;
                result.message = this.message;
                result.gatewayResponse = this.gatewayResponse;
                return result;
            }
        }
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getGatewayResponse() { return gatewayResponse; }
        public void setGatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; }
    }
    
    class PayoutStatus {
        private String transactionId;
        private String status;
        private BigDecimal amount;
        private String message;
        private String gatewayResponse;
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private String transactionId;
            private String status;
            private BigDecimal amount;
            private String message;
            private String gatewayResponse;
            
            public Builder transactionId(String transactionId) { this.transactionId = transactionId; return this; }
            public Builder status(String status) { this.status = status; return this; }
            public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
            public Builder message(String message) { this.message = message; return this; }
            public Builder gatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; return this; }
            public PayoutStatus build() {
                PayoutStatus status = new PayoutStatus();
                status.transactionId = this.transactionId;
                status.status = this.status;
                status.amount = this.amount;
                status.message = this.message;
                status.gatewayResponse = this.gatewayResponse;
                return status;
            }
        }
        
        // Getters and setters
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getGatewayResponse() { return gatewayResponse; }
        public void setGatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; }
    }
    
    class RefundResult {
        private boolean success;
        private String refundId;
        private String message;
        private String gatewayResponse;
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private boolean success;
            private String refundId;
            private String message;
            private String gatewayResponse;
            
            public Builder success(boolean success) { this.success = success; return this; }
            public Builder refundId(String refundId) { this.refundId = refundId; return this; }
            public Builder message(String message) { this.message = message; return this; }
            public Builder gatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; return this; }
            public RefundResult build() {
                RefundResult result = new RefundResult();
                result.success = this.success;
                result.refundId = this.refundId;
                result.message = this.message;
                result.gatewayResponse = this.gatewayResponse;
                return result;
            }
        }
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getRefundId() { return refundId; }
        public void setRefundId(String refundId) { this.refundId = refundId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getGatewayResponse() { return gatewayResponse; }
        public void setGatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; }
    }
}
 */