import 'package:flutter/material.dart';

/// 统一工具元数据清单
class ToolManifest {
  const ToolManifest({
    required this.id,
    required this.version,
    required this.name,
    required this.description,
    required this.category,
    required this.icon,
  });

  final String id;
  final String version;
  final String name;
  final String description;
  final String category;
  final IconData icon;
}

/// 军规级标准工具插件抽象类 (所有后续自定义工具的基类)
///
/// 契约约束（见 .agent/rules/RULES.md）：
/// - [buildView] 必须返回独立 UI 视图，进入时创建独立上下文。
/// - [dispose] 必须在退出页面时被调用，销毁内存快照、定时器与通信通道，禁止常驻泄漏。
abstract class BaseToolPlugin {
  ToolManifest get manifest;

  /// 构建该工具的独立 UI 视图
  Widget buildView(BuildContext context);

  /// 退出工具时的内存释放钩子
  void dispose();
}
