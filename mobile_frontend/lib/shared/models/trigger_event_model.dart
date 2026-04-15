class TriggerEvent {
  final String eventId;
  final String triggerType;
  final String zoneId;
  final String zoneName;
  final double? triggerValue;
  final String status;
  final DateTime? startedAt;
  final DateTime? endedAt;
  final double? disruptedHours;
  final List<String> affectedWorkerIds;
  final int elapsedSeconds;

  TriggerEvent({
    required this.eventId,
    required this.triggerType,
    required this.zoneId,
    required this.zoneName,
    this.triggerValue,
    required this.status,
    this.startedAt,
    this.endedAt,
    this.disruptedHours,
    required this.affectedWorkerIds,
    required this.elapsedSeconds,
  });

  factory TriggerEvent.fromJson(Map<String, dynamic> json) {
    return TriggerEvent(
      eventId: json['eventId'] ?? '',
      triggerType: json['triggerType'] ?? '',
      zoneId: json['zoneId'] ?? '',
      zoneName: json['zoneName'] ?? '',
      triggerValue: double.tryParse(json['triggerValue'].toString()),
      status: json['status'] ?? '',
      startedAt: DateTime.tryParse(json['startedAt'] ?? ''),
      endedAt: DateTime.tryParse(json['endedAt']),
      disruptedHours: double.tryParse(json['disruptedHours'].toString()),
      affectedWorkerIds: List<String>.from(json['affectedWorkerIds'] ?? []),
      elapsedSeconds: json['elapsedSeconds'] ?? 0,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'eventId': eventId,
      'triggerType': triggerType,
      'zoneId': zoneId,
      'zoneName': zoneName,
      'triggerValue': triggerValue,
      'status': status,
      'startedAt': startedAt?.toIso8601String(),
      'endedAt': endedAt?.toIso8601String(),
      'disruptedHours': disruptedHours,
      'affectedWorkerIds': affectedWorkerIds,
      'elapsedSeconds': elapsedSeconds,
    };
  }

  bool get isActive {
    return status == 'active';
  }

  bool get isResolved {
    return status == 'resolved';
  }

  Duration get elapsedDuration {
    return Duration(seconds: elapsedSeconds);
  }

  String get formattedElapsedTime {
    final duration = elapsedDuration;
    final hours = duration.inHours;
    final minutes = duration.inMinutes.remainder(60);
    final seconds = duration.inSeconds.remainder(60);
    
    if (hours > 0) {
      return '${hours}h ${minutes}m ${seconds}s';
    } else {
      return '${minutes}m ${seconds}s';
    }
  }
}
