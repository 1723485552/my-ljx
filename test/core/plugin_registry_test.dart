import 'package:agent_forge/core/plugin/base_tool_plugin.dart';
import 'package:agent_forge/core/plugin/plugin_registry.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

class _StubPlugin implements BaseToolPlugin {
  _StubPlugin(this.manifest);

  @override
  final ToolManifest manifest;

  @override
  Widget buildView(BuildContext context) => const SizedBox.shrink();

  @override
  void dispose() {}
}

void main() {
  group('PluginRegistry', () {
    setUp(() => PluginRegistry.instance.reset());

    test('register adds plugin and count increments', () {
      const ToolManifest manifest = ToolManifest(
        id: 'stub',
        version: '1.0.0',
        name: 'Stub',
        description: 'Stub tool',
        category: 'Test',
        icon: Icons.abc,
      );
      PluginRegistry.instance.register(_StubPlugin(manifest));

      expect(PluginRegistry.instance.count, 1);
      expect(PluginRegistry.instance.getPluginById('stub'), isNotNull);
    });

    test('getPluginsByCategory filters correctly', () {
      const ToolManifest a = ToolManifest(
        id: 'a',
        version: '1.0.0',
        name: 'A',
        description: 'desc',
        category: 'X',
        icon: Icons.abc,
      );
      const ToolManifest b = ToolManifest(
        id: 'b',
        version: '1.0.0',
        name: 'B',
        description: 'desc',
        category: 'Y',
        icon: Icons.abc,
      );
      PluginRegistry.instance.registerAll(<BaseToolPlugin>[
        _StubPlugin(a),
        _StubPlugin(b),
      ]);

      expect(PluginRegistry.instance.getPluginsByCategory('X').length, 1);
      expect(PluginRegistry.instance.getPluginsByCategory('Z').length, 0);
    });
  });
}
