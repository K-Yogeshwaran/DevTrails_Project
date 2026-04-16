package com.devtrails.backend.dashboard;

import com.devtrails.backend.config.ApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // Worker Dashboard Endpoints
    
    // GET /api/dashboard/worker
    @GetMapping("/worker")
    public ResponseEntity<WorkerDashboardResponse> getWorkerDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            String workerId = userDetails.getUsername();
            WorkerDashboardData data = dashboardService.getWorkerDashboard(workerId);
            
            return ResponseEntity.ok(WorkerDashboardResponse.from(data));
            
        } catch (Exception e) {
            log.error("Error getting worker dashboard: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load worker dashboard");
        }
    }

    // GET /api/dashboard/worker/weekly-coverage
    @GetMapping("/worker/weekly-coverage")
    public ResponseEntity<List<WeeklyCoverageResponse>> getWorkerWeeklyCoverage(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            String workerId = userDetails.getUsername();
            List<WeeklyCoverageData> data = dashboardService.getWorkerWeeklyCoverage(workerId);
            
            List<WeeklyCoverageResponse> response = data.stream()
                    .map(WeeklyCoverageResponse::from)
                    .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting worker weekly coverage: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load weekly coverage data");
        }
    }

    // GET /api/dashboard/worker/earnings-protection
    @GetMapping("/worker/earnings-protection")
    public ResponseEntity<EarningsProtectionResponse> getWorkerEarningsProtection(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            String workerId = userDetails.getUsername();
            BigDecimal protectionAmount = dashboardService.calculateEarningsProtection(workerId);
            
            return ResponseEntity.ok(new EarningsProtectionResponse(
                    workerId, protectionAmount, LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("Error calculating earnings protection: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to calculate earnings protection");
        }
    }

    // Insurer Dashboard Endpoints
    
    // GET /api/dashboard/insurer
    @GetMapping("/insurer")
    public ResponseEntity<InsurerDashboardResponse> getInsurerDashboard() {
        
        try {
            InsurerDashboardData data = dashboardService.getInsurerDashboard();
            
            return ResponseEntity.ok(InsurerDashboardResponse.from(data));
            
        } catch (Exception e) {
            log.error("Error getting insurer dashboard: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load insurer dashboard");
        }
    }

    // GET /api/dashboard/insurer/loss-ratios
    @GetMapping("/insurer/loss-ratios")
    public ResponseEntity<List<LossRatioResponse>> getLossRatios() {
        
        try {
            List<LossRatioData> data = dashboardService.getLossRatios();
            
            List<LossRatioResponse> response = data.stream()
                    .map(LossRatioResponse::from)
                    .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting loss ratios: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load loss ratios");
        }
    }

    // GET /api/dashboard/insurer/predictive-analytics
    @GetMapping("/insurer/predictive-analytics")
    public ResponseEntity<PredictiveAnalyticsResponse> getPredictiveAnalytics() {
        
        try {
            List<PredictiveAnalytics> data = dashboardService.getPredictiveAnalytics();
            
            return ResponseEntity.ok(PredictiveAnalyticsResponse.from(data));
            
        } catch (Exception e) {
            log.error("Error getting predictive analytics: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load predictive analytics");
        }
    }

    // GET /api/dashboard/insurer/weekly-prediction
    @GetMapping("/insurer/weekly-prediction")
    public ResponseEntity<WeeklyPredictionResponse> getWeeklyPrediction() {
        
        try {
            WeeklyPrediction prediction = dashboardService.generateWeeklyPrediction();
            
            return ResponseEntity.ok(WeeklyPredictionResponse.from(prediction));
            
        } catch (Exception e) {
            log.error("Error generating weekly prediction: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate weekly prediction");
        }
    }

    // GET /api/dashboard/insurer/zone-analytics
    @GetMapping("/insurer/zone-analytics")
    public ResponseEntity<List<ZoneAnalyticsResponse>> getZoneAnalytics() {
        
        try {
            List<ZoneAnalyticsData> data = dashboardService.getZoneAnalytics();
            
            List<ZoneAnalyticsResponse> response = data.stream()
                    .map(ZoneAnalyticsResponse::from)
                    .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting zone analytics: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load zone analytics");
        }
    }

    // GET /api/dashboard/insurer/weather-correlation
    @GetMapping("/insurer/weather-correlation")
    public ResponseEntity<List<WeatherCorrelationResponse>> getWeatherCorrelation() {
        
        try {
            List<WeatherDisruptionCorrelation> data = dashboardService.getWeatherDisruptionCorrelation();
            
            List<WeatherCorrelationResponse> response = data.stream()
                    .map(WeatherCorrelationResponse::from)
                    .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting weather correlation: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load weather correlation data");
        }
    }

    // GET /api/dashboard/insurer/risk-assessment
    @GetMapping("/insurer/risk-assessment")
    public ResponseEntity<List<RiskAssessmentResponse>> getRiskAssessment() {
        
        try {
            List<RiskAssessmentData> data = dashboardService.getWorkerRiskAssessment();
            
            List<RiskAssessmentResponse> response = data.stream()
                    .map(RiskAssessmentResponse::from)
                    .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting risk assessment: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load risk assessment data");
        }
    }

    // Advanced Analytics Endpoints
    
    // GET /api/dashboard/advanced-analytics
    @GetMapping("/advanced-analytics")
    public ResponseEntity<Map<String, Object>> getAdvancedAnalytics() {
        
        try {
            Map<String, Object> analytics = dashboardService.getAdvancedAnalytics();
            
            return ResponseEntity.ok(analytics);
            
        } catch (Exception e) {
            log.error("Error getting advanced analytics: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load advanced analytics");
        }
    }

    // POST /api/dashboard/refresh
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshDashboard(
            @RequestBody RefreshRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            switch (request.getDashboardType().toUpperCase()) {
                case "WORKER":
                    dashboardService.updateWorkerDashboard(userDetails.getUsername());
                    break;
                case "INSURER":
                    dashboardService.updateInsurerDashboard();
                    break;
                default:
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid dashboard type");
            }
            
            return ResponseEntity.ok(Map.of("message", "Dashboard refreshed successfully"));
            
        } catch (Exception e) {
            log.error("Error refreshing dashboard: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to refresh dashboard");
        }
    }

    // GET /api/dashboard/health
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "dashboard-service",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    // Response DTOs
    public static class WorkerDashboardResponse {
        private String workerId;
        private String workerName;
        private String zoneId;
        private String persona;
        private BigDecimal totalEarningsProtected;
        private Long totalClaims;
        private Long approvedClaims;
        private Long rejectedClaims;
        private BigDecimal totalPayoutsReceived;
        private BigDecimal claimSuccessRate;
        private BigDecimal coveragePercentage;
        private BigDecimal dailyEarnings;
        private Integer activeHours;
        private Integer experienceMonths;
        private Integer daysPerWeek;
        private LocalDateTime lastUpdated;

        public static WorkerDashboardResponse from(WorkerDashboardData data) {
            WorkerDashboardResponse response = new WorkerDashboardResponse();
            response.workerId = data.workerId();
            response.workerName = data.workerName();
            response.zoneId = data.zoneId();
            response.persona = data.persona();
            response.totalEarningsProtected = data.totalEarningsProtected();
            response.totalClaims = data.totalClaims();
            response.approvedClaims = data.approvedClaims();
            response.rejectedClaims = data.rejectedClaims();
            response.totalPayoutsReceived = data.totalPayoutsReceived();
            response.claimSuccessRate = data.getClaimSuccessRate();
            response.coveragePercentage = data.getCoveragePercentage();
            response.dailyEarnings = data.dailyEarnings();
            response.activeHours = data.activeHours();
            response.experienceMonths = data.experienceMonths();
            response.daysPerWeek = data.daysPerWeek();
            response.lastUpdated = data.lastUpdated();
            return response;
        }

        // Getters and setters
        public String getWorkerId() { return workerId; }
        public void setWorkerId(String workerId) { this.workerId = workerId; }
        public String getWorkerName() { return workerName; }
        public void setWorkerName(String workerName) { this.workerName = workerName; }
        public String getZoneId() { return zoneId; }
        public void setZoneId(String zoneId) { this.zoneId = zoneId; }
        public String getPersona() { return persona; }
        public void setPersona(String persona) { this.persona = persona; }
        public BigDecimal getTotalEarningsProtected() { return totalEarningsProtected; }
        public void setTotalEarningsProtected(BigDecimal totalEarningsProtected) { this.totalEarningsProtected = totalEarningsProtected; }
        public Long getTotalClaims() { return totalClaims; }
        public void setTotalClaims(Long totalClaims) { this.totalClaims = totalClaims; }
        public Long getApprovedClaims() { return approvedClaims; }
        public void setApprovedClaims(Long approvedClaims) { this.approvedClaims = approvedClaims; }
        public Long getRejectedClaims() { return rejectedClaims; }
        public void setRejectedClaims(Long rejectedClaims) { this.rejectedClaims = rejectedClaims; }
        public BigDecimal getTotalPayoutsReceived() { return totalPayoutsReceived; }
        public void setTotalPayoutsReceived(BigDecimal totalPayoutsReceived) { this.totalPayoutsReceived = totalPayoutsReceived; }
        public BigDecimal getClaimSuccessRate() { return claimSuccessRate; }
        public void setClaimSuccessRate(BigDecimal claimSuccessRate) { this.claimSuccessRate = claimSuccessRate; }
        public BigDecimal getCoveragePercentage() { return coveragePercentage; }
        public void setCoveragePercentage(BigDecimal coveragePercentage) { this.coveragePercentage = coveragePercentage; }
        public BigDecimal getDailyEarnings() { return dailyEarnings; }
        public void setDailyEarnings(BigDecimal dailyEarnings) { this.dailyEarnings = dailyEarnings; }
        public Integer getActiveHours() { return activeHours; }
        public void setActiveHours(Integer activeHours) { this.activeHours = activeHours; }
        public Integer getExperienceMonths() { return experienceMonths; }
        public void setExperienceMonths(Integer experienceMonths) { this.experienceMonths = experienceMonths; }
        public Integer getDaysPerWeek() { return daysPerWeek; }
        public void setDaysPerWeek(Integer daysPerWeek) { this.daysPerWeek = daysPerWeek; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    public static class WeeklyCoverageResponse {
        private String workerId;
        private String workerName;
        private String weekStart;
        private Integer weeklyClaims;
        private BigDecimal totalDisruptedHours;
        private BigDecimal weeklyPayouts;
        private Integer weeklyActiveHours;
        private BigDecimal coverageEfficiency;
        private BigDecimal weeklyProtectionValue;
        private LocalDateTime lastUpdated;

        public static WeeklyCoverageResponse from(WeeklyCoverageData data) {
            WeeklyCoverageResponse response = new WeeklyCoverageResponse();
            response.workerId = data.workerId();
            response.workerName = data.workerName();
            response.weekStart = data.weekStart().toString();
            response.weeklyClaims = data.weeklyClaims();
            response.totalDisruptedHours = data.totalDisruptedHours();
            response.weeklyPayouts = data.weeklyPayouts();
            response.weeklyActiveHours = data.weeklyActiveHours();
            response.coverageEfficiency = data.getCoverageEfficiency();
            response.lastUpdated = data.lastUpdated();
            return response;
        }

        // Getters and setters
        public String getWorkerId() { return workerId; }
        public void setWorkerId(String workerId) { this.workerId = workerId; }
        public String getWorkerName() { return workerName; }
        public void setWorkerName(String workerName) { this.workerName = workerName; }
        public String getWeekStart() { return weekStart; }
        public void setWeekStart(String weekStart) { this.weekStart = weekStart; }
        public Integer getWeeklyClaims() { return weeklyClaims; }
        public void setWeeklyClaims(Integer weeklyClaims) { this.weeklyClaims = weeklyClaims; }
        public BigDecimal getTotalDisruptedHours() { return totalDisruptedHours; }
        public void setTotalDisruptedHours(BigDecimal totalDisruptedHours) { this.totalDisruptedHours = totalDisruptedHours; }
        public BigDecimal getWeeklyPayouts() { return weeklyPayouts; }
        public void setWeeklyPayouts(BigDecimal weeklyPayouts) { this.weeklyPayouts = weeklyPayouts; }
        public Integer getWeeklyActiveHours() { return weeklyActiveHours; }
        public void setWeeklyActiveHours(Integer weeklyActiveHours) { this.weeklyActiveHours = weeklyActiveHours; }
        public BigDecimal getCoverageEfficiency() { return coverageEfficiency; }
        public void setCoverageEfficiency(BigDecimal coverageEfficiency) { this.coverageEfficiency = coverageEfficiency; }
        public BigDecimal getWeeklyProtectionValue() { return weeklyProtectionValue; }
        public void setWeeklyProtectionValue(BigDecimal weeklyProtectionValue) { this.weeklyProtectionValue = weeklyProtectionValue; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    public static class EarningsProtectionResponse {
        private String workerId;
        private BigDecimal protectedAmount;
        private LocalDateTime calculatedAt;

        public EarningsProtectionResponse(String workerId, BigDecimal protectedAmount, LocalDateTime calculatedAt) {
            this.workerId = workerId;
            this.protectedAmount = protectedAmount;
            this.calculatedAt = calculatedAt;
        }

        // Getters and setters
        public String getWorkerId() { return workerId; }
        public void setWorkerId(String workerId) { this.workerId = workerId; }
        public BigDecimal getProtectedAmount() { return protectedAmount; }
        public void setProtectedAmount(BigDecimal protectedAmount) { this.protectedAmount = protectedAmount; }
        public LocalDateTime getCalculatedAt() { return calculatedAt; }
        public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
    }

    public static class InsurerDashboardResponse {
        private Long totalClaims;
        private BigDecimal totalClaimValue;
        private BigDecimal totalPayoutsProcessed;
        private Long approvedClaims;
        private Long rejectedClaims;
        private Long flaggedClaims;
        private Integer activeWorkers;
        private Integer activeZones;
        private BigDecimal avgDailyEarnings;
        private BigDecimal lossRatio;
        private BigDecimal approvalRate;
        private BigDecimal rejectionRate;
        private BigDecimal flaggedRate;
        private LocalDateTime lastUpdated;

        public static InsurerDashboardResponse from(InsurerDashboardData data) {
            InsurerDashboardResponse response = new InsurerDashboardResponse();
            response.totalClaims = data.totalClaims();
            response.totalClaimValue = data.totalClaimValue();
            response.totalPayoutsProcessed = data.totalPayoutsProcessed();
            response.approvedClaims = data.approvedClaims();
            response.rejectedClaims = data.rejectedClaims();
            response.flaggedClaims = data.flaggedClaims();
            response.activeWorkers = data.activeWorkers();
            response.activeZones = data.activeZones();
            response.avgDailyEarnings = data.avgDailyEarnings();
            response.lossRatio = data.getLossRatio();
            response.approvalRate = data.getApprovalRate();
            response.rejectionRate = data.getRejectionRate();
            response.flaggedRate = data.getFlaggedRate();
            response.lastUpdated = data.lastUpdated();
            return response;
        }

        // Getters and setters
        public Long getTotalClaims() { return totalClaims; }
        public void setTotalClaims(Long totalClaims) { this.totalClaims = totalClaims; }
        public BigDecimal getTotalClaimValue() { return totalClaimValue; }
        public void setTotalClaimValue(BigDecimal totalClaimValue) { this.totalClaimValue = totalClaimValue; }
        public BigDecimal getTotalPayoutsProcessed() { return totalPayoutsProcessed; }
        public void setTotalPayoutsProcessed(BigDecimal totalPayoutsProcessed) { this.totalPayoutsProcessed = totalPayoutsProcessed; }
        public Long getApprovedClaims() { return approvedClaims; }
        public void setApprovedClaims(Long approvedClaims) { this.approvedClaims = approvedClaims; }
        public Long getRejectedClaims() { return rejectedClaims; }
        public void setRejectedClaims(Long rejectedClaims) { this.rejectedClaims = rejectedClaims; }
        public Long getFlaggedClaims() { return flaggedClaims; }
        public void setFlaggedClaims(Long flaggedClaims) { this.flaggedClaims = flaggedClaims; }
        public Integer getActiveWorkers() { return activeWorkers; }
        public void setActiveWorkers(Integer activeWorkers) { this.activeWorkers = activeWorkers; }
        public Integer getActiveZones() { return activeZones; }
        public void setActiveZones(Integer activeZones) { this.activeZones = activeZones; }
        public BigDecimal getAvgDailyEarnings() { return avgDailyEarnings; }
        public void setAvgDailyEarnings(BigDecimal avgDailyEarnings) { this.avgDailyEarnings = avgDailyEarnings; }
        public BigDecimal getLossRatio() { return lossRatio; }
        public void setLossRatio(BigDecimal lossRatio) { this.lossRatio = lossRatio; }
        public BigDecimal getApprovalRate() { return approvalRate; }
        public void setApprovalRate(BigDecimal approvalRate) { this.approvalRate = approvalRate; }
        public BigDecimal getRejectionRate() { return rejectionRate; }
        public void setRejectionRate(BigDecimal rejectionRate) { this.rejectionRate = rejectionRate; }
        public BigDecimal getFlaggedRate() { return flaggedRate; }
        public void setFlaggedRate(BigDecimal flaggedRate) { this.flaggedRate = flaggedRate; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    public static class LossRatioResponse {
        private String triggerType;
        private Long totalClaims;
        private BigDecimal totalPaid;
        private Long rejectedClaims;
        private Long flaggedClaims;
        private Double avgDisruptedHours;
        private BigDecimal lossRatio;
        private BigDecimal averageClaimValue;
        private LocalDateTime lastUpdated;

        public static LossRatioResponse from(LossRatioData data) {
            LossRatioResponse response = new LossRatioResponse();
            response.triggerType = data.triggerType();
            response.totalClaims = data.totalClaims();
            response.totalPaid = data.totalPaid();
            response.rejectedClaims = data.rejectedClaims();
            response.flaggedClaims = data.flaggedClaims();
            response.avgDisruptedHours = data.avgDisruptedHours();
            response.lossRatio = data.getLossRatio();
            response.averageClaimValue = data.getAverageClaimValue();
            response.lastUpdated = data.lastUpdated();
            return response;
        }

        // Getters and setters
        public String getTriggerType() { return triggerType; }
        public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
        public Long getTotalClaims() { return totalClaims; }
        public void setTotalClaims(Long totalClaims) { this.totalClaims = totalClaims; }
        public BigDecimal getTotalPaid() { return totalPaid; }
        public void setTotalPaid(BigDecimal totalPaid) { this.totalPaid = totalPaid; }
        public Long getRejectedClaims() { return rejectedClaims; }
        public void setRejectedClaims(Long rejectedClaims) { this.rejectedClaims = rejectedClaims; }
        public Long getFlaggedClaims() { return flaggedClaims; }
        public void setFlaggedClaims(Long flaggedClaims) { this.flaggedClaims = flaggedClaims; }
        public Double getAvgDisruptedHours() { return avgDisruptedHours; }
        public void setAvgDisruptedHours(Double avgDisruptedHours) { this.avgDisruptedHours = avgDisruptedHours; }
        public BigDecimal getLossRatio() { return lossRatio; }
        public void setLossRatio(BigDecimal lossRatio) { this.lossRatio = lossRatio; }
        public BigDecimal getAverageClaimValue() { return averageClaimValue; }
        public void setAverageClaimValue(BigDecimal averageClaimValue) { this.averageClaimValue = averageClaimValue; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    public static class PredictiveAnalyticsResponse {
        private List<PredictiveAnalyticsItem> analytics;
        private LocalDateTime lastUpdated;

        public static PredictiveAnalyticsResponse from(List<PredictiveAnalytics> data) {
            PredictiveAnalyticsResponse response = new PredictiveAnalyticsResponse();
            response.analytics = data.stream()
                    .map(PredictiveAnalyticsItem::from)
                    .toList();
            response.lastUpdated = data.isEmpty() ? null : data.get(0).lastUpdated();
            return response;
        }

        // Getters and setters
        public List<PredictiveAnalyticsItem> getAnalytics() { return analytics; }
        public void setAnalytics(List<PredictiveAnalyticsItem> analytics) { this.analytics = analytics; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    public static class PredictiveAnalyticsItem {
        private String triggerType;
        private Long claimCount;
        private Double avgDisruptedHours;
        private BigDecimal totalPayout;
        private Integer dayOfWeek;
        private Integer hourOfDay;
        private Double riskScore;
        private String prediction;
        private String riskLevel;

        public static PredictiveAnalyticsItem from(PredictiveAnalytics data) {
            PredictiveAnalyticsItem item = new PredictiveAnalyticsItem();
            item.triggerType = data.triggerType();
            item.claimCount = data.claimCount();
            item.avgDisruptedHours = data.avgDisruptedHours();
            item.totalPayout = data.totalPayout();
            item.dayOfWeek = data.dayOfWeek();
            item.hourOfDay = data.hourOfDay();
            item.riskScore = data.riskScore();
            item.prediction = data.prediction();
            item.riskLevel = data.prediction(); // Using prediction as risk level
            return item;
        }

        // Getters and setters
        public String getTriggerType() { return triggerType; }
        public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
        public Long getClaimCount() { return claimCount; }
        public void setClaimCount(Long claimCount) { this.claimCount = claimCount; }
        public Double getAvgDisruptedHours() { return avgDisruptedHours; }
        public void setAvgDisruptedHours(Double avgDisruptedHours) { this.avgDisruptedHours = avgDisruptedHours; }
        public BigDecimal getTotalPayout() { return totalPayout; }
        public void setTotalPayout(BigDecimal totalPayout) { this.totalPayout = totalPayout; }
        public Integer getDayOfWeek() { return dayOfWeek; }
        public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }
        public Integer getHourOfDay() { return hourOfDay; }
        public void setHourOfDay(Integer hourOfDay) { this.hourOfDay = hourOfDay; }
        public Double getRiskScore() { return riskScore; }
        public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }
        public String getPrediction() { return prediction; }
        public void setPrediction(String prediction) { this.prediction = prediction; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    }

    public static class WeeklyPredictionResponse {
        private String weekStartDate;
        private String weekEndDate;
        private Integer totalPredictedClaims;
        private BigDecimal predictedPayoutAmount;
        private String primaryRiskFactor;
        private String secondaryRiskFactor;
        private String confidenceLevel;
        private LocalDateTime generatedAt;

        public static WeeklyPredictionResponse from(WeeklyPrediction data) {
            WeeklyPredictionResponse response = new WeeklyPredictionResponse();
            response.weekStartDate = data.weekStartDate();
            response.weekEndDate = data.weekEndDate();
            response.totalPredictedClaims = data.totalPredictedClaims();
            response.predictedPayoutAmount = data.predictedPayoutAmount();
            response.primaryRiskFactor = data.primaryRiskFactor();
            response.secondaryRiskFactor = data.secondaryRiskFactor();
            response.confidenceLevel = data.getConfidenceLevel();
            response.generatedAt = data.generatedAt();
            return response;
        }

        // Getters and setters
        public String getWeekStartDate() { return weekStartDate; }
        public void setWeekStartDate(String weekStartDate) { this.weekStartDate = weekStartDate; }
        public String getWeekEndDate() { return weekEndDate; }
        public void setWeekEndDate(String weekEndDate) { this.weekEndDate = weekEndDate; }
        public Integer getTotalPredictedClaims() { return totalPredictedClaims; }
        public void setTotalPredictedClaims(Integer totalPredictedClaims) { this.totalPredictedClaims = totalPredictedClaims; }
        public BigDecimal getPredictedPayoutAmount() { return predictedPayoutAmount; }
        public void setPredictedPayoutAmount(BigDecimal predictedPayoutAmount) { this.predictedPayoutAmount = predictedPayoutAmount; }
        public String getPrimaryRiskFactor() { return primaryRiskFactor; }
        public void setPrimaryRiskFactor(String primaryRiskFactor) { this.primaryRiskFactor = primaryRiskFactor; }
        public String getSecondaryRiskFactor() { return secondaryRiskFactor; }
        public void setSecondaryRiskFactor(String secondaryRiskFactor) { this.secondaryRiskFactor = secondaryRiskFactor; }
        public String getConfidenceLevel() { return confidenceLevel; }
        public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    }

    public static class ZoneAnalyticsResponse {
        private String zoneId;
        private Integer activeWorkers;
        private Long totalClaims;
        private BigDecimal totalPayouts;
        private BigDecimal avgDailyEarnings;
        private BigDecimal averageClaimsPerWorker;
        private BigDecimal zoneRiskScore;
        private LocalDateTime lastUpdated;

        public static ZoneAnalyticsResponse from(ZoneAnalyticsData data) {
            ZoneAnalyticsResponse response = new ZoneAnalyticsResponse();
            response.zoneId = data.zoneId();
            response.activeWorkers = data.activeWorkers();
            response.totalClaims = data.totalClaims();
            response.totalPayouts = data.totalPayouts();
            response.avgDailyEarnings = data.avgDailyEarnings();
            response.averageClaimsPerWorker = data.getAverageClaimsPerWorker();
            response.zoneRiskScore = data.getZoneRiskScore();
            response.lastUpdated = data.lastUpdated();
            return response;
        }

        // Getters and setters
        public String getZoneId() { return zoneId; }
        public void setZoneId(String zoneId) { this.zoneId = zoneId; }
        public Integer getActiveWorkers() { return activeWorkers; }
        public void setActiveWorkers(Integer activeWorkers) { this.activeWorkers = activeWorkers; }
        public Long getTotalClaims() { return totalClaims; }
        public void setTotalClaims(Long totalClaims) { this.totalClaims = totalClaims; }
        public BigDecimal getTotalPayouts() { return totalPayouts; }
        public void setTotalPayouts(BigDecimal totalPayouts) { this.totalPayouts = totalPayouts; }
        public BigDecimal getAvgDailyEarnings() { return avgDailyEarnings; }
        public void setAvgDailyEarnings(BigDecimal avgDailyEarnings) { this.avgDailyEarnings = avgDailyEarnings; }
        public BigDecimal getAverageClaimsPerWorker() { return averageClaimsPerWorker; }
        public void setAverageClaimsPerWorker(BigDecimal averageClaimsPerWorker) { this.averageClaimsPerWorker = averageClaimsPerWorker; }
        public BigDecimal getZoneRiskScore() { return zoneRiskScore; }
        public void setZoneRiskScore(BigDecimal zoneRiskScore) { this.zoneRiskScore = zoneRiskScore; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    public static class WeatherCorrelationResponse {
        private String triggerType;
        private Integer dayOfWeek;
        private Integer hourOfDay;
        private Long claimCount;
        private Double avgDisruptedHours;
        private BigDecimal totalPayout;
        private String timePattern;
        private String dayPattern;
        private LocalDateTime lastUpdated;

        public static WeatherCorrelationResponse from(WeatherDisruptionCorrelation data) {
            WeatherCorrelationResponse response = new WeatherCorrelationResponse();
            response.triggerType = data.triggerType();
            response.dayOfWeek = data.dayOfWeek();
            response.hourOfDay = data.hourOfDay();
            response.claimCount = data.claimCount();
            response.avgDisruptedHours = data.avgDisruptedHours();
            response.totalPayout = data.totalPayout();
            response.timePattern = data.getTimePattern();
            response.dayPattern = data.getDayPattern();
            response.lastUpdated = data.lastUpdated();
            return response;
        }

        // Getters and setters
        public String getTriggerType() { return triggerType; }
        public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
        public Integer getDayOfWeek() { return dayOfWeek; }
        public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }
        public Integer getHourOfDay() { return hourOfDay; }
        public void setHourOfDay(Integer hourOfDay) { this.hourOfDay = hourOfDay; }
        public Long getClaimCount() { return claimCount; }
        public void setClaimCount(Long claimCount) { this.claimCount = claimCount; }
        public Double getAvgDisruptedHours() { return avgDisruptedHours; }
        public void setAvgDisruptedHours(Double avgDisruptedHours) { this.avgDisruptedHours = avgDisruptedHours; }
        public BigDecimal getTotalPayout() { return totalPayout; }
        public void setTotalPayout(BigDecimal totalPayout) { this.totalPayout = totalPayout; }
        public String getTimePattern() { return timePattern; }
        public void setTimePattern(String timePattern) { this.timePattern = timePattern; }
        public String getDayPattern() { return dayPattern; }
        public void setDayPattern(String dayPattern) { this.dayPattern = dayPattern; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    public static class RiskAssessmentResponse {
        private String workerId;
        private String workerName;
        private String zoneId;
        private String persona;
        private Integer experienceMonths;
        private BigDecimal dailyEarnings;
        private Long totalClaims;
        private BigDecimal totalPaid;
        private Long rejectedClaims;
        private Long flaggedClaims;
        private Double avgDisruptedHours;
        private Double rejectionRate;
        private BigDecimal overallRiskScore;
        private String riskCategory;
        private String riskFactors;
        private LocalDateTime lastUpdated;

        public static RiskAssessmentResponse from(RiskAssessmentData data) {
            RiskAssessmentResponse response = new RiskAssessmentResponse();
            response.workerId = data.workerId();
            response.workerName = data.workerName();
            response.zoneId = data.zoneId();
            response.persona = data.persona();
            response.experienceMonths = data.experienceMonths();
            response.dailyEarnings = data.dailyEarnings();
            response.totalClaims = data.totalClaims();
            response.totalPaid = data.totalPaid();
            response.rejectedClaims = data.rejectedClaims();
            response.flaggedClaims = data.flaggedClaims();
            response.avgDisruptedHours = data.avgDisruptedHours();
            response.rejectionRate = data.rejectionRate();
            response.overallRiskScore = data.calculateOverallRiskScore();
            response.riskCategory = data.getRiskCategory();
            response.riskFactors = data.getRiskFactors();
            response.lastUpdated = data.lastUpdated();
            return response;
        }

        // Getters and setters
        public String getWorkerId() { return workerId; }
        public void setWorkerId(String workerId) { this.workerId = workerId; }
        public String getWorkerName() { return workerName; }
        public void setWorkerName(String workerName) { this.workerName = workerName; }
        public String getZoneId() { return zoneId; }
        public void setZoneId(String zoneId) { this.zoneId = zoneId; }
        public String getPersona() { return persona; }
        public void setPersona(String persona) { this.persona = persona; }
        public Integer getExperienceMonths() { return experienceMonths; }
        public void setExperienceMonths(Integer experienceMonths) { this.experienceMonths = experienceMonths; }
        public BigDecimal getDailyEarnings() { return dailyEarnings; }
        public void setDailyEarnings(BigDecimal dailyEarnings) { this.dailyEarnings = dailyEarnings; }
        public Long getTotalClaims() { return totalClaims; }
        public void setTotalClaims(Long totalClaims) { this.totalClaims = totalClaims; }
        public BigDecimal getTotalPaid() { return totalPaid; }
        public void setTotalPaid(BigDecimal totalPaid) { this.totalPaid = totalPaid; }
        public Long getRejectedClaims() { return rejectedClaims; }
        public void setRejectedClaims(Long rejectedClaims) { this.rejectedClaims = rejectedClaims; }
        public Long getFlaggedClaims() { return flaggedClaims; }
        public void setFlaggedClaims(Long flaggedClaims) { this.flaggedClaims = flaggedClaims; }
        public Double getAvgDisruptedHours() { return avgDisruptedHours; }
        public void setAvgDisruptedHours(Double avgDisruptedHours) { this.avgDisruptedHours = avgDisruptedHours; }
        public Double getRejectionRate() { return rejectionRate; }
        public void setRejectionRate(Double rejectionRate) { this.rejectionRate = rejectionRate; }
        public BigDecimal getOverallRiskScore() { return overallRiskScore; }
        public void setOverallRiskScore(BigDecimal overallRiskScore) { this.overallRiskScore = overallRiskScore; }
        public String getRiskCategory() { return riskCategory; }
        public void setRiskCategory(String riskCategory) { this.riskCategory = riskCategory; }
        public String getRiskFactors() { return riskFactors; }
        public void setRiskFactors(String riskFactors) { this.riskFactors = riskFactors; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    public static class RefreshRequest {
        @NotBlank(message = "Dashboard type is required")
        private String dashboardType;

        public String getDashboardType() { return dashboardType; }
        public void setDashboardType(String dashboardType) { this.dashboardType = dashboardType; }
    }
}
