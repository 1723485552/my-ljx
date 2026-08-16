import 'base_tool_plugin.dart';

/// 工具动态插槽注册中心
///
/// 单一事实来源：宿主外壳与各业务表现层均通过本中心感知已挂载工具，
/// 严禁在宿主层硬编码任何工具实例。
class PluginRegistry {
  PluginRegistry._();

  static final PluginRegistry instance = PluginRegistry._();

  final Map<String, BaseToolPlugin> _registeredPlugins = {};

  /// 注册单个工具插件
  void register(BaseToolPlugin plugin) {
    _registeredPlugins[plugin.manifest.id] = plugin;
  }

  /// 批量注册
  void registerAll(List<BaseToolPlugin> plugins) {
    for (final BaseToolPlugin p in plugins) {
      register(p);
    }
  }

  /// 获取所有已注册工具列表
  List<BaseToolPlugin> getAllPlugins() => _registeredPlugins.values.toList();

  /// 按分类检索工具
  List<BaseToolPlugin> getPluginsByCategory(String category) {
    return _registeredPlugins.values
        .where((BaseToolPlugin p) => p.manifest.category == category)
        .toList();
  }

  /// 根据 ID 查找特定工具
  BaseToolPlugin? getPluginById(String id) => _registeredPlugins[id];

  /// 已挂载插件数量
  int get count => _registeredPlugins.length;

  /// 清空所有已注册插件（仅供测试隔离使用，宿主运行时禁止调用）
  void reset() => _registeredPlugins.clear();
}
