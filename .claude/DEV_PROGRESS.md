# 开发进度记录

> 最后更新: 2026-04-28

## 当前状态: Batch 1 进行中

### ✅ 已完成

| 文件 | 改动 |
|---|---|
| `BrowserViewModel.kt` | 新增 5 个方法: addPortProxy, addWebsiteProxy, refreshProxy, retryProxy, closeProxy |

### 🔧 进行中 — Batch 1 剩余

**BrowserScreen.kt — 待修改:**
1. "Add Port Proxy" 卡片加 `.clickable { viewModel.showAddPortDialog() }`（行109-122）
2. "Add Website" 卡片加 `.clickable { viewModel.showAddWebsiteDialog() }`（行123-136）
3. 新增两个 `AlertDialog` 弹窗（端口输入 + URL 输入），使用 `var port by remember { mutableStateOf("") }` 管理输入
4. 代理条目箭头加 `.clickable`（行203 的 "→" 文字）
5. 浏览器工具栏 6 个图标加 `.clickable` 回调（行219-221）
6. 需要新增 import: `androidx.compose.runtime.mutableStateOf`, `androidx.compose.runtime.setValue`, `androidx.compose.runtime.getValue`

**FilesScreen.kt — 待修改:**
- Search/Refresh/Settings 按钮接入 ViewModel
- 路径复制图标加 `.clickable { clipboardManager.setText(...) }`
- 添加 SnackbarHost

**FilesViewModel.kt — 待修改:**
- createSessionAtPath() 调用 sessionRepository.createSession()
- 新增 toggleSearch/refreshCurrentPath

**AgentsScreen.kt — 待修改:**
- `if (false)` → `if (uiState.isLoading)` (行566)
- 通知铃铛接入导航
- 工具栏图标 (AutoAwesome/Star/Code/Casino) 接入实际行为

**AgentsViewModel.kt — 待修改:**
- disconnect() 中先调用 messageRepository.disconnect(sessionId)

### ⏳ 待开始

| Batch | 内容 |
|---|---|
| Batch 2 | 消息渲染分流 + 通知 + 语音 |
| Batch 3 | Skill Tags + 任务完成弹窗 |
| Batch 4 | Provider Control + 设备管理 |
| Batch 5 | Git 管理面板 |
| Batch 6 | Team 骨架 + Token 计数 + 收尾 |

### 📋 关键文件索引

- 计划文件: `.claude/plans/prancy-skipping-dusk.md`
- 功能清单: `BTELO-Coding-完整功能设计清单.md`
- 完成报告: `BTELO-Coding-重构完成报告.md`

### 🔨 编译命令

```bash
./gradlew assembleDebug
```