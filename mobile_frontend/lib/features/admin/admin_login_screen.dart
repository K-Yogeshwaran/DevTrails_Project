import 'package:flutter/material.dart';
import '../../core/themes/app_theme.dart';

class AdminLoginScreen extends StatelessWidget {
  const AdminLoginScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      appBar: AppBar(
        backgroundColor: AppTheme.surfaceColor,
        elevation: 0,
        title: Text(
          'Admin Login',
          style: AppTheme.heading2.copyWith(color: AppTheme.textColor),
        ),
      ),
      body: const Center(
        child: Text(
          'Admin Login - Coming Soon',
          style: TextStyle(color: Colors.white),
        ),
      ),
    );
  }
}
