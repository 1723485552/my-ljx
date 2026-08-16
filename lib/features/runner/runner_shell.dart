import 'package:flutter/material.dart';
import '../../core/plugin/base_tool_plugin.dart';

/// 通用工具承载容器 (Runner Shell)
///
/// 提供标准化的工具运行外壳：顶部返回栏 + 独立内容区。各工具可通过
/// [BaseToolPlugin.buildView] 复用此容器，确保退出时统一触发 [BaseToolPlugin.dispose]。
class RunnerShell extends StatelessWidget {
  const RunnerShell({
    super.key,
    required this.plugin,
    required this.body,
  });

  final BaseToolPlugin plugin;
  final Widget body;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: Text(plugin.manifest.name),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: () {
            plugin.dispose();
            Navigator.of(context).pop();
          },
        ),
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: body,
        ),
      ),
      backgroundColor: theme.colorScheme.surfaceBright,
    );
  }
}
