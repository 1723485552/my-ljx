import 'package:agent_forge/features/shell/main_shell.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('MainShell renders three navigation destinations', (WidgetTester tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: const MainShell(),
        theme: ThemeData.light(useMaterial3: true),
      ),
    );

    expect(find.byType(NavigationBar), findsOneWidget);
    expect(find.text('工作台'), findsOneWidget);
    expect(find.text('分类库'), findsOneWidget);
    expect(find.text('设置'), findsOneWidget);
  });

  testWidgets('tapping settings destination switches tab', (WidgetTester tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: const MainShell(),
        theme: ThemeData.light(useMaterial3: true),
      ),
    );

    await tester.tap(find.text('设置'));
    await tester.pumpAndSettle();

    expect(find.text('系统设置'), findsOneWidget);
  });
}
