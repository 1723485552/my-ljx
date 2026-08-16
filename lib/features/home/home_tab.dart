import 'package:flutter/material.dart';
import '../../core/plugin/base_tool_plugin.dart';
import '../../core/plugin/plugin_registry.dart';

/// Tab 1: 工作台 / 常用 / 搜索 / 最近使用
///
/// 纯净底座：未挂载任何工具时自动呈现标准空状态，绝不硬编码假数据。
class HomeTab extends StatelessWidget {
  const HomeTab({super.key});

  @override
  Widget build(BuildContext context) {
    final List<BaseToolPlugin> plugins = PluginRegistry.instance.getAllPlugins();

    return Scaffold(
      appBar: AppBar(title: const Text('AgentForge 工作台')),
      body: plugins.isEmpty
          ? _buildEmptyState(context, '当前工作台暂无挂载工具', '请通过 PluginRegistry 注册或使用 Agent 生成新工具')
          : ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: plugins.length,
              itemBuilder: (BuildContext context, int index) =>
                  _buildPluginCard(context, plugins[index]),
            ),
    );
  }

  Widget _buildPluginCard(BuildContext context, BaseToolPlugin plugin) {
    final ThemeData theme = Theme.of(context);
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: theme.colorScheme.surface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: theme.colorScheme.outline),
      ),
      child: ListTile(
        leading: Icon(plugin.manifest.icon, color: theme.colorScheme.primary),
        title: Text(plugin.manifest.name, style: const TextStyle(fontWeight: FontWeight.w600)),
        subtitle: Text(
          plugin.manifest.description,
          style: TextStyle(color: theme.colorScheme.onSurfaceVariant, fontSize: 12),
        ),
        trailing: const Icon(Icons.arrow_forward_ios_rounded, size: 14),
        onTap: () {
          Navigator.of(context)
              .push(
                MaterialPageRoute<dynamic>(builder: (BuildContext ctx) => plugin.buildView(ctx)),
              )
              .then((dynamic _) => plugin.dispose());
        },
      ),
    );
  }

  Widget _buildEmptyState(BuildContext context, String title, String subtitle) {
    final ThemeData theme = Theme.of(context);
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: <Widget>[
          Icon(Icons.widgets_outlined, size: 56, color: theme.colorScheme.outline),
          const SizedBox(height: 16),
          Text(title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
          const SizedBox(height: 6),
          Text(
            subtitle,
            style: TextStyle(fontSize: 13, color: theme.colorScheme.onSurfaceVariant),
          ),
        ],
      ),
    );
  }
}
