class ApiConstants {
  // Base URL
  static const String baseUrl = 'http://localhost:8080/api';
  
  // Auth endpoints
  static const String login = '$baseUrl/workers/login';
  static const String register = '$baseUrl/workers/register';
  static const String adminLogin = '$baseUrl/admin/login';
  
  // Worker endpoints
  static const String getWorker = '$baseUrl/workers';
  static const String updateWorker = '$baseUrl/workers';
  
  // Policy endpoints
  static const String createPolicy = '$baseUrl/policies';
  static const String getCurrentPolicy = '$baseUrl/policies';
  static const String getPolicyHistory = '$baseUrl/policies';
  static const String checkCoverage = '$baseUrl/policies';
  
  // Claims endpoints
  static const String getWorkerClaims = '$baseUrl/claims/worker';
  static const String getClaimDetail = '$baseUrl/claims/detail';
  static const String getClaimByEvent = '$baseUrl/claims/event';
  static const String getClaimLogs = '$baseUrl/claims/logs';
  static const String getClaimsAnalytics = '$baseUrl/claims/analytics';
  
  // Trigger events endpoints
  static const String createTriggerEvent = '$baseUrl/trigger-events';
  static const String resolveTriggerEvent = '$baseUrl/trigger-events/resolve';
  static const String getActiveEvents = '$baseUrl/trigger-events/active';
  
  // Wallet endpoints
  static const String getWallet = '$baseUrl/wallet';
  static const String getWalletTransactions = '$baseUrl/wallet';
  
  // Admin endpoints
  static const String getAdminStats = '$baseUrl/admin/stats';
  static const String getFlaggedClaims = '$baseUrl/admin/claims/flagged';
  static const String approveClaim = '$baseUrl/admin/claims';
  static const String rejectClaim = '$baseUrl/admin/claims';
  static const String getAllClaims = '$baseUrl/admin/claims/all';
  static const String getWorkerStats = '$baseUrl/admin/workers';
  static const String getPolicyStats = '$baseUrl/admin/policies';
  
  // External services
  static const String triggerEngineUrl = 'http://localhost:5001';
  static const String mockPlatformUrl = 'http://localhost:5002';
  static const String mlServiceUrl = 'http://localhost:5003';
  
  // Socket.io
  static const String socketUrl = 'http://localhost:5001';
  
  // Request timeout
  static const Duration connectTimeout = Duration(seconds: 30);
  static const Duration receiveTimeout = Duration(seconds: 30);
  static const Duration sendTimeout = Duration(seconds: 30);
}
