import 'package:flutter/material.dart';
import '../../core/services/api_service.dart';
import '../../shared/models/worker_model.dart';

class AuthProvider with ChangeNotifier {
  final ApiService _apiService;
  
  Worker? _worker;
  bool _isLoading = false;
  String? _error;
  bool _isAuthenticated = false;

  AuthProvider(this._apiService);

  // Getters
  Worker? get worker => _worker;
  bool get isLoading => _isLoading;
  String? get error => _error;
  bool get isAuthenticated => _isAuthenticated;

  Future<void> login(String phone, String password) async {
    _setLoading(true);
    _clearError();

    try {
      final response = await _apiService.login(phone, password);
      
      // Get worker details
      _worker = await _apiService.getWorker(response['workerId']);
      _isAuthenticated = true;
      
      notifyListeners();
    } catch (e) {
      _setError(e.toString());
    } finally {
      _setLoading(false);
    }
  }

  Future<void> register(Map<String, dynamic> workerData) async {
    _setLoading(true);
    _clearError();

    try {
      final response = await _apiService.register(workerData);
      
      // Get worker details
      _worker = await _apiService.getWorker(response['workerId']);
      _isAuthenticated = true;
      
      notifyListeners();
    } catch (e) {
      _setError(e.toString());
    } finally {
      _setLoading(false);
    }
  }

  Future<void> logout() async {
    try {
      await _apiService.logout();
      _worker = null;
      _isAuthenticated = false;
      notifyListeners();
    } catch (e) {
      _setError(e.toString());
    }
  }

  Future<void> checkAuthStatus() async {
    _setLoading(true);
    _clearError();

    try {
      final token = await _apiService.getToken();
      final workerId = await _apiService.getWorkerId();

      if (token != null && workerId != null) {
        _worker = await _apiService.getWorker(workerId);
        _isAuthenticated = true;
      } else {
        _isAuthenticated = false;
      }
      
      notifyListeners();
    } catch (e) {
      _isAuthenticated = false;
      _setError(e.toString());
    } finally {
      _setLoading(false);
    }
  }

  Future<void> updateWorkerProfile(Map<String, dynamic> data) async {
    if (_worker == null) return;

    _setLoading(true);
    _clearError();

    try {
      _worker = await _apiService.updateWorker(_worker!.workerId, data);
      notifyListeners();
    } catch (e) {
      _setError(e.toString());
    } finally {
      _setLoading(false);
    }
  }

  void _setLoading(bool loading) {
    _isLoading = loading;
    notifyListeners();
  }

  void _setError(String error) {
    _error = error;
    notifyListeners();
  }

  void _clearError() {
    _error = null;
    notifyListeners();
  }
}
