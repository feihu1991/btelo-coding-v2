# BTELO Coding PRD 重构计划

## 背景

参照 `BTELO-Coding-产品需求文档.md` (PRD v1.0) 对 Android 应用进行重构。当前实现为线性导航（扫描 → Agent → 聊天），配色方案与 PRD 附录 A（基于视频帧分析）不一致，且缺少多项 P0 功能：底部 Tab 栏、Token 计数、斜杠命令面板、AI 思考状态、工具执行记录、Files 标签页、Browser 标签页。

---

## Phase 1: 配色与主题基础

**修改文件：** `ui/theme/Color.kt`、`ui/theme/Theme.kt`；修复 `InputBar.kt`、`ScanScreen.kt` 中的硬编码颜色

- 背景色：`#0A0A0A` → `#1A1A1A`，卡片：`#111114` → `#2A2A2A`
- 次要文字：`rgba(0.6)` → `#888888`
- 新增 token：`AccentBlue` (#3B82F6)、`ThinkingPurple` (#8B5CF6)、`SendGradientStart/End`、`SkillTagBorder` (#F97316)
- Material3 角色重映射：primary → AccentBlue，background → 新 AppBackground

---

## Phase 2: 底部 Tab 导航

**新建：** `ui/navigation/BottomNavBar.kt`、`ui/navigation/MainScreen.kt`
**修改：** `AppNavigation.kt`、`MainActivity.kt`、`ScanScreen.kt`

- 4 个 Tab：Agents、Team、Files、Browser（对应 PRD 3.1 节）
- 选中态：蓝色 #3B82F6 图标+文字；未选中：#888888
- 每个 Tab 内嵌独立 NavHost，配置 saveState/restoreState
- 扫描页 → 主页跳转（连接后弹出自身）

---

## Phase 3: 领域模型升级

**修改：** `domain/model/Session.kt`、`Message.kt`；所有 Room Entity、EntityMappers、AppDatabase (v2→v3)、SessionDao、Repository 层

- Session：新增 `path`、`messageCount`、`tokenCount`、`status`（ACTIVE/IDLE/COMPLETED）
- Message：新增 `sender`、`tools: List<ToolExecution>?`，新增类型 TOOL、THINKING
- 新增：`ToolExecution`（type, command, output?, status）、`ToolType` 枚举、`ToolStatus` 枚举
- Room 迁移 MIGRATION_2_3

---

## Phase 4: Agents 页面增强

**新建：** `ui/chat/ToolExecutionBubble.kt`、`ui/chat/SlashCommandPanel.kt`、`domain/model/SlashCommand.kt`
**修改：** `AgentsScreen.kt`、`AgentsViewModel.kt`、`MessageBubble.kt`

- 顶部栏：Token 计数（"287 context tokens"）+ 命令标签（"/deploy-iphone"）
- AI 思考气泡：紫色脉冲图标 + "Thinking..." 文字
- 工具执行记录：按工具类型着色图标（Bash=绿色、Read=蓝色、Grep=橙色等）
- 完成状态条："N tool(s) used · Done"，可展开
- 斜杠命令面板：输入 "/" 时弹出，过滤列表，内置 PRD 中的命令
- 发送按钮：绿色渐变 → 紫色渐变（#8B5CF6→#6366F1）

---

## Phase 5: Files 标签页

**新建：** `ui/files/FilesScreen.kt`、`ui/files/FilesViewModel.kt`、`domain/model/GitRepoInfo.kt`

- Git 仓库卡片网格（3 列，120×80dp）
- 路径浏览区域（路径输入框 + 目录列表）
- "+ New Session Here" 按钮（蓝色 #3B82F6）
- MVP 阶段：使用 mock 数据，未连接时显示空状态

---

## Phase 6: Browser 标签页

**新建：** `ui/browser/BrowserScreen.kt`、`ui/browser/BrowserViewModel.kt`、`domain/model/ProxyEntry.kt`

- Web Proxy 标题 + 说明文字
- Add Port Proxy / Add Website 操作卡片
- Auto-proxy ports 开关
- 代理条目列表（正常和错误状态）
- MVP 阶段：仅本地状态管理

---

## Phase 7: Team 占位页 + 集成收尾

**新建：** `ui/team/TeamScreen.kt`

- "Coming Soon" 占位页面
- 全流程端到端验证

---

## 依赖关系

```
Phase 1 (配色/主题)
  ├── Phase 2 (导航重构)
  ├── Phase 3 (领域模型)
  ├── Phase 5 (Files 页) ──┐
  └── Phase 6 (Browser 页) ─┤
Phase 4 (Agents 增强) ← 依赖 1+2+3
Phase 7 (集成收尾) ← 依赖全部
```

## 关键风险与对策

| 风险 | 对策 |
|---|---|
| Room 迁移损坏已有数据 | 已配置 fallbackToDestructiveMigration |
| AgentsScreen 代码过长 | 提取子组件到独立文件 |
| Tab 切换丢失导航状态 | 嵌套 NavHost 使用 saveState/restoreState |
| Tab 切换断开 WebSocket | WebSocketClientFactory 为 @Singleton，跨 Tab 保持 |

## 验证方式

- Phase 1：目视检查所有页面配色一致；grep 搜索旧色值
- Phase 2：扫描→连接→Tab 切换→聊天→返回→断开，全流程测试
- Phase 3：编译通过；Database Inspector 检查新字段；覆盖安装测试迁移
- Phase 4：输入 "/" 弹出命令面板；命令标签显示；工具气泡着色正确
- Phase 5-6：Tab 页正确展示 mock 数据
- 最终：`./gradlew assembleDebug` 编译通过；APK 安装后所有 Tab 功能正常
