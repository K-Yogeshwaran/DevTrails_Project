class Policy {
  final String policyNumber;
  final String workerId;
  final String tier;
  final double weeklyPremium;
  final double coverageCap;
  final double coverageUsed;
  final double coverageRemaining;
  final String season;
  final String status;
  final String weekStart;
  final String weekEnd;
  final DateTime createdAt;

  Policy({
    required this.policyNumber,
    required this.workerId,
    required this.tier,
    required this.weeklyPremium,
    required this.coverageCap,
    required this.coverageUsed,
    required this.coverageRemaining,
    required this.season,
    required this.status,
    required this.weekStart,
    required this.weekEnd,
    required this.createdAt,
  });

  factory Policy.fromJson(Map<String, dynamic> json) {
    return Policy(
      policyNumber: json['policyNumber'] ?? '',
      workerId: json['workerId'] ?? '',
      tier: json['tier'] ?? '',
      weeklyPremium: double.tryParse(json['weeklyPremium'].toString()) ?? 0.0,
      coverageCap: double.tryParse(json['coverageCap'].toString()) ?? 0.0,
      coverageUsed: double.tryParse(json['coverageUsed'].toString()) ?? 0.0,
      coverageRemaining: double.tryParse(json['coverageRemaining'].toString()) ?? 0.0,
      season: json['season'] ?? '',
      status: json['status'] ?? '',
      weekStart: json['weekStart'] ?? '',
      weekEnd: json['weekEnd'] ?? '',
      createdAt: DateTime.tryParse(json['createdAt'] ?? '') ?? DateTime.now(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'policyNumber': policyNumber,
      'workerId': workerId,
      'tier': tier,
      'weeklyPremium': weeklyPremium,
      'coverageCap': coverageCap,
      'coverageUsed': coverageUsed,
      'coverageRemaining': coverageRemaining,
      'season': season,
      'status': status,
      'weekStart': weekStart,
      'weekEnd': weekEnd,
      'createdAt': createdAt.toIso8601String(),
    };
  }

  double get coveragePercentage {
    if (coverageCap == 0) return 0.0;
    return (coverageUsed / coverageCap * 100).clamp(0.0, 100.0);
  }

  bool get isExhausted {
    return coverageRemaining <= 0;
  }

  bool get isLowCoverage {
    return coveragePercentage > 80;
  }
}
