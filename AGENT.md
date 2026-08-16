# NovaToolBox 智能协作总调度 (Master Agent Dispatcher)

> 本文件是当前工作区的最高调度入口。任何 Agent 会话在执行具体任务前，必须首先阅读并加载本规范。

## 1. 核心上下文与规则索引 (SSOT)
- **全局军规级研发规范**：`.agent/rules/RULES.md`（必须严格遵守架构红线、零警告、单向依赖与即用即走机制）。
- **项目底座类型**：Flutter 跨平台纯净宿主 + 插件化插槽（Clean Architecture）。
- **依赖方向约束**：`Features -> Core`，禁止反向耦合。

## 2. 角色分工与执行路由 (Role Router)
根据用户的输入意图，自动切换对应的 Agent 身份并读取对应规则：

| 触发场景 / 用户意图 | 接管角色 | 规则定义文件 | 核心交付物 |
| :--- | :--- | :--- | :--- |
| 提需求、做新工具、功能构想 | **PO-Agent** | `.agent/roles/01_po_agent.md` | `PRD.json` 规格定义 |
| 架构设计、状态定义、契约设计 | **Arch-Agent** | `.agent/roles/02_arch_agent.md` | State / DSL / Plugin Interface |
| 编写 Flutter / 插件代码 | **Dev-Agent** | `.agent/roles/03_dev_agent.md` | 100% 完整、可编译 Dart 代码 |
| 查 Bug、自测、写测试用例 | **QA-Agent** | `.agent/roles/04_qa_agent.md` | 测试报告与自愈补丁 |
| 打包、元数据注册、体积审计 | **Release-Agent** | `.agent/roles/05_release_agent.md` | 注册清单与体积审计表 |

## 3. 标准执行流程 (Execution Flow)
1. **识别意图**：解析用户指令，匹配上述角色。
2. **加载规则**：读取对应角色 `.md` 与 `RULES.md`。
3. **输出闭环**：严格按照军规输出【文件完整路径】+【生产级全量代码】+【自测指令】。
