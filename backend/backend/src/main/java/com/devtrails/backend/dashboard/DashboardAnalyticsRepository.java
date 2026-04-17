package com.devtrails.backend.dashboard;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DashboardAnalyticsRepository extends JpaRepository<DashboardAnalytics, Long> {

    // Worker analytics queries
    @Query("SELECT new com.devtrails.backend.dashboard.WorkerDashboardData(" +
            "w.workerId, w.name, w.zoneId, w.persona, " +
            "COALESCE(SUM(c.payoutAmount), 0.0), " +
            "COALESCE(COUNT(c.claimId), 0), " +
            "COALESCE(COUNT(CASE WHEN c.status = 'approved' THEN 1 END), 0), " +
            "COALESCE(COUNT(CASE WHEN c.status = 'rejected' THEN 1 END), 0), " +
            "COALESCE(SUM(CASE WHEN c.status = 'approved' THEN c.payoutAmount ELSE 0 END), 0.0), " +
            "w.dailyEarnings, w.activeHours, w.experienceMonths, w.daysPerWeek) " +
            "FROM Worker w " +
            "LEFT JOIN Claim c ON w.workerId = c.workerId " +
            "WHERE w.workerId = :workerId " +
            "GROUP BY w.workerId, w.name, w.zoneId, w.persona, w.dailyEarnings, w.activeHours, w.experienceMonths, w.daysPerWeek")
    Optional<WorkerDashboardData> getWorkerDashboardData(@Param("workerId") String workerId);

    // Insurer analytics queries
    @Query("SELECT new com.devtrails.backend.dashboard.InsurerDashboardData(" +
            "COUNT(c.claimId), " +
            "COALESCE(SUM(c.payoutAmount), 0.0), " +
            "COALESCE(SUM(CASE WHEN c.status = 'approved' THEN c.payoutAmount ELSE 0 END), 0.0), " +
            "COALESCE(SUM(CASE WHEN c.status = 'approved' THEN 1 ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN c.status = 'rejected' THEN 1 ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN c.status = 'flagged' THEN 1 ELSE 0 END), 0), " +
            "COUNT(DISTINCT w.workerId), " +
            "COUNT(DISTINCT w.zoneId), " +
            "COALESCE(AVG(w.dailyEarnings), 0.0)) " +
            "FROM Claim c " +
            "LEFT JOIN Worker w ON c.workerId = w.workerId " +
            "WHERE c.createdAt >= :since")
    Optional<InsurerDashboardData> getInsurerDashboardData(@Param("since") LocalDateTime since);

    // Predictive analytics queries
    @Query("SELECT c.triggerType, COUNT(c.claimId) as claimCount, " +
            "AVG(c.disruptedHours) as avgDisruptedHours, " +
            "SUM(c.payoutAmount) as totalPayout, " +
            "EXTRACT(DAY FROM c.triggeredAt) as dayOfWeek, " +
            "EXTRACT(HOUR FROM c.triggeredAt) as hourOfDay " +
            "FROM Claim c " +
            "WHERE c.createdAt >= :since " +
            "GROUP BY c.triggerType, EXTRACT(DAY FROM c.triggeredAt), EXTRACT(HOUR FROM c.triggeredAt) " +
            "ORDER BY claimCount DESC")
    List<Object[]> getPredictiveAnalytics(@Param("since") LocalDateTime since);

    // Weekly coverage analytics
    @Query("SELECT new com.devtrails.backend.dashboard.WeeklyCoverageData(" +
            "w.workerId, w.name, " +
            "DATE_TRUNC('week', c.createdAt) as weekStart, " +
            "COUNT(c.claimId) as weeklyClaims, " +
            "COALESCE(SUM(c.disruptedHours), 0.0) as totalDisruptedHours, " +
            "COALESCE(SUM(CASE WHEN c.status = 'approved' THEN c.payoutAmount ELSE 0 END), 0.0) as weeklyPayouts, " +
            "w.activeHours * w.daysPerWeek as weeklyActiveHours) " +
            "FROM Worker w " +
            "LEFT JOIN Claim c ON w.workerId = c.workerId AND c.createdAt >= :weekStart " +
            "WHERE w.workerId = :workerId AND DATE_TRUNC('week', c.createdAt) = DATE_TRUNC('week', :weekStart) " +
            "GROUP BY w.workerId, w.name, DATE_TRUNC('week', c.createdAt), w.activeHours * w.daysPerWeek " +
            "ORDER BY weekStart DESC " +
            "LIMIT 4")
    List<WeeklyCoverageData> getWorkerWeeklyCoverage(@Param("workerId") String workerId, @Param("weekStart") LocalDateTime weekStart);

    // Loss ratio calculations
    @Query("SELECT new com.devtrails.backend.dashboard.LossRatioData(" +
            "c.triggerType, " +
            "COUNT(c.claimId) as totalClaims, " +
            "COALESCE(SUM(CASE WHEN c.status = 'approved' THEN c.payoutAmount ELSE 0 END), 0.0) as totalPaid, " +
            "COALESCE(SUM(CASE WHEN c.status = 'rejected' THEN 1 ELSE 0 END), 0.0) as rejectedClaims, " +
            "COALESCE(SUM(CASE WHEN c.status = 'flagged' THEN 1 ELSE 0 END), 0.0) as flaggedClaims, " +
            "COALESCE(AVG(c.disruptedHours), 0.0) as avgDisruptedHours) " +
            "FROM Claim c " +
            "WHERE c.createdAt >= :since " +
            "GROUP BY c.triggerType " +
            "ORDER BY totalClaims DESC")
    List<LossRatioData> getLossRatioByTriggerType(@Param("since") LocalDateTime since);

    // Zone-based analytics
    @Query("SELECT new com.devtrails.backend.dashboard.ZoneAnalyticsData(" +
            "w.zoneId, " +
            "COUNT(DISTINCT w.workerId) as activeWorkers, " +
            "COUNT(c.claimId) as totalClaims, " +
            "COALESCE(SUM(CASE WHEN c.status = 'approved' THEN c.payoutAmount ELSE 0 END), 0.0) as totalPayouts, " +
            "COALESCE(AVG(w.dailyEarnings), 0.0) as avgDailyEarnings) " +
            "FROM Worker w " +
            "LEFT JOIN Claim c ON w.workerId = c.workerId AND c.createdAt >= :since " +
            "GROUP BY w.zoneId " +
            "ORDER BY totalPayouts DESC")
    List<ZoneAnalyticsData> getZoneAnalytics(@Param("since") LocalDateTime since);

    // Weather-disruption correlation
    @Query("SELECT new com.devtrails.backend.dashboard.WeatherDisruptionCorrelation(" +
            "c.triggerType, " +
            "EXTRACT(DAY FROM c.triggeredAt) as dayOfWeek, " +
            "EXTRACT(HOUR FROM c.triggeredAt) as hourOfDay, " +
            "COUNT(c.claimId) as claimCount, " +
            "COALESCE(AVG(c.disruptedHours), 0.0) as avgDisruptedHours, " +
            "COALESCE(SUM(c.payoutAmount), 0.0) as totalPayout) " +
            "FROM Claim c " +
            "WHERE c.createdAt >= :since AND c.status = 'approved' " +
            "GROUP BY c.triggerType, EXTRACT(DAY FROM c.triggeredAt), EXTRACT(HOUR FROM c.triggeredAt) " +
            "ORDER BY claimCount DESC")
    List<WeatherDisruptionCorrelation> getWeatherDisruptionCorrelation(@Param("since") LocalDateTime since);

    // Risk assessment queries
    @Query("SELECT new com.devtrails.backend.dashboard.RiskAssessmentData(" +
            "w.workerId, w.name, w.zoneId, w.persona, " +
            "w.experienceMonths, w.dailyEarnings, " +
            "COUNT(c.claimId) as totalClaims, " +
            "COALESCE(SUM(CASE WHEN c.status = 'approved' THEN c.payoutAmount ELSE 0 END), 0.0) as totalPaid, " +
            "COALESCE(SUM(CASE WHEN c.status = 'rejected' THEN 1 ELSE 0 END), 0.0) as rejectedClaims, " +
            "COALESCE(SUM(CASE WHEN c.status = 'flagged' THEN 1 ELSE 0 END), 0.0) as flaggedClaims, " +
            "COALESCE(AVG(c.disruptedHours), 0.0) as avgDisruptedHours, " +
            "CASE WHEN COUNT(c.claimId) > 0 THEN " +
            "  (COALESCE(SUM(CASE WHEN c.status = 'rejected' THEN 1 ELSE 0 END), 0.0) / COUNT(c.claimId)) * 100 " +
            "ELSE 0 END as rejectionRate) " +
            "FROM Worker w " +
            "LEFT JOIN Claim c ON w.workerId = c.workerId AND c.createdAt >= :since " +
            "GROUP BY w.workerId, w.name, w.zoneId, w.persona, w.experienceMonths, w.dailyEarnings, " +
            "COUNT(c.claimId), COALESCE(SUM(CASE WHEN c.status = 'approved' THEN c.payoutAmount ELSE 0 END), 0.0), " +
            "COALESCE(SUM(CASE WHEN c.status = 'rejected' THEN 1 ELSE 0 END), 0.0), " +
            "COALESCE(SUM(CASE WHEN c.status = 'flagged' THEN 1 ELSE 0 END), 0.0), " +
            "COALESCE(AVG(c.disruptedHours), 0.0)")
    List<RiskAssessmentData> getWorkerRiskAssessment(@Param("since") LocalDateTime since);
}
