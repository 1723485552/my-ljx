import 'package:flutter/material.dart';
import '../../core/plugin/plugin_registry.dart';

/// Tab 3: 缓存、主题与系统配置
class SettingsTab extends StatelessWidget {
  const SettingsTab({super.key});

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);
    final int pluginCount = PluginRegistry.instance.count;

    return Scaffold(
      appBar: AppBar(title: const Text('系统设置')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: <Widget>[
          Container(
            decoration: BoxDecoration(
              color: theme.colorScheme.surface,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: theme.colorScheme.outline),
            ),
            child: Column(
              children: <Widget>[
                const ListTile(
                  leading: Icon(Icons.verified_outlined),
                  title: Text('宿主底座版本'),
                  trailing: Text('v1.0.0-pure-shell'),
                ),
                Divider(height: 1, color: theme.colorScheme.outline),
                ListTile(
                  leading: const Icon(Icons.extension_outlined),
                  title: const Text('已挂载插件数'),
                  trailing: Text('$pluginCount'),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
