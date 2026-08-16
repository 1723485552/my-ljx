import 'package:flutter/material.dart';

/// 极简设计系统 (Design Tokens)
///
/// 严格遵循中性黑白灰 + 极简单强调色体系，所有间距、圆角、字阶均由此导出，
/// 禁止业务层出现无语义硬编码尺寸。
class AppTheme {
  AppTheme._();

  // ---- 设计令牌 (Design Tokens) ----
  static const double spaceXs = 4.0;
  static const double spaceSm = 8.0;
  static const double spaceMd = 12.0;
  static const double spaceLg = 16.0;
  static const double spaceXl = 24.0;

  static const double radiusSm = 8.0;
  static const double radiusMd = 12.0;
  static const double radiusLg = 16.0;

  static const double iconSizeSm = 14.0;
  static const double iconSizeMd = 24.0;
  static const double iconSizeLg = 56.0;

  // 极简单强调色（低饱和蓝）
  static const Color accentLight = Color(0xFF2563EB);
  static const Color accentDark = Color(0xFF60A5FA);

  static ThemeData get lightTheme => _buildTheme(
        brightness: Brightness.light,
        surface: const Color(0xFFFFFFFF),
        background: const Color(0xFFF5F5F5),
        onSurface: const Color(0xFF1A1A1A),
        onSurfaceVariant: const Color(0xFF6B6B6B),
        outline: const Color(0xFFE0E0E0),
        primary: accentLight,
      );

  static ThemeData get darkTheme => _buildTheme(
        brightness: Brightness.dark,
        surface: const Color(0xFF1C1C1E),
        background: const Color(0xFF000000),
        onSurface: const Color(0xFFF2F2F2),
        onSurfaceVariant: const Color(0xFF9A9A9A),
        outline: const Color(0xFF3A3A3C),
        primary: accentDark,
      );

  static ThemeData _buildTheme({
    required Brightness brightness,
    required Color surface,
    required Color background,
    required Color onSurface,
    required Color onSurfaceVariant,
    required Color outline,
    required Color primary,
  }) {
    final ColorScheme colorScheme = ColorScheme(
      brightness: brightness,
      primary: primary,
      onPrimary: Colors.white,
      secondary: primary,
      onSecondary: Colors.white,
      surface: surface,
      onSurface: onSurface,
      surfaceContainerHighest: surface,
      onSurfaceVariant: onSurfaceVariant,
      outline: outline,
      surfaceBright: background,
      error: const Color(0xFFD32F2F),
      onError: Colors.white,
    );

    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      colorScheme: colorScheme,
      scaffoldBackgroundColor: background,
      appBarTheme: AppBarTheme(
        backgroundColor: surface,
        foregroundColor: onSurface,
        elevation: 0,
        centerTitle: false,
        titleTextStyle: TextStyle(
          fontSize: 18,
          fontWeight: FontWeight.w600,
          color: onSurface,
        ),
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: surface,
        indicatorColor: primary.withValues(alpha: 0.12),
        labelTextStyle: WidgetStateProperty.all(
          const TextStyle(fontSize: 12, fontWeight: FontWeight.w500),
        ),
      ),
      cardTheme: CardThemeData(
        color: surface,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(radiusMd),
          side: BorderSide(color: outline),
        ),
      ),
      listTileTheme: ListTileThemeData(
        iconColor: primary,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: spaceLg,
          vertical: spaceSm,
        ),
      ),
    );
  }
}
