import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/themes/app_theme.dart';
import '../../core/constants/app_constants.dart';
import '../../shared/providers/auth_provider.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  int _currentStep = 0;
  
  // Form controllers
  final _nameController = TextEditingController();
  final _phoneController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  
  // Work profile
  String? _selectedPersona;
  String? _linkedPlatform;
  String? _zoneId;
  bool _loadingLocation = false;
  bool _loadingPlatform = false;
  
  // Auto-filled values
  int _activeHours = 8;
  int _dailyEarnings = 500;
  int _experienceMonths = 6;
  int _daysPerWeek = 6;
  
  bool _obscurePassword = true;
  bool _obscureConfirm = true;

  final List<String> _steps = ['Personal Info', 'Work Profile', 'Set Password'];

  @override
  void dispose() {
    _nameController.dispose();
    _phoneController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  Future<void> _detectLocation() async {
    setState(() {
      _loadingLocation = true;
    });

    try {
      // This would normally use geolocation and call the zones API
      // For now, we'll simulate it
      await Future.delayed(const Duration(seconds: 2));
      setState(() {
        _zoneId = 'zone_chennai_t_nagar';
      });
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Location detection failed: $e'),
          backgroundColor: AppTheme.errorColor,
        ),
      );
    } finally {
      setState(() {
        _loadingLocation = false;
      });
    }
  }

  Future<void> _linkPlatform(String platform) async {
    setState(() {
      _loadingPlatform = true;
    });

    try {
      // Simulate platform verification
      await Future.delayed(const Duration(seconds: 2));
      
      // Auto-fill values based on platform
      setState(() {
        _linkedPlatform = platform;
        _activeHours = 8;
        _dailyEarnings = 600;
        _experienceMonths = 12;
        _daysPerWeek = 6;
      });
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Platform verification failed: $e'),
          backgroundColor: AppTheme.errorColor,
        ),
      );
    } finally {
      setState(() {
        _loadingPlatform = false;
      });
    }
  }

  Future<void> _handleRegister() async {
    if (_currentStep < _steps.length - 1) {
      if (_validateCurrentStep()) {
        setState(() {
          _currentStep++;
        });
      }
      return;
    }

    if (_formKey.currentState!.validate()) {
      final authProvider = Provider.of<AuthProvider>(context, listen: false);
      
      final workerData = {
        'name': _nameController.text.trim(),
        'phone': _phoneController.text.trim(),
        'email': _emailController.text.trim().isEmpty ? null : _emailController.text.trim(),
        'persona': _selectedPersona,
        'zoneId': _zoneId,
        'dailyEarnings': _dailyEarnings,
        'activeHours': _activeHours,
        'experienceMonths': _experienceMonths,
        'daysPerWeek': _daysPerWeek,
        'password': _passwordController.text,
      };

      await authProvider.register(workerData);

      if (authProvider.isAuthenticated) {
        Navigator.pushReplacementNamed(context, '/dashboard');
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

  bool _validateCurrentStep() {
    switch (_currentStep) {
      case 0:
        if (_nameController.text.trim().isEmpty) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Please enter your full name')),
          );
          return false;
        }
        if (!RegExp(r'^[6-9]\d{9}$').hasMatch(_phoneController.text)) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Please enter a valid 10-digit mobile number')),
          );
          return false;
        }
        return true;
        
      case 1:
        if (_selectedPersona == null) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Please select your work type')),
          );
          return false;
        }
        if (_linkedPlatform == null) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Please link at least one platform')),
          );
          return false;
        }
        if (_zoneId == null) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Please detect your location')),
          );
          return false;
        }
        return true;
        
      default:
        return true;
    }
  }

  void _goBack() {
    if (_currentStep > 0) {
      setState(() {
        _currentStep--;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      appBar: AppBar(
        backgroundColor: AppTheme.backgroundColor,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.pop(context),
        ),
        title: Text(
          'Create Account',
          style: AppTheme.heading3.copyWith(color: AppTheme.textColor),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            children: [
              // Step Progress
              _buildStepProgress(),
              
              const SizedBox(height: 32),
              
              // Step Content
              _buildStepContent(),
              
              const SizedBox(height: 32),
              
              // Navigation Buttons
              _buildNavigationButtons(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildStepProgress() {
    return Row(
      children: List.generate(_steps.length, (index) {
        final isCompleted = index < _currentStep;
        final isCurrent = index == _currentStep;
        
        return Expanded(
          child: Row(
            children: [
              Container(
                width: 32,
                height: 32,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: isCompleted
                      ? AppTheme.primaryColor
                      : isCurrent
                          ? AppTheme.primaryColor
                          : AppTheme.borderColor,
                ),
                child: Center(
                  child: isCompleted
                      ? const Icon(Icons.check, color: Colors.white, size: 18)
                      : Text(
                          '${index + 1}',
                          style: TextStyle(
                            color: isCurrent ? Colors.white : AppTheme.textSecondaryColor,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                ),
              ),
              
              if (index < _steps.length - 1)
                Expanded(
                  child: Container(
                    height: 2,
                    color: isCompleted ? AppTheme.primaryColor : AppTheme.borderColor,
                    margin: const EdgeInsets.symmetric(horizontal: 8),
                  ),
                ),
            ],
          ),
        );
      }),
    );
  }

  Widget _buildStepContent() {
    switch (_currentStep) {
      case 0:
        return _buildPersonalInfoStep();
      case 1:
        return _buildWorkProfileStep();
      case 2:
        return _buildPasswordStep();
      default:
        return const SizedBox.shrink();
    }
  }

  Widget _buildPersonalInfoStep() {
    return Card(
      color: AppTheme.cardColor,
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              'Personal Information',
              style: AppTheme.heading3,
              textAlign: TextAlign.center,
            ),
            
            const SizedBox(height: 24),
            
            TextFormField(
              controller: _nameController,
              decoration: const InputDecoration(
                labelText: 'Full Name',
                hintText: 'e.g. Ravi Kumar',
                prefixIcon: Icon(Icons.person),
              ),
            ),
            
            const SizedBox(height: 16),
            
            TextFormField(
              controller: _phoneController,
              keyboardType: TextInputType.phone,
              maxLength: AppConstants.maxPhoneLength,
              decoration: const InputDecoration(
                labelText: 'Mobile Number',
                hintText: '10-digit number',
                prefixIcon: Icon(Icons.phone),
              ),
            ),
            
            const SizedBox(height: 16),
            
            TextFormField(
              controller: _emailController,
              keyboardType: TextInputType.emailAddress,
              decoration: const InputDecoration(
                labelText: 'Email (optional)',
                hintText: 'your@email.com',
                prefixIcon: Icon(Icons.email),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildWorkProfileStep() {
    return Card(
      color: AppTheme.cardColor,
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              'Work Profile',
              style: AppTheme.heading3,
              textAlign: TextAlign.center,
            ),
            
            const SizedBox(height: 24),
            
            // Persona Selection
            Text(
              'Work Type',
              style: AppTheme.bodyMedium.copyWith(fontWeight: FontWeight.w600),
            ),
            
            const SizedBox(height: 12),
            
            Row(
              children: [
                Expanded(
                  child: _buildPersonaCard('food', 'Food', '??'),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: _buildPersonaCard('grocery', 'Grocery', '??'),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: _buildPersonaCard('ecommerce', 'E-Commerce', '??'),
                ),
              ],
            ),
            
            if (_selectedPersona != null) ...[
              const SizedBox(height: 24),
              
              // Platform Selection
              Text(
                'Link Your Platform',
                style: AppTheme.bodyMedium.copyWith(fontWeight: FontWeight.w600),
              ),
              
              const SizedBox(height: 12),
              
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: AppConstants.platformMap[_selectedPersona]!.map((platform) {
                  return _buildPlatformButton(platform);
                }).toList(),
              ),
              
              if (_linkedPlatform != null) ...[
                const SizedBox(height: 16),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: AppTheme.successColor.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: AppTheme.successColor),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.check_circle, color: AppTheme.successColor),
                      const SizedBox(width: 8),
                      Text(
                        'Linked to ${_linkedPlatform!} - earnings auto-filled',
                        style: AppTheme.bodySmall.copyWith(color: AppTheme.successColor),
                      ),
                    ],
                  ),
                ),
              ],
              
              const SizedBox(height: 24),
              
              // Location Detection
              Text(
                'Your Zone',
                style: AppTheme.bodyMedium.copyWith(fontWeight: FontWeight.w600),
              ),
              
              const SizedBox(height: 12),
              
              ElevatedButton.icon(
                onPressed: _loadingLocation ? null : _detectLocation,
                icon: _loadingLocation
                    ? const SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.location_on),
                label: Text(_loadingLocation ? 'Detecting...' : 'Detect My Location'),
              ),
              
              const SizedBox(height: 12),
              
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: AppTheme.surfaceColor,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: AppTheme.borderColor),
                ),
                child: Text(
                  _zoneId?.replaceAll('_', ' ').toUpperCase() ?? 'Zone will appear after detection',
                  style: AppTheme.bodyMedium.copyWith(
                    color: _zoneId != null ? AppTheme.textColor : AppTheme.textSecondaryColor,
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildPersonaCard(String persona, String label, String emoji) {
    final isSelected = _selectedPersona == persona;
    
    return GestureDetector(
      onTap: () {
        setState(() {
          _selectedPersona = persona;
          _linkedPlatform = null; // Reset platform when persona changes
        });
      },
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: isSelected ? AppTheme.primaryColor.withOpacity(0.2) : AppTheme.surfaceColor,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(
            color: isSelected ? AppTheme.primaryColor : AppTheme.borderColor,
          ),
        ),
        child: Column(
          children: [
            Text(
              emoji,
              style: const TextStyle(fontSize: 24),
            ),
            const SizedBox(height: 4),
            Text(
              label,
              style: AppTheme.bodySmall.copyWith(
                fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
                color: isSelected ? AppTheme.primaryColor : AppTheme.textColor,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPlatformButton(String platform) {
    final isLinked = _linkedPlatform == platform;
    
    return ElevatedButton(
      onPressed: _loadingPlatform ? null : () => _linkPlatform(platform),
      style: ElevatedButton.styleFrom(
        backgroundColor: isLinked ? AppTheme.successColor : AppTheme.surfaceColor,
        foregroundColor: isLinked ? Colors.white : AppTheme.textColor,
        side: BorderSide(color: isLinked ? AppTheme.successColor : AppTheme.borderColor),
      ),
      child: _loadingPlatform && _linkedPlatform == platform
          ? const SizedBox(
              width: 16,
              height: 16,
              child: CircularProgressIndicator(strokeWidth: 2),
            )
          : Text(
              platform.toUpperCase(),
              style: AppTheme.bodySmall,
            ),
    );
  }

  Widget _buildPasswordStep() {
    return Card(
      color: AppTheme.cardColor,
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                'Secure Your Account',
                style: AppTheme.heading3,
                textAlign: TextAlign.center,
              ),
              
              const SizedBox(height: 24),
              
              TextFormField(
                controller: _passwordController,
                obscureText: _obscurePassword,
                decoration: InputDecoration(
                  labelText: 'Password',
                  hintText: 'Minimum 6 characters',
                  prefixIcon: const Icon(Icons.lock),
                  suffixIcon: IconButton(
                    icon: Icon(_obscurePassword ? Icons.visibility_off : Icons.visibility),
                    onPressed: () {
                      setState(() {
                        _obscurePassword = !_obscurePassword;
                      });
                    },
                  ),
                ),
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return 'Password is required';
                  }
                  if (value.length < AppConstants.minPasswordLength) {
                    return 'Password must be at least ${AppConstants.minPasswordLength} characters';
                  }
                  return null;
                },
              ),
              
              const SizedBox(height: 16),
              
              TextFormField(
                controller: _confirmPasswordController,
                obscureText: _obscureConfirm,
                decoration: InputDecoration(
                  labelText: 'Confirm Password',
                  hintText: 'Re-enter your password',
                  prefixIcon: const Icon(Icons.lock),
                  suffixIcon: IconButton(
                    icon: Icon(_obscureConfirm ? Icons.visibility_off : Icons.visibility),
                    onPressed: () {
                      setState(() {
                        _obscureConfirm = !_obscureConfirm;
                      });
                    },
                  ),
                ),
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return 'Please confirm your password';
                  }
                  if (value != _passwordController.text) {
                    return 'Passwords do not match';
                  }
                  return null;
                },
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildNavigationButtons() {
    return Consumer<AuthProvider>(
      builder: (context, authProvider, child) {
        return Row(
          children: [
            if (_currentStep > 0)
              Expanded(
                child: OutlinedButton(
                  onPressed: _goBack,
                  child: const Text('Back'),
                ),
              ),
            
            if (_currentStep > 0) const SizedBox(width: 16),
            
            Expanded(
              child: ElevatedButton(
                onPressed: authProvider.isLoading ? null : _handleRegister,
                child: authProvider.isLoading
                    ? const SizedBox(
                        height: 20,
                        width: 20,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                        ),
                      )
                    : Text(_currentStep < _steps.length - 1 ? 'Continue' : 'Create Account ??'),
              ),
            ),
          ],
        );
      },
    );
  }
}
