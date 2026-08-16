import 'package:flutter/material.dart';
import 'core/plugin/base_tool_plugin.dart';
import 'core/plugin/plugin_registry.dart';
import 'core/theme/app_theme.dart';
import 'features/shell/main_shell.dart';
import 'features/tools/temporary_memory/temporary_memory_plugin.dart';
import 'features/tools/text_reverser/text_reverser_plugin.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  // 工具插槽挂载：每个 BaseToolPlugin 实例在此注册即可被宿主自动感知渲染。
  PluginRegistry.instance.registerAll(<BaseToolPlugin>[
    TextReverserPlugin(),
    TemporaryMemoryPlugin(),
  ]);

  runApp(const AgentForgeApp());
}

class AgentForgeApp extends StatelessWidget {
  const AgentForgeApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'AgentForge',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: ThemeMode.system,
      home: const MainShell(),
    );
  }
}
