package com.devtrails.backend.payout;

import com.devtrails.backend.claims.Claim;
import com.devtrails.backend.claims.ClaimRepository;
import com.devtrails.backend.config.ApiException;
import com.devtrails.backend.worker.Worker;
import com.devtrails.backend.worker.WorkerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PayoutController.class)
@AutoConfigureWebMvc
@ExtendWith(MockitoExtension.class)
class PayoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PayoutService payoutService;

    @MockBean
    private PayoutRepository payoutRepository;

    @MockBean
    private ClaimRepository claimRepository;

    @MockBean
    private WorkerRepository workerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Payout testPayout;
    private Claim testClaim;
    private Worker testWorker;

    @BeforeEach
    void setUp() {
        testPayout = createTestPayout();
        testClaim = createTestClaim();
        testWorker = createTestWorker();
    }

    @Test
    void testInitiatePayout_Success() throws Exception {
        // Arrange
        PayoutController.PayoutInitiateRequest request = new PayoutController.PayoutInitiateRequest();
        request.setClaimId("CLM-TEST001-1234567890");
        request.setPaymentMethod("RAZORPAY");
        request.setUpiId("test@upi");
        request.setAccountNumber("1234567890");
        request.setIfscCode("HDFC0001234");

        when(workerRepository.findByWorkerId("WORKER001")).thenReturn(Optional.of(testWorker));
        when(claimRepository.findByClaimId("CLM-TEST001-1234567890")).thenReturn(Optional.of(testClaim));
        when(payoutRepository.findByClaimIdOrderByCreatedAtDesc("CLM-TEST001-1234567890")).thenReturn(List.of());
        when(payoutService.initiatePayout(any(), any(), any(), any(), any(), any())).thenReturn(testPayout);

        // Act & Assert
        mockMvc.perform(post("/api/payouts/initiate")
                .with(SecurityMockMvcRequestPostProcessors.user("WORKER001")
                        .authorities(new SimpleGrantedAuthority("ROLE_WORKER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payoutId").value(testPayout.getPayoutId()))
                .andExpect(jsonPath("$.claimId").value(testPayout.getClaimId()))
                .andExpect(jsonPath("$.workerId").value(testPayout.getWorkerId()))
                .andExpect(jsonPath("$.paymentMethod").value(testPayout.getPaymentMethod()))
                .andExpect(jsonPath("$.amount").value(testPayout.getAmount()))
                .andExpect(jsonPath("$.status").value(testPayout.getStatus()));
    }

    @Test
    void testInitiatePayout_ClaimNotFound() throws Exception {
        // Arrange
        PayoutController.PayoutInitiateRequest request = new PayoutController.PayoutInitiateRequest();
        request.setClaimId("NONEXISTENT");
        request.setPaymentMethod("RAZORPAY");

        when(workerRepository.findByWorkerId("WORKER001")).thenReturn(Optional.of(testWorker));
        when(claimRepository.findByClaimId("NONEXISTENT")).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(post("/api/payouts/initiate")
                .with(SecurityMockMvcRequestPostProcessors.user("WORKER001")
                        .authorities(new SimpleGrantedAuthority("ROLE_WORKER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Claim not found"));
    }

    @Test
    void testInitiatePayout_ClaimNotApproved() throws Exception {
        // Arrange
        testClaim.setStatus("pending"); // Not approved
        PayoutController.PayoutInitiateRequest request = new PayoutController.PayoutInitiateRequest();
        request.setClaimId("CLM-TEST001-1234567890");
        request.setPaymentMethod("RAZORPAY");

        when(workerRepository.findByWorkerId("WORKER001")).thenReturn(Optional.of(testWorker));
        when(claimRepository.findByClaimId("CLM-TEST001-1234567890")).thenReturn(Optional.of(testClaim));
        when(payoutRepository.findByClaimIdOrderByCreatedAtDesc("CLM-TEST001-1234567890")).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(post("/api/payouts/initiate")
                .with(SecurityMockMvcRequestPostProcessors.user("WORKER001")
                        .authorities(new SimpleGrantedAuthority("ROLE_WORKER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Claim is not approved"));
    }

    @Test
    void testInitiatePayout_ClaimNotBelongToWorker() throws Exception {
        // Arrange
        testClaim.setWorkerId("OTHER_WORKER"); // Different worker
        PayoutController.PayoutInitiateRequest request = new PayoutController.PayoutInitiateRequest();
        request.setClaimId("CLM-TEST001-1234567890");
        request.setPaymentMethod("RAZORPAY");

        when(workerRepository.findByWorkerId("WORKER001")).thenReturn(Optional.of(testWorker));
        when(claimRepository.findByClaimId("CLM-TEST001-1234567890")).thenReturn(Optional.of(testClaim));
        when(payoutRepository.findByClaimIdOrderByCreatedAtDesc("CLM-TEST001-1234567890")).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(post("/api/payouts/initiate")
                .with(SecurityMockMvcRequestPostProcessors.user("WORKER001")
                        .authorities(new SimpleGrantedAuthority("ROLE_WORKER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Claim does not belong to worker"));
    }

    @Test
    void testInitiatePayout_InvalidPaymentMethod() throws Exception {
        // Arrange
        PayoutController.PayoutInitiateRequest request = new PayoutController.PayoutInitiateRequest();
        request.setClaimId("CLM-TEST001-1234567890");
        request.setPaymentMethod("INVALID");

        when(workerRepository.findByWorkerId("WORKER001")).thenReturn(Optional.of(testWorker));
        when(claimRepository.findByClaimId("CLM-TEST001-1234567890")).thenReturn(Optional.of(testClaim));
        when(payoutRepository.findByClaimIdOrderByCreatedAtDesc("CLM-TEST001-1234567890")).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(post("/api/payouts/initiate")
                .with(SecurityMockMvcRequestPostProcessors.user("WORKER001")
                        .authorities(new SimpleGrantedAuthority("ROLE_WORKER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testGetPayout_Success() throws Exception {
        // Arrange
        when(payoutRepository.findByPayoutId("PYT-WORKER001-1234567890-001"))
                .thenReturn(Optional.of(testPayout));

        // Act & Assert
        mockMvc.perform(get("/api/payouts/PYT-WORKER001-1234567890-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payoutId").value(testPayout.getPayoutId()))
                .andExpect(jsonPath("$.claimId").value(testPayout.getClaimId()))
                .andExpect(jsonPath("$.workerId").value(testPayout.getWorkerId()))
                .andExpect(jsonPath("$.paymentMethod").value(testPayout.getPaymentMethod()))
                .andExpect(jsonPath("$.amount").value(testPayout.getAmount()))
                .andExpect(jsonPath("$.status").value(testPayout.getStatus()));
    }

    @Test
    void testGetPayout_NotFound() throws Exception {
        // Arrange
        when(payoutRepository.findByPayoutId("NONEXISTENT")).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/payouts/NONEXISTENT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Payout not found"));
    }

    @Test
    void testGetWorkerPayouts_Success() throws Exception {
        // Arrange
        List<Payout> expectedPayouts = List.of(testPayout, createTestPayout());
        when(payoutRepository.findByWorkerIdOrderByCreatedAtDesc("WORKER001"))
                .thenReturn(expectedPayouts);

        // Act & Assert
        mockMvc.perform(get("/api/payouts/worker/WORKER001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(expectedPayouts.size()))
                .andExpect(jsonPath("$[0].payoutId").value(testPayout.getPayoutId()));
    }

    @Test
    void testGetClaimPayouts_Success() throws Exception {
        // Arrange
        List<Payout> expectedPayouts = List.of(testPayout, createTestPayout());
        when(payoutRepository.findByClaimIdOrderByCreatedAtDesc("CLM-TEST001-1234567890"))
                .thenReturn(expectedPayouts);

        // Act & Assert
        mockMvc.perform(get("/api/payouts/claim/CLM-TEST001-1234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(expectedPayouts.size()))
                .andExpect(jsonPath("$[0].payoutId").value(testPayout.getPayoutId()));
    }

    @Test
    void testCheckPayoutStatus_Success() throws Exception {
        // Arrange
        Payout updatedPayout = createTestPayout();
        updatedPayout.setStatus("COMPLETED");
        updatedPayout.setCompletedAt(LocalDateTime.now());

        when(payoutRepository.findByPayoutId("PYT-WORKER001-1234567890-001"))
                .thenReturn(Optional.of(testPayout));
        when(payoutService.checkPayoutStatus("PYT-WORKER001-1234567890-001"))
                .thenReturn(updatedPayout);

        // Act & Assert
        mockMvc.perform(post("/api/payouts/PYT-WORKER001-1234567890-001/status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payoutId").value(updatedPayout.getPayoutId()))
                .andExpect(jsonPath("$.status").value(updatedPayout.getStatus()));
    }

    @Test
    void testCheckPayoutStatus_NotFound() throws Exception {
        // Arrange
        when(payoutRepository.findByPayoutId("NONEXISTENT")).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(post("/api/payouts/NONEXISTENT/status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Payout not found"));
    }

    @Test
    void testRefundPayout_Success() throws Exception {
        // Arrange
        Payout refundedPayout = createTestPayout();
        refundedPayout.setStatus("REFUNDED");
        refundedPayout.setFailureReason("Customer requested refund");

        when(payoutRepository.findByPayoutId("PYT-WORKER001-1234567890-001"))
                .thenReturn(Optional.of(testPayout));
        when(payoutService.refundPayout(eq("PYT-WORKER001-1234567890-001"), any()))
                .thenReturn(refundedPayout);

        PayoutController.RefundRequest refundRequest = new PayoutController.RefundRequest();
        refundRequest.setReason("Customer requested refund");

        // Act & Assert
        mockMvc.perform(post("/api/payouts/PYT-WORKER001-1234567890-001/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payoutId").value(refundedPayout.getPayoutId()))
                .andExpect(jsonPath("$.status").value(refundedPayout.getStatus()));
    }

    @Test
    void testRefundPayout_NotFound() throws Exception {
        // Arrange
        when(payoutRepository.findByPayoutId("NONEXISTENT")).thenReturn(Optional.empty());

        PayoutController.RefundRequest refundRequest = new PayoutController.RefundRequest();
        refundRequest.setReason("Customer requested refund");

        // Act & Assert
        mockMvc.perform(post("/api/payouts/NONEXISTENT/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Payout not found"));
    }

    @Test
    void testHandleRazorpayWebhook_Success() throws Exception {
        // Arrange
        String webhookPayload = "{\"payout_id\":\"PYT-WORKER001-1234567890-001\",\"status\":\"COMPLETED\"}";
        String signature = "test_signature";

        when(payoutRepository.findByPayoutId("PYT-WORKER001-1234567890-001"))
                .thenReturn(Optional.of(testPayout));
        when(payoutService.updatePayoutFromWebhook(any(), any(), any(), any()))
                .thenReturn(testPayout);

        // Act & Assert
        mockMvc.perform(post("/api/payouts/webhook/razorpay")
                .header("X-Razorpay-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook received"));
    }

    @Test
    void testHandleStripeWebhook_Success() throws Exception {
        // Arrange
        String webhookPayload = "{\"payout_id\":\"PYT-WORKER001-1234567890-001\",\"status\":\"paid\"}";
        String signature = "test_signature";

        when(payoutRepository.findByPayoutId("PYT-WORKER001-1234567890-001"))
                .thenReturn(Optional.of(testPayout));
        when(payoutService.updatePayoutFromWebhook(any(), any(), any(), any()))
                .thenReturn(testPayout);

        // Act & Assert
        mockMvc.perform(post("/api/payouts/webhook/stripe")
                .header("Stripe-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook received"));
    }

    @Test
    void testHandleUPIWebhook_Success() throws Exception {
        // Arrange
        String webhookPayload = "{\"payout_id\":\"PYT-WORKER001-1234567890-001\",\"status\":\"SUCCESS\"}";
        String signature = "test_signature";

        when(payoutRepository.findByPayoutId("PYT-WORKER001-1234567890-001"))
                .thenReturn(Optional.of(testPayout));
        when(payoutService.updatePayoutFromWebhook(any(), any(), any(), any()))
                .thenReturn(testPayout);

        // Act & Assert
        mockMvc.perform(post("/api/payouts/webhook/upi")
                .header("X-UPI-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook received"));
    }

    @Test
    void testGetPayoutAnalytics_Success() throws Exception {
        // Arrange
        when(payoutRepository.findByStatusInOrderByCreatedAtDesc(any()))
                .thenReturn(List.of(testPayout));
        when(payoutRepository.getDailyPayoutStatsSince(any()))
                .thenReturn(List.of(new Object[]{"2024-01-01", 5L, new BigDecimal("2500.00")}));
        when(payoutRepository.getPayoutBreakdownSince(any()))
                .thenReturn(List.of(new Object[]{"RAZORPAY", "COMPLETED", 5L}));

        // Act & Assert
        mockMvc.perform(get("/api/payouts/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPayouts").isNumber())
                .andExpect(jsonPath("$.dailyStats").isArray())
                .andExpect(jsonPath("$.methodBreakdown").isArray());
    }

    @Test
    void testHealth_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/payouts/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.service").value("payout-service"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    private Claim createTestClaim() {
        Claim claim = new Claim();
        claim.setClaimId("CLM-TEST001-1234567890");
        claim.setWorkerId("WORKER001");
        claim.setPayoutAmount(new BigDecimal("500.00"));
        claim.setStatus("approved");
        claim.setTriggeredAt(LocalDateTime.now());
        return claim;
    }

    private Worker createTestWorker() {
        Worker worker = new Worker();
        worker.setWorkerId("WORKER001");
        worker.setName("Test Worker");
        worker.setPhone("9876543210");
        worker.setEmail("test@example.com");
        worker.setZoneId("zone-001");
        worker.setPersona("delivery");
        worker.setDailyEarnings(500);
        worker.setActiveHours(8);
        worker.setExperienceMonths(12);
        worker.setDaysPerWeek(6);
        return worker;
    }

    private Payout createTestPayout() {
        Payout payout = new Payout();
        payout.setPayoutId("PYT-WORKER001-1234567890-001");
        payout.setClaimId("CLM-TEST001-1234567890");
        payout.setWorkerId("WORKER001");
        payout.setPaymentMethod("RAZORPAY");
        payout.setAmount(new BigDecimal("500.00"));
        payout.setCurrency("INR");
        payout.setStatus("PENDING");
        payout.setUpiId("test@upi");
        payout.setAccountNumber("1234567890");
        payout.setIfscCode("HDFC0001234");
        payout.setBeneficiaryName("Test Worker");
        payout.setCreatedAt(LocalDateTime.now());
        return payout;
    }
}
