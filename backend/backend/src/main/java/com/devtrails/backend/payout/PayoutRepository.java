package com.devtrails.backend.payout;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, Long> {

    Optional<Payout> findByPayoutId(String payoutId);

    List<Payout> findByWorkerIdOrderByCreatedAtDesc(String workerId);

    List<Payout> findByClaimIdOrderByCreatedAtDesc(String claimId);

    List<Payout> findByStatusOrderByCreatedAtDesc(String status);

    List<Payout> findByPaymentMethodOrderByCreatedAtDesc(String paymentMethod);

    @Query("SELECT p FROM Payout p WHERE p.status IN :statuses ORDER BY p.createdAt DESC")
    List<Payout> findByStatusInOrderByCreatedAtDesc(@Param("statuses") List<String> statuses);

    @Query("SELECT p FROM Payout p WHERE p.workerId = :workerId AND p.status IN :statuses ORDER BY p.createdAt DESC")
    List<Payout> findByWorkerIdAndStatusInOrderByCreatedAtDesc(
            @Param("workerId") String workerId, 
            @Param("statuses") List<String> statuses
    );

    @Query("SELECT COUNT(p) FROM Payout p WHERE p.workerId = :workerId AND p.status = 'COMPLETED' AND p.completedAt >= :since")
    long countCompletedPayoutsForWorkerSince(
            @Param("workerId") String workerId, 
            @Param("since") LocalDateTime since
    );

    @Query("SELECT SUM(p.amount) FROM Payout p WHERE p.workerId = :workerId AND p.status = 'COMPLETED' AND p.completedAt >= :since")
    Double sumCompletedPayoutsForWorkerSince(
            @Param("workerId") String workerId, 
            @Param("since") LocalDateTime since
    );

    @Query("SELECT COUNT(p) FROM Payout p WHERE p.status = 'FAILED' AND p.createdAt >= :since")
    long countFailedPayoutsSince(@Param("since") LocalDateTime since);

    @Query("SELECT p.paymentMethod, COUNT(p), SUM(p.amount) FROM Payout p WHERE p.status = 'COMPLETED' AND p.completedAt >= :since GROUP BY p.paymentMethod")
    List<Object[]> getPayoutStatsByPaymentMethodSince(@Param("since") LocalDateTime since);

    @Query("SELECT p FROM Payout p WHERE p.status = 'PROCESSING' AND p.processedAt < :threshold ORDER BY p.createdAt ASC")
    List<Payout> findStuckProcessingPayouts(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT p FROM Payout p WHERE p.status = 'PENDING' AND p.createdAt >= :since ORDER BY p.createdAt ASC")
    List<Payout> findPendingPayoutsSince(@Param("since") LocalDateTime since);

    // Find payouts that need status update (webhook not received within timeout)
    @Query("SELECT p FROM Payout p WHERE p.status = 'PROCESSING' AND p.processedAt < :timeout AND p.webhookReceived = false ORDER BY p.createdAt ASC")
    List<Payout> findPayoutsNeedingStatusCheck(@Param("timeout") LocalDateTime timeout);

    // Analytics queries
    @Query("SELECT DATE(p.createdAt) as date, COUNT(p) as count, SUM(p.amount) as total FROM Payout p WHERE p.status = 'COMPLETED' AND p.createdAt >= :since GROUP BY DATE(p.createdAt) ORDER BY date DESC")
    List<Object[]> getDailyPayoutStatsSince(@Param("since") LocalDateTime since);

    @Query("SELECT p.paymentMethod, p.status, COUNT(p) as count FROM Payout p WHERE p.createdAt >= :since GROUP BY p.paymentMethod, p.status ORDER BY count DESC")
    List<Object[]> getPayoutBreakdownSince(@Param("since") LocalDateTime since);
}
