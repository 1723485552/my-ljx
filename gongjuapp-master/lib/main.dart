import 'package:flutter/material.dart';
import 'core/plugin/base_tool_plugin.dart';
import 'core/plugin/plugin_registry.dart';
import 'core/theme/app_theme.dart';
import 'features/shell/main_shell.dart';
import 'features/tools/chat_privacy_masker/chat_privacy_masker_plugin.dart';
import 'features/tools/fullscreen_barrage/fullscreen_barrage_plugin.dart';
import 'features/tools/image_grid_stitcher/image_grid_stitcher_plugin.dart';
import 'features/tools/instant_translator/instant_translator_plugin.dart';
import 'features/tools/media_processor/media_processor_plugin.dart';
import 'features/tools/mood_heatmap/mood_heatmap_plugin.dart';
import 'features/tools/temporary_memory/temporary_memory_plugin.dart';
import 'features/tools/text_reverser/text_reverser_plugin.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  // 工具插槽挂载：每个 BaseToolPlugin 实例在此注册即可被宿主自动感知渲染。
  PluginRegistry.instance.registerAll(<BaseToolPlugin>[
    TextReverserPlugin(),
    TemporaryMemoryPlugin(),
    ImageGridStitcherPlugin(),
    ChatPrivacyMaskerPlugin(),
    FullscreenBarragePlugin(),
    InstantTranslatorPlugin(),
    MediaProcessorPlugin(),
    MoodHeatmapPlugin(),
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
