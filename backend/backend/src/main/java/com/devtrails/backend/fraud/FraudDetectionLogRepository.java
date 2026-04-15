package com.devtrails.backend.fraud;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FraudDetectionLogRepository extends JpaRepository<FraudDetectionLog, Long> {

    List<FraudDetectionLog> findByClaimIdOrderByCreatedAtDesc(String claimId);

    List<FraudDetectionLog> findByWorkerIdOrderByCreatedAtDesc(String workerId);

    List<FraudDetectionLog> findByDetectionTypeOrderByCreatedAtDesc(String detectionType);

    List<FraudDetectionLog> findByRiskLevelOrderByCreatedAtDesc(String riskLevel);

    @Query("SELECT COUNT(f) FROM FraudDetectionLog f " +
            "WHERE f.workerId = :workerId " +
            "AND f.createdAt >= :since")
    long countRecentFraudLogs(
            @Param("workerId") String workerId,
            @Param("since") LocalDateTime since
    );

    @Query("SELECT f.detectionType, COUNT(f) FROM FraudDetectionLog f " +
            "WHERE f.createdAt >= :since " +
            "GROUP BY f.detectionType")
    List<Object[]> countByDetectionTypeSince(@Param("since") LocalDateTime since);

    @Query("SELECT f.riskLevel, COUNT(f) FROM FraudDetectionLog f " +
            "WHERE f.createdAt >= :since " +
            "GROUP BY f.riskLevel")
    List<Object[]> countByRiskLevelSince(@Param("since") LocalDateTime since);

    @Query("SELECT f FROM FraudDetectionLog f " +
            "WHERE f.riskScore >= :minScore " +
            "ORDER BY f.riskScore DESC")
    List<FraudDetectionLog> findHighRiskFraudLogs(@Param("minScore") Double minScore);
}
