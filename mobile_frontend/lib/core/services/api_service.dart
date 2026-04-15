import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../constants/api_constants.dart';
import '../../shared/models/worker_model.dart';
import '../../shared/models/policy_model.dart';
import '../../shared/models/claim_model.dart';
import '../../shared/models/wallet_model.dart';
import '../../shared/models/trigger_event_model.dart';

class ApiService {
  final Dio _dio;
  final FlutterSecureStorage _storage;

  ApiService()
      : _dio = Dio(BaseOptions(
          baseUrl: ApiConstants.baseUrl,
          connectTimeout: ApiConstants.connectTimeout,
          receiveTimeout: ApiConstants.receiveTimeout,
          sendTimeout: ApiConstants.sendTimeout,
        )),
        _storage = const FlutterSecureStorage() {
    _setupInterceptors();
  }

  void _setupInterceptors() {
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          final token = await _storage.read(key: 'token');
          if (token != null) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          handler.next(options);
        },
        onError: (error, handler) async {
          if (error.response?.statusCode == 401) {
            await _storage.deleteAll();
            // Navigate to login screen
          }
          handler.next(error);
        },
      ),
    );
  }

  // Authentication
  Future<Map<String, dynamic>> login(String phone, String password) async {
    try {
      final response = await _dio.post(
        ApiConstants.login,
        data: {'phone': phone, 'password': password},
      );
      
      final data = response.data;
      await _storage.write(key: 'token', value: data['token']);
      await _storage.write(key: 'workerId', value: data['workerId']);
      
      return data;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<Map<String, dynamic>> register(Map<String, dynamic> workerData) async {
    try {
      final response = await _dio.post(
        ApiConstants.register,
        data: workerData,
      );
      
      final data = response.data;
      await _storage.write(key: 'token', value: data['token']);
      await _storage.write(key: 'workerId', value: data['workerId']);
      
      return data;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<Map<String, dynamic>> adminLogin(String username, String password) async {
    try {
      final response = await _dio.post(
        ApiConstants.adminLogin,
        data: {'username': username, 'password': password},
      );
      
      final data = response.data;
      await _storage.write(key: 'adminToken', value: data['token']);
      
      return data;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  // Worker endpoints
  Future<Worker> getWorker(String workerId) async {
    try {
      final response = await _dio.get('${ApiConstants.getWorker}/$workerId');
      return Worker.fromJson(response.data);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<Worker> updateWorker(String workerId, Map<String, dynamic> data) async {
    try {
      final response = await _dio.put('${ApiConstants.updateWorker}/$workerId', data: data);
      return Worker.fromJson(response.data);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  // Policy endpoints
  Future<Policy> getCurrentPolicy(String workerId) async {
    try {
      final response = await _dio.get('${ApiConstants.getCurrentPolicy}/$workerId/current');
      return Policy.fromJson(response.data);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<List<Policy>> getPolicyHistory(String workerId) async {
    try {
      final response = await _dio.get('${ApiConstants.getPolicyHistory}/$workerId/history');
      final List<dynamic> data = response.data;
      return data.map((item) => Policy.fromJson(item)).toList();
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<Policy> createPolicy(Map<String, dynamic> policyData) async {
    try {
      final response = await _dio.post(ApiConstants.createPolicy, data: policyData);
      return Policy.fromJson(response.data);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  // Claims endpoints
  Future<List<Claim>> getWorkerClaims(String workerId) async {
    try {
      final response = await _dio.get('${ApiConstants.getWorkerClaims}/$workerId');
      final List<dynamic> data = response.data;
      return data.map((item) => Claim.fromJson(item)).toList();
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<Claim> getClaimDetail(String claimId) async {
    try {
      final response = await _dio.get('${ApiConstants.getClaimDetail}/$claimId');
      return Claim.fromJson(response.data);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<Map<String, dynamic>> getClaimByEvent(String eventId) async {
    try {
      final response = await _dio.get('${ApiConstants.getClaimByEvent}/$eventId');
      return response.data;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<List<ClaimLog>> getClaimLogs(String claimId) async {
    try {
      final response = await _dio.get('${ApiConstants.getClaimLogs}/$claimId');
      final List<dynamic> data = response.data;
      return data.map((item) => ClaimLog.fromJson(item)).toList();
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<ClaimAnalytics> getClaimsAnalytics(String workerId) async {
    try {
      final response = await _dio.get('${ApiConstants.getClaimsAnalytics}?workerId=$workerId');
      return ClaimAnalytics.fromJson(response.data);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  // Trigger events endpoints
  Future<TriggerEvent> createTriggerEvent(Map<String, dynamic> eventData) async {
    try {
      final response = await _dio.post(ApiConstants.createTriggerEvent, data: eventData);
      return TriggerEvent.fromJson(response.data);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<TriggerEvent> resolveTriggerEvent(Map<String, dynamic> resolveData) async {
    try {
      final response = await _dio.post(ApiConstants.resolveTriggerEvent, data: resolveData);
      return TriggerEvent.fromJson(response.data);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<List<TriggerEvent>> getActiveEvents(String zoneId) async {
    try {
      final response = await _dio.get('${ApiConstants.getActiveEvents}/$zoneId');
      final List<dynamic> data = response.data;
      return data.map((item) => TriggerEvent.fromJson(item)).toList();
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  // Wallet endpoints
  Future<Wallet> getWallet(String workerId) async {
    try {
      final response = await _dio.get('${ApiConstants.getWallet}/$workerId');
      return Wallet.fromJson(response.data);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<List<WalletTransaction>> getWalletTransactions(String workerId) async {
    try {
      final response = await _dio.get('${ApiConstants.getWalletTransactions}/$workerId/transactions');
      final List<dynamic> data = response.data;
      return data.map((item) => WalletTransaction.fromJson(item)).toList();
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  // Admin endpoints
  Future<Map<String, dynamic>> getAdminStats() async {
    try {
      final response = await _dio.get(ApiConstants.getAdminStats);
      return response.data;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<List<Map<String, dynamic>>> getFlaggedClaims() async {
    try {
      final response = await _dio.get(ApiConstants.getFlaggedClaims);
      final List<dynamic> data = response.data;
      return data.cast<Map<String, dynamic>>();
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<Map<String, dynamic>> approveClaim(String claimId) async {
    try {
      final response = await _dio.post('${ApiConstants.approveClaim}/$claimId/approve');
      return response.data;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<Map<String, dynamic>> rejectClaim(String claimId) async {
    try {
      final response = await _dio.post('${ApiConstants.rejectClaim}/$claimId/reject');
      return response.data;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  String _handleError(DioException error) {
    switch (error.type) {
      case DioExceptionType.connectionTimeout:
        return 'Connection timeout. Please check your internet connection.';
      case DioExceptionType.sendTimeout:
        return 'Request timeout. Please try again.';
      case DioExceptionType.receiveTimeout:
        return 'Server timeout. Please try again later.';
      case DioExceptionType.badResponse:
        final message = error.response?.data?['message'] ?? 'Something went wrong';
        return message;
      case DioExceptionType.cancel:
        return 'Request was cancelled.';
      case DioExceptionType.unknown:
        return 'Network error. Please check your connection.';
      default:
        return 'An unexpected error occurred.';
    }
  }

  Future<void> logout() async {
    await _storage.deleteAll();
  }

  Future<String?> getToken() async {
    return await _storage.read(key: 'token');
  }

  Future<String?> getWorkerId() async {
    return await _storage.read(key: 'workerId');
  }
}
