# 军规级研发与架构规范 (Military-Grade Engineering Standards)

> 本文件为全局单一事实来源 (SSOT, Single Source of Truth)。所有 Agent 与研发活动必须无条件遵守。

## 1. 架构红线 (Architectural Invariants)
- **单向依赖原则**：依赖流向严格限制为 `Features -> Core`。`Core` 模块严禁引入任何 `Features` 层的代码或状态。
- **零污染与即用即走**：每个工具在进入时创建独立上下文，退出页面时必须在 `dispose()` 中销毁所有内存快照、定时器及通信通道，禁止任何全局常驻泄漏。
- **契约先行 (Contract-First)**：任何新功能的增加，必须先冻结 Data Schema / Interface 契约，严禁在无类型定义的情况下直接写业务代码。

## 2. 代码质量与类型安全
- **零警告准则 (Zero Warnings)**：编译过程与静态代码检查（Linter）不得出现任何 `Warning` 或 `Info`。
- **强类型与不可变性**：所有数据传输对象（DTO/State）必须使用 `final` 字段，杜绝 `dynamic` 的泛滥使用。
- **防御性错误边界**：所有 JSON 解析与外部调用必须包裹在 `try-catch` 或 `Result<T, E>` 中，并提供合理的 Fallback 状态，严禁引发红屏 Crash。

## 3. UI 与设计系统约束
- **严禁花哨与硬编码**：禁止使用强渐变、高饱和度色彩与无语义的硬编码尺寸（如 `margin: 17.5`），所有间距、圆角、字阶必须引用 `AppTheme`。
- **纯扁平高对比度**：严格遵循中性黑白灰 + 极简单强调色体系，确保双端在深色/浅色模式下的完美对比度。

## 4. 提交与变更准则
- 每次 Agent 输出必须包含三要素：**改动文件路径**、**完整生产级代码（严禁省略号 `// TODO` 或伪代码）**、**自测验证指令**。

## 5. Agent 流水线协作契约
1. `01_po_agent` 产出 `PRD.json`（输入/动作/边界矩阵）。
2. `02_arch_agent` 审核 PRD 并冻结 State / DSL / Action 契约，确认后方可下发。
3. `03_dev_agent` 按契约产出 100% 完整可编译代码，挂载至 `PluginRegistry`。
4. `04_qa_agent` 编写全覆盖测试，缺陷直接打回 Dev 形成自愈闭环。
5. `05_release_agent` 审计权限、压缩体积（单包 < 50KB）、更新版本清单。

> 任何一步不达标，流水线回退至上游 Agent，不得向下游放行。
## 6. 防御性工程红线 (Defensive Guardrails)
- **依赖准入限制**：严禁 Agent 未经明确指示修改 `pubspec.yaml` 引入第三方 Package；一律优先使用 Dart 标准库 (`dart:convert`, `dart:math` 等) 与 Flutter 原生组件。
- **爆炸半径隔离 (Additive Only)**：编写新工具插件时，仅允许在业务目录下新增文件，严禁未经允许修改 `lib/core/` 基础设施或已有稳定插件。
- **资源生命周期闭环**：组件内创建的所有 `TextEditingController`、`FocusNode`、`Timer`、`StreamSubscription`，必须在 `dispose()` 中 100% 显式注销，杜绝内存泄漏与全局静态变量污染。
- **离线优先与优雅降级**：所有基础工具必须在无网环境下 100% 可用；涉及网络请求必须设置超时（<=3000ms）并提供本地缓存兜底，严禁无响应卡死。
- **UI 规范与防截断**：间距、圆角必须为 4 的倍数（4, 8, 12, 16, 24）；所有文本展示必须配置 `TextOverflow.ellipsis` 或弹性布局，杜绝屏幕像素溢出警告。