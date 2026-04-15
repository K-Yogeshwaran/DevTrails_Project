class Wallet {
  final String workerId;
  final double balance;
  final double totalCredited;
  final double totalDebited;
  final DateTime createdAt;
  final DateTime updatedAt;

  Wallet({
    required this.workerId,
    required this.balance,
    required this.totalCredited,
    required this.totalDebited,
    required this.createdAt,
    required this.updatedAt,
  });

  factory Wallet.fromJson(Map<String, dynamic> json) {
    return Wallet(
      workerId: json['workerId'] ?? '',
      balance: double.tryParse(json['balance'].toString()) ?? 0.0,
      totalCredited: double.tryParse(json['totalCredited'].toString()) ?? 0.0,
      totalDebited: double.tryParse(json['totalDebited'].toString()) ?? 0.0,
      createdAt: DateTime.tryParse(json['createdAt'] ?? '') ?? DateTime.now(),
      updatedAt: DateTime.tryParse(json['updatedAt'] ?? '') ?? DateTime.now(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'workerId': workerId,
      'balance': balance,
      'totalCredited': totalCredited,
      'totalDebited': totalDebited,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt.toIso8601String(),
    };
  }
}

class WalletTransaction {
  final String transactionId;
  final String workerId;
  final String type; // credit or debit
  final String category;
  final double amount;
  final String description;
  final double balanceAfter;
  final DateTime createdAt;

  WalletTransaction({
    required this.transactionId,
    required this.workerId,
    required this.type,
    required this.category,
    required this.amount,
    required this.description,
    required this.balanceAfter,
    required this.createdAt,
  });

  factory WalletTransaction.fromJson(Map<String, dynamic> json) {
    return WalletTransaction(
      transactionId: json['transactionId'] ?? '',
      workerId: json['workerId'] ?? '',
      type: json['type'] ?? '',
      category: json['category'] ?? '',
      amount: double.tryParse(json['amount'].toString()) ?? 0.0,
      description: json['description'] ?? '',
      balanceAfter: double.tryParse(json['balanceAfter'].toString()) ?? 0.0,
      createdAt: DateTime.tryParse(json['createdAt'] ?? '') ?? DateTime.now(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'transactionId': transactionId,
      'workerId': workerId,
      'type': type,
      'category': category,
      'amount': amount,
      'description': description,
      'balanceAfter': balanceAfter,
      'createdAt': createdAt.toIso8601String(),
    };
  }

  bool get isCredit {
    return type == 'credit';
  }

  bool get isDebit {
    return type == 'debit';
  }

  String get formattedAmount {
    final sign = isCredit ? '+' : '-';
    return '$sign?${amount.toStringAsFixed(2)}';
  }
}
