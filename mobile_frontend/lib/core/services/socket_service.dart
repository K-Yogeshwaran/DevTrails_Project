import 'dart:async';
import 'package:socket_io_client/socket_io_client.dart' as IO;
import '../constants/api_constants.dart';
import '../../shared/models/trigger_event_model.dart';

class SocketService {
  IO.Socket? _socket;
  final StreamController<TriggerEvent> _triggerEventController = StreamController<TriggerEvent>.broadcast();
  final StreamController<Map<String, dynamic>> _triggerResolvedController = StreamController<Map<String, dynamic>>.broadcast();
  final StreamController<String> _connectionStatusController = StreamController<String>.broadcast();

  Stream<TriggerEvent> get triggerEvents => _triggerEventController.stream;
  Stream<Map<String, dynamic>> get triggerResolved => _triggerResolvedController.stream;
  Stream<String> get connectionStatus => _connectionStatusController.stream;

  Future<void> connect(String workerId) async {
    try {
      _socket = IO.io(
        ApiConstants.socketUrl,
        IO.OptionBuilder()
            .setTransports(['polling'])
            .setReconnectionAttempts(5)
            .setReconnectionDelay(2000)
            .build(),
      );

      _socket!.connect();

      _socket!.on('connect', (_) {
        print('Connected to socket server');
        _socket!.emit('subscribe_worker', {'worker_id': workerId});
        _connectionStatusController.add('connected');
      });

      _socket!.on('disconnect', (_) {
        print('Disconnected from socket server');
        _connectionStatusController.add('disconnected');
      });

      _socket!.on('trigger_fired', (data) {
        print('Trigger fired: $data');
        if (data['worker_id'] == workerId) {
          final triggerEvent = TriggerEvent.fromJson(data);
          _triggerEventController.add(triggerEvent);
        }
      });

      _socket!.on('trigger_resolved', (data) {
        print('Trigger resolved: $data');
        if (data['worker_id'] == workerId) {
          _triggerResolvedController.add(data);
        }
      });

      _socket!.on('error', (error) {
        print('Socket error: $error');
        _connectionStatusController.add('error');
      });

    } catch (e) {
      print('Error connecting to socket: $e');
      _connectionStatusController.add('error');
    }
  }

  void disconnect() {
    if (_socket != null) {
      _socket!.disconnect();
      _socket!.dispose();
      _socket = null;
    }
  }

  bool get isConnected => _socket?.connected ?? false;

  void dispose() {
    disconnect();
    _triggerEventController.close();
    _triggerResolvedController.close();
    _connectionStatusController.close();
  }
}
