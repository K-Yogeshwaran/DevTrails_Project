import 'package:flutter/material.dart';
import '../../core/themes/app_theme.dart';

class WalletScreen extends StatelessWidget {
  const WalletScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      appBar: AppBar(
        backgroundColor: AppTheme.surfaceColor,
        elevation: 0,
        title: Text(
          'Wallet',
          style: AppTheme.heading2.copyWith(color: AppTheme.textColor),
        ),
      ),
      body: const Center(
        child: Text(
          'Wallet - Coming Soon',
          style: TextStyle(color: Colors.white),
        ),
      ),
    );
  }
}
