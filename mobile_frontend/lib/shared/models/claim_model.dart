class Claim {
  final String claimId;
  final String workerId;
  final String? policyNumber;
  final String triggerType;
  final double? triggerValue;
  final String zoneId;
  final double? disruptedHours;
  final double payoutAmount;
  final double fraudScore;
  final String status;
  final DateTime triggeredAt;
  final DateTime? processedAt;

  Claim({
    required this.claimId,
    required this.workerId,
    this.policyNumber,
    required this.triggerType,
    this.triggerValue,
    required this.zoneId,
    this.disruptedHours,
    required this.payoutAmount,
    required this.fraudScore,
    required this.status,
    required this.triggeredAt,
    this.processedAt,
  });

  factory Claim.fromJson(Map<String, dynamic> json) {
    return Claim(
      claimId: json['claimId'] ?? '',
      workerId: json['workerId'] ?? '',
      policyNumber: json['policyNumber'],
      triggerType: json['triggerType'] ?? '',
      triggerValue: double.tryParse(json['triggerValue'].toString()),
      zoneId: json['zoneId'] ?? '',
      disruptedHours: double.tryParse(json['disruptedHours'].toString()),
      payoutAmount: double.tryParse(json['payoutAmount'].toString()) ?? 0.0,
      fraudScore: double.tryParse(json['fraudScore'].toString()) ?? 0.0,
      status: json['status'] ?? '',
      triggeredAt: DateTime.tryParse(json['triggeredAt'] ?? '') ?? DateTime.now(),
      processedAt: DateTime.tryParse(json['processedAt']),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'claimId': claimId,
      'workerId': workerId,
      'policyNumber': policyNumber,
      'triggerType': triggerType,
      'triggerValue': triggerValue,
      'zoneId': zoneId,
      'disruptedHours': disruptedHours,
      'payoutAmount': payoutAmount,
      'fraudScore': fraudScore,
      'status': status,
      'triggeredAt': triggeredAt.toIso8601String(),
      'processedAt': processedAt?.toIso8601String(),
    };
  }

  double get fraudPercentage {
    return (fraudScore * 100).clamp(0.0, 100.0);
  }

  String get fraudScoreColor {
    if (fraudScore > 0.7) return '#ef4444';
    if (fraudScore > 0.4) return '#f59e0b';
    return '#22c55e';
  }

  bool get isApproved {
    return status == 'approved';
  }

  bool get isRejected {
    return status == 'rejected';
  }

  bool get isFlagged {
    return status == 'flagged';
  }

  bool get isPending {
    return status == 'pending' || status == 'processing';
  }
}

class ClaimAnalytics {
  final int totalClaims;
  final int approvedClaims;
  final int rejectedClaims;
  final int flaggedClaims;
  final double totalPaidOut;
  final double approvalRate;
  final double fraudRate;

  ClaimAnalytics({
    required this.totalClaims,
    required this.approvedClaims,
    required this.rejectedClaims,
    required this.flaggedClaims,
    required this.totalPaidOut,
    required this.approvalRate,
    required this.fraudRate,
  });

  factory ClaimAnalytics.fromJson(Map<String, dynamic> json) {
    return ClaimAnalytics(
      totalClaims: json['totalClaims'] ?? 0,
      approvedClaims: json['approvedClaims'] ?? 0,
      rejectedClaims: json['rejectedClaims'] ?? 0,
      flaggedClaims: json['flaggedClaims'] ?? 0,
      totalPaidOut: double.tryParse(json['totalPaidOut'].toString()) ?? 0.0,
      approvalRate: double.tryParse(json['approvalRate'].toString()) ?? 0.0,
      fraudRate: double.tryParse(json['fraudRate'].toString()) ?? 0.0,
    );
  }
}

class ClaimLog {
  final String claimId;
  final String stage;
  final String status;
  final String detail;
  final DateTime timestamp;

  ClaimLog({
    required this.claimId,
    required this.stage,
    required this.status,
    required this.detail,
    required this.timestamp,
  });

  factory ClaimLog.fromJson(Map<String, dynamic> json) {
    return ClaimLog(
      claimId: json['claimId'] ?? '',
      stage: json['stage'] ?? '',
      status: json['status'] ?? '',
      detail: json['detail'] ?? '',
      timestamp: DateTime.tryParse(json['timestamp'] ?? '') ?? DateTime.now(),
    );
  }
}
