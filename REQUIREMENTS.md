# BTELO Coding - 需求与当前状态

## 一、用户需求（2026-05-01）

### 1. 思考框设计（核心需求）

**发送消息后行为：**
- 用户发送消息后，**立即**显示思考框（不等待响应）
- 思考框图标是一个**转动的灯泡**
- 思考过程中，框外**轮动显示**当前收到的子类型消息（如"调用 Bash..."、"深度思考中…"）
- 思考完成后，灯泡**停止转动**

**思考框内容：**
- 所有子类型消息在**一个框内**：
  - 工具调用（TOOL_CALL）
  - 文件操作（FILE_OP）
  - 思考过程（THINKING）
  - 错误信息（ERROR）
  - 系统消息（SYSTEM）

**展开/折叠逻辑：**
- 点击思考框展开 → 显示每个子类型的折叠列表
- 每个子类型有**独立图标**：
  - 💡 思考过程（ThinkingPurple）
  - ⌨️ 工具调用（BubbleGradientStart）
  - 📄 文件操作（WarningAmber）
  - ❌ 错误信息（RedError）
  - 💻 系统消息（TextSecondary）
- 点击子类型展开 → 显示该类型的具体消息内容

**Claude文本回复：**
- 思考完成后，Claude的文本回复作为**单独消息**显示在思考框下方

### 2. 键盘交互

- 点击输入框时，键盘弹出，**消息列表应该跟着上移**（类似微信）
- 发送消息后，键盘**不要自动收回**

### 3. 消息复制

- 用户发送的消息下方显示**复制图标**（✓ 已实现）

---

## 二、当前代码状态

### 核心文件

| 文件 | 作用 | 状态 |
|------|------|------|
| `app/.../ui/chat/ChatViewModel.kt` | 消息处理、思考会话管理 | 已更新，需真机验证 |
| `app/.../ui/chat/MessageList.kt` | 消息列表渲染 | 已更新 |
| `app/.../ui/chat/MessageBubble.kt` | 消息气泡与统一思考框组件 | 已更新 |
| `app/.../ui/chat/ChatScreen.kt` | 聊天页面布局、键盘 insets | 已尝试修复，需真机验证 |
| `app/.../data/repository/MessageRepositoryImpl.kt` | 消息存储、WebSocket处理 | 已更新 |
| `server/relay.js` | Relay Server，Bridge 注册、App 发现设备、WebSocket 转发 | 已更新 |
| `server/bridge.js` | Bridge CLI（Windows console input 优先，resume fallback） | 已更新，需跨平台测试 |
| `server/output-parser.js` | 解析 Claude Code stream-json，并向 App 发送 structured_output | 已修复，默认导出 OutputParser 类 |

### 数据模型

```kotlin
// 思考消息类型
enum class ThinkingMessageType {
    THINKING,    // 思考过程
    TOOL_CALL,   // 工具调用
    FILE_OP,     // 文件操作
    ERROR,       // 错误信息
    SYSTEM       // 系统消息
}

// 思考消息
data class ThinkingMessage(
    val type: ThinkingMessageType,
    val content: String,
    val timestamp: Long
)

// 思考会话
data class ThinkingSession(
    val isActive: Boolean,
    val messages: List<ThinkingMessage>,  // 所有子类型消息
    val currentMessageType: ThinkingMessageType?,  // 当前显示的类型
    val currentMessage: String  // 框外显示的文字
)
```

### 消息流程

```
用户发送消息
    ↓
sendMessage() → 立即设置 thinkingSession.isActive = true
    ↓
显示思考框（转动灯泡 + "等待响应…"）
    ↓
Bridge 发送 structured_output
    ↓
MessageRepositoryImpl.handleMessage()
    ├── 保存到数据库
    └── emit 到 structuredOutputFlow
    ↓
ChatViewModel.processStructuredOutput()
    ├── THINKING → 添加到 thinkingSession.messages
    ├── TOOL_CALL → 添加到 thinkingSession.messages
    ├── FILE_OP → 添加到 thinkingSession.messages
    ├── ERROR → 添加到 thinkingSession.messages
    ├── SYSTEM → 添加到 thinkingSession.messages
    └── CLAUDE_RESPONSE → 结束思考会话，显示文本回复
```

### Bridge 当前输入路径

```
手机 command
    ↓
Relay 转发到 Bridge
    ↓
bridge.js 优先查找正在运行的 Claude Code PID
    ↓
Windows: 通过 console-bridge/console-sender.ps1 发送键盘输入
    ↓
Bridge 监听 Claude JSONL 文件变化
    ↓
new_message / structured_output 回传手机
```

> 注意：`executeResumeCommand()` 仍保留为 fallback/legacy 路径，但当前主要交互链路优先使用 Windows console input。macOS/Linux 需要单独验证 fallback 是否完整覆盖。

---

## 三、已知问题

### 问题1：思考框不立即显示
**原因：** `loadMessages()` 函数在收到数据库消息时会清空 `thinkingSession`
**状态：** 已修复（移除了清空逻辑）

### 问题2：工具调用不在思考框内
**原因：** 工具调用被保存到数据库后，触发 `loadMessages`，清空了思考会话
**状态：** 已修复

### 问题3：键盘弹出时消息列表不上移
**原因：** `imePadding()` 与 Scaffold 的交互问题
**状态：** 已尝试修复，需真机验证
**当前方案：**
- `Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0))`
- 内容 Column 使用 `.imePadding()`

### 问题4：灯泡转动不平滑
**原因：** `infiniteRepeatable` 动画在重组时可能重置
**状态：** 需要真机测试

### 问题5：Bridge 结构化输出未启用
**原因：** `server/output-parser.js` 之前导出对象，`bridge.js` 期望默认导出 `OutputParser` 函数/类
**状态：** 已修复。`output-parser.js` 现在默认导出 `OutputParser` 类，并保留命名导出供测试使用。

### 问题6：公网 Relay 信息暴露
**原因：** `/bridges` 当前可直接列出 bridge 设备信息和工作目录
**状态：** 待修复
**建议：**
- `/bridges` 不返回完整 `work_dir`
- 增加失败次数限制和 auth code 过期时间
- Relay 暴露公网时增加访问保护

---

## 四、测试命令

```bash
# 构建
./gradlew assembleDebug

# 安装
adb install -r app/build/outputs/apk/debug/Yami-Coding-1.0.0-mvp.apk

# 启动
adb shell am start -n com.btelo.coding/.MainActivity

# 启动 Relay Server
cd server && npm start

# 启动 Bridge
node server/bridge.js --workdir "C:\workspace\btelo-coding-v2" --session <session_id>

# 测试 output parser
node server/output-parser.js
# 然后粘贴 Claude Code stream-json 输出

# 查看 Bridge 日志
cat /c/Users/win/AppData/Local/Temp/claude/C--workspace-btelo-coding-v2/*/tasks/*.output
```

---

## 五、参考：微信行为

- 点击输入框 → 键盘弹出 → 消息列表**整体上移**
- 输入框始终在键盘正上方
- 发送消息后 → 键盘**保持打开**
- 新消息 → 自动滚动到底部

---

## 六、技术实现细节

### ChatViewModel 关键逻辑

```kotlin
// sendMessage() - 发送消息后立即显示思考框
fun sendMessage() {
    // 1. 创建用户消息
    // 2. 立即设置 thinkingSession.isActive = true
    // 3. 设置 currentMessage = "等待响应…"
}

// processStructuredOutput() - 处理结构化输出
fun processStructuredOutput(structuredMsg: Message) {
    // 1. THINKING 且无活跃会话 → 开始新思考会话
    // 2. 活跃会话 + 非CLAUDE_RESPONSE → 添加到 thinkingSession.messages
    // 3. 活跃会话 + CLAUDE_RESPONSE → 结束思考会话，显示文本回复
}
```

### MessageRepositoryImpl 关键逻辑

```kotlin
// handleMessage() - 处理 WebSocket 消息
when (message) {
    is BteloMessage.StructuredOutput -> {
        // 1. 转换为领域模型
        // 2. emit 到 structuredOutputFlow（ViewModel 监听）
        // 3. 保存到数据库（所有类型都保存）
    }
}
```

### MessageList 渲染逻辑

```kotlin
LazyColumn {
    // 1. 普通消息列表
    items(messages) { MessageBubble(it) }

    // 2. 思考框（活跃时显示）
    if (thinkingSession.isActive) {
        item { ThinkingBox(session = thinkingSession) }
    }

    // 3. 流式内容（Claude 文本回复）
    if (isStreaming && streamingContent != "…") {
        item { AiStreamingBubble(streamingContent) }
    }
}
```

### ThinkingBox 组件结构

```
┌─────────────────────────────────────┐
│ 💡 调用 Bash...           [展开]    │  ← 框外显示当前消息
├─────────────────────────────────────┤
│ ▼ 思考过程 (3)                      │  ← 展开后显示子类型
│   • 思考内容1...                    │
│   • 思考内容2...                    │
│                                     │
│ ▶ 工具调用 (5)                      │
│ ▶ 文件操作 (2)                      │
│ ▶ 错误信息 (0)                      │
│ ▶ 系统消息 (1)                      │
└─────────────────────────────────────┘
```

---

## 七、待修复清单

- [ ] 键盘弹出时消息列表上移：真机验证
- [ ] 发送消息后键盘不自动收回：真机验证
- [ ] 思考框展开/折叠动画优化
- [ ] 灯泡转动平滑度优化
- [ ] `/bridges` 信息暴露与 auth code 限流
- [ ] macOS/Linux fallback 路径测试
