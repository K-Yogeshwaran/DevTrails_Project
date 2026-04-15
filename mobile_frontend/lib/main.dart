import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'core/constants/app_constants.dart';
import 'core/themes/app_theme.dart';
import 'core/services/api_service.dart';
import 'core/services/router_service.dart';
import 'shared/providers/auth_provider.dart';

void main() {
  runApp(const GigShieldApp());
}

class GigShieldApp extends StatelessWidget {
  const GigShieldApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(
          create: (_) => AuthProvider(ApiService()),
        ),
      ],
      child: MaterialApp.router(
        title: AppConstants.appName,
        debugShowCheckedModeBanner: false,
        theme: AppTheme.darkTheme,
        routerConfig: AppRouter.router,
      ),
    );
  }
}
