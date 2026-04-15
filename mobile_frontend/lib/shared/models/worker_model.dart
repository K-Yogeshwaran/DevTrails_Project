class Worker {
  final String workerId;
  final String name;
  final String phone;
  final String? email;
  final String zoneId;
  final String persona;
  final int dailyEarnings;
  final int activeHours;
  final int experienceMonths;
  final int daysPerWeek;
  final bool isActive;
  final DateTime createdAt;

  Worker({
    required this.workerId,
    required this.name,
    required this.phone,
    this.email,
    required this.zoneId,
    required this.persona,
    required this.dailyEarnings,
    required this.activeHours,
    required this.experienceMonths,
    required this.daysPerWeek,
    required this.isActive,
    required this.createdAt,
  });

  factory Worker.fromJson(Map<String, dynamic> json) {
    return Worker(
      workerId: json['workerId'] ?? '',
      name: json['name'] ?? '',
      phone: json['phone'] ?? '',
      email: json['email'],
      zoneId: json['zoneId'] ?? '',
      persona: json['persona'] ?? '',
      dailyEarnings: int.tryParse(json['dailyEarnings'].toString()) ?? 0,
      activeHours: int.tryParse(json['activeHours'].toString()) ?? 0,
      experienceMonths: int.tryParse(json['experienceMonths'].toString()) ?? 0,
      daysPerWeek: int.tryParse(json['daysPerWeek'].toString()) ?? 0,
      isActive: json['isActive'] ?? true,
      createdAt: DateTime.tryParse(json['createdAt'] ?? '') ?? DateTime.now(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'workerId': workerId,
      'name': name,
      'phone': phone,
      'email': email,
      'zoneId': zoneId,
      'persona': persona,
      'dailyEarnings': dailyEarnings,
      'activeHours': activeHours,
      'experienceMonths': experienceMonths,
      'daysPerWeek': daysPerWeek,
      'isActive': isActive,
      'createdAt': createdAt.toIso8601String(),
    };
  }

  Worker copyWith({
    String? workerId,
    String? name,
    String? phone,
    String? email,
    String? zoneId,
    String? persona,
    int? dailyEarnings,
    int? activeHours,
    int? experienceMonths,
    int? daysPerWeek,
    bool? isActive,
    DateTime? createdAt,
  }) {
    return Worker(
      workerId: workerId ?? this.workerId,
      name: name ?? this.name,
      phone: phone ?? this.phone,
      email: email ?? this.email,
      zoneId: zoneId ?? this.zoneId,
      persona: persona ?? this.persona,
      dailyEarnings: dailyEarnings ?? this.dailyEarnings,
      activeHours: activeHours ?? this.activeHours,
      experienceMonths: experienceMonths ?? this.experienceMonths,
      daysPerWeek: daysPerWeek ?? this.daysPerWeek,
      isActive: isActive ?? this.isActive,
      createdAt: createdAt ?? this.createdAt,
    );
  }
}
