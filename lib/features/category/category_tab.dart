import 'package:flutter/material.dart';
import '../../core/plugin/plugin_registry.dart';

/// Tab 2: 分类与工具索引
///
/// 依赖方向 Features -> Core：通过 PluginRegistry 动态聚合各分类工具，
/// 不持有任何业务硬编码。
class CategoryTab extends StatelessWidget {
  const CategoryTab({super.key});

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);
    final List<String> categories = PluginRegistry.instance
        .getAllPlugins()
        .map((p) => p.manifest.category)
        .toSet()
        .toList();

    return Scaffold(
      appBar: AppBar(title: const Text('工具分类索引')),
      body: categories.isEmpty
          ? Center(
              child: Text(
                '分类索引已就绪 (等待插件注册)',
                style: TextStyle(color: theme.colorScheme.onSurfaceVariant),
              ),
            )
          : ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: categories.length,
              itemBuilder: (BuildContext context, int index) {
                final String category = categories[index];
                final int count = PluginRegistry.instance
                    .getPluginsByCategory(category)
                    .length;
                return Container(
                  margin: const EdgeInsets.only(bottom: 12),
                  decoration: BoxDecoration(
                    color: theme.colorScheme.surface,
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: theme.colorScheme.outline),
                  ),
                  child: ListTile(
                    leading: Icon(Icons.folder_outlined, color: theme.colorScheme.primary),
                    title: Text(category),
                    trailing: Chip(
                      label: Text('$count'),
                      backgroundColor:
                          theme.colorScheme.primary.withValues(alpha: 0.12),
                    ),
                  ),
                );
              },
            ),
    );
  }
}
