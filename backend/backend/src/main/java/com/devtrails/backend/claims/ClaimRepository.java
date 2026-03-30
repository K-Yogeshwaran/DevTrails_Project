package com.devtrails.backend.claims;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    Optional<Claim> findByClaimId(String claimId);

    List<Claim> findByWorkerIdOrderByCreatedAtDesc(String workerId);

    List<Claim> findByStatus(String status);
    @Query("SELECT COUNT(c) > 0 FROM Claim c " +
            "WHERE c.workerId = :workerId " +
            "AND c.triggerType = :triggerType " +
            "AND c.triggeredAt >= :dayStart " +
            "AND c.triggeredAt < :dayEnd")
    boolean existsDuplicateClaim(
            @Param("workerId")    String workerId,
            @Param("triggerType") String triggerType,
            @Param("dayStart")    LocalDateTime dayStart,
            @Param("dayEnd")      LocalDateTime dayEnd
    );

    @Query("SELECT COUNT(c) FROM Claim c " +
            "WHERE c.workerId = :workerId " +
            "AND c.createdAt >= :since")
    long countRecentClaims(
            @Param("workerId") String workerId,
            @Param("since")    LocalDateTime since
    );

    @Query("SELECT SUM(c.payoutAmount) FROM Claim c WHERE c.status = 'paid'")
    java.math.BigDecimal totalPaidOut();

    @Query("SELECT c.triggerType, COUNT(c) FROM Claim c GROUP BY c.triggerType")
    List<Object[]> countByTriggerType();
}
