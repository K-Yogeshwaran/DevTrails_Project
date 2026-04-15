import 'package:flutter/material.dart';

class AppTheme {
  // Colors
  static const Color primaryColor = Color(0xFF6366f1);
  static const Color primaryDark = Color(0xFF4f46e5);
  static const Color secondaryColor = Color(0xFF22c55e);
  static const Color successColor = Color(0xFF22c55e);
  static const Color warningColor = Color(0xFFf59e0b);
  static const Color errorColor = Color(0xFFef4444);
  static const Color backgroundColor = Color(0xFF0f0f23);
  static const Color surfaceColor = Color(0xFF16161f);
  static const Color cardColor = Color(0xFF1a1a2e);
  static const Color textColor = Color(0xFFe4e4e7);
  static const Color textSecondaryColor = Color(0xFFa1a1aa);
  static const Color borderColor = Color(0xFF2a2a3a);
  
  // Gradients
  static const LinearGradient primaryGradient = LinearGradient(
    colors: [primaryColor, primaryDark],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );
  
  static const LinearGradient successGradient = LinearGradient(
    colors: [Color(0xFF22c55e), Color(0xFF16a34a)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );
  
  // Text Styles
  static const TextStyle heading1 = TextStyle(
    fontSize: 32,
    fontWeight: FontWeight.bold,
    color: textColor,
    height: 1.2,
  );
  
  static const TextStyle heading2 = TextStyle(
    fontSize: 24,
    fontWeight: FontWeight.bold,
    color: textColor,
    height: 1.3,
  );
  
  static const TextStyle heading3 = TextStyle(
    fontSize: 20,
    fontWeight: FontWeight.w600,
    color: textColor,
    height: 1.3,
  );
  
  static const TextStyle bodyLarge = TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.normal,
    color: textColor,
    height: 1.5,
  );
  
  static const TextStyle bodyMedium = TextStyle(
    fontSize: 14,
    fontWeight: FontWeight.normal,
    color: textColor,
    height: 1.4,
  );
  
  static const TextStyle bodySmall = TextStyle(
    fontSize: 12,
    fontWeight: FontWeight.normal,
    color: textSecondaryColor,
    height: 1.4,
  );
  
  // Dark Theme
  static ThemeData darkTheme = ThemeData(
    useMaterial3: true,
    brightness: Brightness.dark,
    colorScheme: const ColorScheme.dark(
      primary: primaryColor,
      secondary: secondaryColor,
      error: errorColor,
      surface: surfaceColor,
      background: backgroundColor,
      onPrimary: Colors.white,
      onSecondary: Colors.white,
      onError: Colors.white,
      onSurface: textColor,
      onBackground: textColor,
    ),
    
    scaffoldBackgroundColor: backgroundColor,
    appBarTheme: const AppBarTheme(
      backgroundColor: surfaceColor,
      foregroundColor: textColor,
      elevation: 0,
      centerTitle: true,
      titleTextStyle: heading2,
    ),
    
    cardTheme: CardThemeData(
      color: cardColor,
      elevation: 4,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
      ),
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
    ),
    
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: primaryColor,
        foregroundColor: Colors.white,
        elevation: 4,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8),
        ),
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
        textStyle: bodyMedium.copyWith(fontWeight: FontWeight.w600),
      ),
    ),
    
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: primaryColor,
        side: const BorderSide(color: primaryColor, width: 2),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8),
        ),
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
        textStyle: bodyMedium.copyWith(fontWeight: FontWeight.w600),
      ),
    ),
    
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(
        foregroundColor: primaryColor,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        textStyle: bodyMedium.copyWith(fontWeight: FontWeight.w600),
      ),
    ),
    
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: cardColor,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: const BorderSide(color: borderColor),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: const BorderSide(color: borderColor),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: const BorderSide(color: primaryColor, width: 2),
      ),
      errorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: const BorderSide(color: errorColor),
      ),
      labelStyle: bodyMedium.copyWith(color: textSecondaryColor),
      hintStyle: bodyMedium.copyWith(color: textSecondaryColor),
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
    ),
    
    bottomNavigationBarTheme: const BottomNavigationBarThemeData(
      backgroundColor: surfaceColor,
      selectedItemColor: primaryColor,
      unselectedItemColor: textSecondaryColor,
      type: BottomNavigationBarType.fixed,
      elevation: 8,
    ),
    
    progressIndicatorTheme: const ProgressIndicatorThemeData(
      color: primaryColor,
      linearTrackColor: borderColor,
      circularTrackColor: borderColor,
    ),
    
    chipTheme: ChipThemeData(
      backgroundColor: cardColor,
      selectedColor: primaryColor.withOpacity(0.2),
      disabledColor: borderColor,
      labelStyle: bodySmall,
      side: const BorderSide(color: borderColor),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
      ),
    ),
  );
}
