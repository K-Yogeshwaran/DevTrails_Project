package com.devtrails.backend.payout;

import com.devtrails.backend.claims.Claim;
import com.devtrails.backend.payout.gateway.PaymentGateway;
import com.devtrails.backend.payout.gateway.RazorpayPayoutGateway;
import com.devtrails.backend.payout.gateway.STRIPEPayoutGateway;
import com.devtrails.backend.payout.gateway.UPISimulatorGateway;
import com.devtrails.backend.worker.Worker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {

    @Mock
    private PayoutRepository payoutRepository;

    @Mock
    private RazorpayPayoutGateway razorpayGateway;

    @Mock
    private STRIPEPayoutGateway stripeGateway;

    @Mock
    private UPISimulatorGateway upiSimulatorGateway;

    @InjectMocks
    private PayoutService payoutService;

    private Claim testClaim;
    private Worker testWorker;

    @BeforeEach
    void setUp() {
        // Set configuration values
        ReflectionTestUtils.setField(payoutService, "minPayoutAmount", new BigDecimal("50"));
        ReflectionTestUtils.setField(payoutService, "maxPayoutAmount", new BigDecimal("5000"));
        ReflectionTestUtils.setField(payoutService, "autoApproveThreshold", new BigDecimal("1000"));

        testClaim = createTestClaim();
        testWorker = createTestWorker();
    }

    @Test
    void testInitiatePayout_Success() {
        // Arrange
        when(payoutRepository.findByClaimIdOrderByCreatedAtDesc(testClaim.getClaimId()))
                .thenReturn(List.of());
        when(payoutRepository.save(any(Payout.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentGateway.PayoutResult mockResult = PaymentGateway.PayoutResult.builder()
                .success(true)
                .transactionId("txn_123456")
                .status("COMPLETED")
                .message("Success")
                .build();

        when(razorpayGateway.initiatePayout(any())).thenReturn(mockResult);

        // Act
        Payout result = payoutService.initiatePayout(
                testClaim, testWorker, "RAZORPAY", 
                "test@upi", "1234567890", "HDFC0001234"
        );

        // Assert
        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertEquals(testClaim.getClaimId(), result.getClaimId());
        assertEquals(testWorker.getWorkerId(), result.getWorkerId());
        assertEquals("RAZORPAY", result.getPaymentMethod());
        assertEquals(testClaim.getPayoutAmount(), result.getAmount());

        verify(payoutRepository).save(any(Payout.class));
    }

    @Test
    void testInitiatePayout_AmountBelowMinimum() {
        // Arrange
        testClaim.setPayoutAmount(new BigDecimal("25")); // Below minimum

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> payoutService.initiatePayout(
                        testClaim, testWorker, "RAZORPAY",
                        "test@upi", "1234567890", "HDFC0001234"
                )
        );

        assertEquals("Payout amount below minimum threshold: 50", exception.getMessage());
    }

    @Test
    void testInitiatePayout_AmountAboveMaximum() {
        // Arrange
        testClaim.setPayoutAmount(new BigDecimal("6000")); // Above maximum

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> payoutService.initiatePayout(
                        testClaim, testWorker, "RAZORPAY",
                        "test@upi", "1234567890", "HDFC0001234"
                )
        );

        assertEquals("Payout amount above maximum threshold: 5000", exception.getMessage());
    }

    @Test
    void testInitiatePayout_ExistingPayout() {
        // Arrange
        Payout existingPayout = createTestPayout();
        existingPayout.setStatus("PROCESSING");
        
        when(payoutRepository.findByClaimIdOrderByCreatedAtDesc(testClaim.getClaimId()))
                .thenReturn(List.of(existingPayout));

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> payoutService.initiatePayout(
                        testClaim, testWorker, "RAZORPAY",
                        "test@upi", "1234567890", "HDFC0001234"
                )
        );

        assertEquals("Payout already exists for claim: " + testClaim.getClaimId(), exception.getMessage());
    }

    @Test
    void testInitiatePayout_ExistingFailedPayout() {
        // Arrange
        Payout failedPayout = createTestPayout();
        failedPayout.setStatus("FAILED");
        
        when(payoutRepository.findByClaimIdOrderByCreatedAtDesc(testClaim.getClaimId()))
                .thenReturn(List.of(failedPayout));

        PaymentGateway.PayoutResult mockResult = PaymentGateway.PayoutResult.builder()
                .success(true)
                .transactionId("txn_123456")
                .status("COMPLETED")
                .message("Success")
                .build();

        when(razorpayGateway.initiatePayout(any())).thenReturn(mockResult);
        when(payoutRepository.save(any(Payout.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Payout result = payoutService.initiatePayout(
                testClaim, testWorker, "RAZORPAY",
                "test@upi", "1234567890", "HDFC0001234"
        );

        // Assert
        assertNotNull(result);
        verify(payoutRepository, times(2)).save(any(Payout.class));
    }

    @Test
    void testInitiatePayout_UnsupportedPaymentMethod() {
        // Arrange
        when(payoutRepository.findByClaimIdOrderByCreatedAtDesc(testClaim.getClaimId()))
                .thenReturn(List.of());
        when(payoutRepository.save(any(Payout.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Payout result = payoutService.initiatePayout(
                testClaim, testWorker, "UNSUPPORTED",
                "test@upi", "1234567890", "HDFC0001234"
        );

        // Assert
        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        // Note: The actual gateway failure would be caught in async processing
    }

    @Test
    void testCheckPayoutStatus_Success() {
        // Arrange
        Payout payout = createTestPayout();
        payout.setStatus("PROCESSING");
        payout.setPaymentGatewayId("txn_123456");

        when(payoutRepository.findByPayoutId(payout.getPayoutId()))
                .thenReturn(Optional.of(payout));

        PaymentGateway.PayoutStatus mockStatus = PaymentGateway.PayoutStatus.builder()
                .transactionId("txn_123456")
                .status("COMPLETED")
                .amount(payout.getAmount())
                .gatewayResponse("Completed successfully")
                .build();

        when(razorpayGateway.checkPayoutStatus("txn_123456")).thenReturn(mockStatus);
        when(payoutRepository.save(any(Payout.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Payout result = payoutService.checkPayoutStatus(payout.getPayoutId());

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertNotNull(result.getCompletedAt());
        verify(payoutRepository).save(any(Payout.class));
    }

    @Test
    void testCheckPayoutStatus_AlreadyCompleted() {
        // Arrange
        Payout payout = createTestPayout();
        payout.setStatus("COMPLETED");

        when(payoutRepository.findByPayoutId(payout.getPayoutId()))
                .thenReturn(Optional.of(payout));

        // Act
        Payout result = payoutService.checkPayoutStatus(payout.getPayoutId());

        // Assert
        assertEquals(payout, result);
        verify(razorpayGateway, never()).checkPayoutStatus(any());
    }

    @Test
    void testCheckPayoutStatus_PayoutNotFound() {
        // Arrange
        when(payoutRepository.findByPayoutId("NONEXISTENT"))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> payoutService.checkPayoutStatus("NONEXISTENT")
        );

        assertEquals("Payout not found: NONEXISTENT", exception.getMessage());
    }

    @Test
    void testRefundPayout_Success() {
        // Arrange
        Payout payout = createTestPayout();
        payout.setStatus("COMPLETED");
        payout.setPaymentGatewayId("txn_123456");

        when(payoutRepository.findByPayoutId(payout.getPayoutId()))
                .thenReturn(Optional.of(payout));

        PaymentGateway.RefundResult mockRefund = PaymentGateway.RefundResult.builder()
                .success(true)
                .refundId("refund_123456")
                .message("Refund successful")
                .build();

        when(razorpayGateway.refundPayout(eq("txn_123456"), eq(payout.getAmount()), any()))
                .thenReturn(mockRefund);
        when(payoutRepository.save(any(Payout.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Payout result = payoutService.refundPayout(payout.getPayoutId(), "Customer requested refund");

        // Assert
        assertNotNull(result);
        assertEquals("REFUNDED", result.getStatus());
        assertTrue(result.getFailureReason().contains("Customer requested refund"));
        verify(payoutRepository).save(any(Payout.class));
    }

    @Test
    void testRefundPayout_NonCompletedPayout() {
        // Arrange
        Payout payout = createTestPayout();
        payout.setStatus("PROCESSING");

        when(payoutRepository.findByPayoutId(payout.getPayoutId()))
                .thenReturn(Optional.of(payout));

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> payoutService.refundPayout(payout.getPayoutId(), "Customer requested refund")
        );

        assertEquals("Cannot refund non-completed payout: " + payout.getPayoutId(), exception.getMessage());
    }

    @Test
    void testRefundPayout_RefundFailed() {
        // Arrange
        Payout payout = createTestPayout();
        payout.setStatus("COMPLETED");
        payout.setPaymentGatewayId("txn_123456");

        when(payoutRepository.findByPayoutId(payout.getPayoutId()))
                .thenReturn(Optional.of(payout));

        PaymentGateway.RefundResult mockRefund = PaymentGateway.RefundResult.builder()
                .success(false)
                .message("Refund failed")
                .build();

        when(razorpayGateway.refundPayout(eq("txn_123456"), eq(payout.getAmount()), any()))
                .thenReturn(mockRefund);

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> payoutService.refundPayout(payout.getPayoutId(), "Customer requested refund")
        );

        assertEquals("Refund failed: Refund failed", exception.getMessage());
    }

    @Test
    void testGetWorkerPayouts() {
        // Arrange
        List<Payout> expectedPayouts = List.of(createTestPayout(), createTestPayout());
        when(payoutRepository.findByWorkerIdOrderByCreatedAtDesc(testWorker.getWorkerId()))
                .thenReturn(expectedPayouts);

        // Act
        List<Payout> result = payoutService.getWorkerPayouts(testWorker.getWorkerId());

        // Assert
        assertEquals(expectedPayouts, result);
        verify(payoutRepository).findByWorkerIdOrderByCreatedAtDesc(testWorker.getWorkerId());
    }

    @Test
    void testGetClaimPayouts() {
        // Arrange
        List<Payout> expectedPayouts = List.of(createTestPayout(), createTestPayout());
        when(payoutRepository.findByClaimIdOrderByCreatedAtDesc(testClaim.getClaimId()))
                .thenReturn(expectedPayouts);

        // Act
        List<Payout> result = payoutService.getClaimPayouts(testClaim.getClaimId());

        // Assert
        assertEquals(expectedPayouts, result);
        verify(payoutRepository).findByClaimIdOrderByCreatedAtDesc(testClaim.getClaimId());
    }

    @Test
    void testGetPayout_Success() {
        // Arrange
        Payout expectedPayout = createTestPayout();
        when(payoutRepository.findByPayoutId(expectedPayout.getPayoutId()))
                .thenReturn(Optional.of(expectedPayout));

        // Act
        Payout result = payoutService.getPayout(expectedPayout.getPayoutId());

        // Assert
        assertEquals(expectedPayout, result);
        verify(payoutRepository).findByPayoutId(expectedPayout.getPayoutId());
    }

    @Test
    void testGetPayout_NotFound() {
        // Arrange
        when(payoutRepository.findByPayoutId("NONEXISTENT"))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> payoutService.getPayout("NONEXISTENT")
        );

        assertEquals("Payout not found: NONEXISTENT", exception.getMessage());
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
