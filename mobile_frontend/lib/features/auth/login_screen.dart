import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import '../../core/themes/app_theme.dart';
import '../../core/constants/app_constants.dart';
import '../../shared/providers/auth_provider.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _phoneController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _obscurePassword = true;

  @override
  void dispose() {
    _phoneController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _handleLogin() async {
    if (_formKey.currentState!.validate()) {
      final authProvider = Provider.of<AuthProvider>(context, listen: false);
      await authProvider.login(
        _phoneController.text.trim(),
        _passwordController.text,
      );

      if (authProvider.isAuthenticated) {
        // Navigate to dashboard
        context.go('/dashboard');
      } else if (authProvider.error != null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(authProvider.error!),
            backgroundColor: AppTheme.errorColor,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const SizedBox(height: 60),
              
              // Brand
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Text(
                    '??',
                    style: TextStyle(fontSize: 48),
                  ),
                  const SizedBox(width: 8),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Gig',
                        style: AppTheme.heading1.copyWith(
                          color: AppTheme.primaryColor,
                          fontSize: 36,
                        ),
                      ),
                      Text(
                        'Shield',
                        style: AppTheme.heading1.copyWith(
                          color: AppTheme.textColor,
                          fontSize: 36,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
              
              const SizedBox(height: 16),
              
              Text(
                AppConstants.appTagline,
                textAlign: TextAlign.center,
                style: AppTheme.bodyMedium.copyWith(
                  color: AppTheme.textSecondaryColor,
                ),
              ),
              
              const SizedBox(height: 48),
              
              // Login Form
              Card(
                color: AppTheme.cardColor,
                child: Padding(
                  padding: const EdgeInsets.all(24.0),
                  child: Form(
                    key: _formKey,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Text(
                          'Welcome Back',
                          style: AppTheme.heading3,
                          textAlign: TextAlign.center,
                        ),
                        
                        const SizedBox(height: 32),
                        
                        // Phone Field
                        TextFormField(
                          controller: _phoneController,
                          keyboardType: TextInputType.phone,
                          maxLength: AppConstants.maxPhoneLength,
                          decoration: const InputDecoration(
                            labelText: 'Mobile Number',
                            hintText: 'Enter your 10-digit number',
                            prefixIcon: Icon(Icons.phone),
                          ),
                          validator: (value) {
                            if (value == null || value.isEmpty) {
                              return 'Please enter your mobile number';
                            }
                            if (!RegExp(r'^[6-9]\d{9}$').hasMatch(value)) {
                              return 'Enter a valid 10-digit Indian mobile number';
                            }
                            return null;
                          },
                        ),
                        
                        const SizedBox(height: 16),
                        
                        // Password Field
                        TextFormField(
                          controller: _passwordController,
                          obscureText: _obscurePassword,
                          decoration: InputDecoration(
                            labelText: 'Password',
                            hintText: 'Enter your password',
                            prefixIcon: const Icon(Icons.lock),
                            suffixIcon: IconButton(
                              icon: Icon(
                                _obscurePassword ? Icons.visibility_off : Icons.visibility,
                              ),
                              onPressed: () {
                                setState(() {
                                  _obscurePassword = !_obscurePassword;
                                });
                              },
                            ),
                          ),
                          validator: (value) {
                            if (value == null || value.isEmpty) {
                              return 'Please enter your password';
                            }
                            return null;
                          },
                        ),
                        
                        const SizedBox(height: 24),
                        
                        // Login Button
                        Consumer<AuthProvider>(
                          builder: (context, authProvider, child) {
                            return ElevatedButton(
                              onPressed: authProvider.isLoading ? null : _handleLogin,
                              child: authProvider.isLoading
                                  ? const SizedBox(
                                      height: 20,
                                      width: 20,
                                      child: CircularProgressIndicator(
                                        strokeWidth: 2,
                                        valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                                      ),
                                    )
                                  : const Text('Sign In'),
                            );
                          },
                        ),
                        
                        const SizedBox(height: 16),
                        
                        // Register Link
                        TextButton(
                          onPressed: () {
                            context.go('/register');
                          },
                          child: const Text("Don't have an account? Create one"),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              
              const SizedBox(height: 24),
              
              // Admin Login Link
              TextButton(
                onPressed: () {
                  context.go('/admin/login');
                },
                child: Text(
                  'Admin Login',
                  style: AppTheme.bodySmall.copyWith(
                    color: AppTheme.textSecondaryColor,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
