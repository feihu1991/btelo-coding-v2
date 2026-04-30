# BTELO Coding 架构文档

## 概述

BTELO Coding 是一个移动端远程控制 Claude Code 的解决方案，采用分层架构，通过 WebSocket 实现实时双向通信。

## 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        Mobile App                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐│
│  │  ChatScreen │  │ SessionList │  │      MessageBubble       ││
│  └──────┬──────┘  └──────┬──────┘  └────────────┬────────────┘│
│         │                 │                       │              │
│  ┌──────▼─────────────────▼───────────────────────▼────────────┐│
│  │                      ChatViewModel                          ││
│  │  - 消息状态管理                                                ││
│  │  - Structured Output 处理                                    ││
│  │  - 流式输出缓冲                                               ││
│  └─────────────────────────┬───────────────────────────────────┘│
│                            │                                     │
│  ┌─────────────────────────▼───────────────────────────────────┐│
│  │                    MessageRepository                        ││
│  │  - 消息持久化 (Room)                                        ││
│  │  - WebSocket 消息路由                                         ││
│  └─────────────────────────┬───────────────────────────────────┘│
│                            │                                     │
│  ┌─────────────────────────▼───────────────────────────────────┐│
│  │              EnhancedWebSocketClient                         ││
│  │  - 连接管理                                                  ││
│  │  - 重连机制                                                  ││
│  │  - 消息序列化/反序列化                                       ││
│  └─────────────────────────────────────────────────────────────┘│
└────────────────────────────────────────────────────────────────┘
                              │ WebSocket
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Relay Server (relay.js)                    │
│                                                                 │
│  ┌──────────────────┐    ┌──────────────────┐                    │
│  │  Mobile WS       │    │  Bridge WS       │                    │
│  │  (/ws?token=)    │◄──►│  (/bridge/ws)   │                    │
│  └────────┬─────────┘    └────────┬─────────┘                    │
│           │                        │                              │
│           └───────────┬────────────┘                              │
│                       │ Message Forwarding                        │
│  ┌────────────────────▼────────────────────────────────────┐    │
│  │                   Token Management                         │    │
│  │  - connect_token: 移动端认证                               │    │
│  │  - bridge_token: Bridge 端认证                             │    │
│  └───────────────────────────────────────────────────────────┘    │
│                                                                 │
│  Features:                                                      │
│  - structured_output 消息透传                                    │
│  - 心跳机制 (30s ping/pong)                                     │
│  - 会话状态广播                                                  │
└─────────────────────────────────────────────────────────────────┘
                              │ WebSocket
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Bridge CLI (bridge.js)                     │
│                                                                 │
│  ┌────────────────────┐    ┌────────────────────┐               │
│  │ Session Discovery   │    │   Output Parser    │               │
│  │ - 扫描 .claude/    │    │ (output-parser.js) │               │
│  │ - JSONL 历史       │    │                    │               │
│  └─────────┬──────────┘    └─────────┬──────────┘               │
│            │                          │                          │
│  ┌─────────▼──────────────────────────▼─────────────┐          │
│  │              Claude Code Process                   │          │
│  │  claude -p <cmd> -r <session_id>                  │          │
│  │  --output-format stream-json                        │          │
│  └───────────────────────────────────────────────────┘          │
│                                                                 │
│  PTY Mode (pty-bridge.js):                                      │
│  ┌─────────────────────────────────────────────┐                │
│  │              node-pty                         │                │
│  │  - 创建伪终端                                │                │
│  │  - 支持终端 resize                           │                │
│  │  - 交互式输入/输出                           │                │
│  └─────────────────────────────────────────────┘                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Claude Code CLI                            │
│                                                                 │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐    │
│  │  Session Mgmt  │  │  Tool Executor │  │  JSONL Logger  │    │
│  └────────────────┘  └────────────────┘  └────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

## 消息协议

### 消息类型

| Type | Direction | 说明 |
|------|-----------|------|
| `command` | Mobile → Bridge | 用户发送的命令 |
| `output` | Bridge → Mobile | 原始 CLI 输出 |
| `structured_output` | Bridge → Mobile | 解析后的结构化输出 |
| `sync_history` | Bridge → Mobile | 历史消息同步 |
| `new_message` | Bridge → Mobile | 实时新消息 (JSONL 监听) |
| `select_session` | Mobile → Bridge | 选择 Claude 会话 |
| `status` | Bidirectional | 连接状态 |
| `session_state` | Bidirectional | 会话状态广播 |
| `ping/pong` | Bidirectional | 心跳检测 |

### 结构化输出格式

```json
{
  "type": "structured_output",
  "output_type": "tool_call",
  "content": "Running: npm install",
  "metadata": {
    "toolId": "struct-xxx",
    "toolName": "Bash",
    "toolType": "bash",
    "command": "npm install",
    "filePath": null,
    "isFileOp": false,
    "parserVersion": "1.0.0"
  },
  "timestamp": "2024-01-01T12:00:00.000Z"
}
```

### Output Type 枚举

| 枚举值 | 说明 | 渲染样式 |
|--------|------|----------|
| `CLAUDE_RESPONSE` | 普通文本响应 | 标准消息气泡 |
| `TOOL_CALL` | 工具调用请求 | 可折叠工具卡片 |
| `FILE_OP` | 文件操作详情 | 文件操作卡片 |
| `THINKING` | 思考过程 | 虚线框，可折叠 |
| `ERROR` | 错误信息 | 红色错误卡片 |
| `SYSTEM` | 系统消息 | 灰色系统卡片 |

## 数据流

### 1. 命令执行流程

```
用户输入命令
    │
    ▼
Mobile App ─── command ──► Relay ─── command ──► Bridge
                                              │
                                              ▼
                                        Claude Code CLI
                                        (stream-json 输出)
                                              │
                                              ▼
Bridge Output Parser ──────────────────────────┼──► Relay ───► Mobile App
      │                                              │
      ▼                                              ▼
structured_output                            output (原始)
消息分组缓冲
      │
      ▼
达到消息边界时发送到 App
```

### 2. 结构化输出处理

```
CLI stream-json 输出
    │
    ▼
OutputParser.process()
    │
    ├─► assistant event ──► 提取文本块 ──► CLAUDE_RESPONSE
    │
    ├─► tool_use block ──► 提取工具信息 ──► TOOL_CALL
    │
    ├─► tool_result ──► 工具执行结果 ──► 追加到当前消息
    │
    ├─► subtype: thinking ──► THINKING
    │
    └─► error ──► ERROR
```

### 3. 消息分组策略

结构化输出消息根据以下规则分组：

1. **工具调用组**：TOOL_CALL 开始新组
2. **工具结果追加**：TOOL_RESULT 追加到最近的 TOOL_CALL 组
3. **响应结束**：遇到下一个 TOOL_CALL 或流结束时，输出之前的 CLAUDE_RESPONSE

## 数据库设计 (Room)

### MessageEntity

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | String | 主键 |
| `sessionId` | String | 所属会话 |
| `content` | String | 消息内容 |
| `type` | String | 消息类型枚举 |
| `timestamp` | Long | 时间戳 |
| `isFromUser` | Boolean | 是否用户消息 |
| `outputType` | String? | 结构化输出类型 |
| `metadataJson` | String? | 结构化输出元数据 |
| `thinkingContent` | String? | 思考过程内容 |

## WebSocket 重连策略

```kotlin
ReconnectConfig(
    initialDelayMs = 1000L,     // 初始重连延迟
    maxDelayMs = 30000L,        // 最大延迟
    maxAttempts = 10,           // 最大重试次数
    multiplier = 2.0,            // 延迟倍增
    jitterPercent = 20           // 随机抖动
)
```

## PTY 模式

PTY (Pseudo-Terminal) 模式提供完整的终端体验：

### 特性

- 真实的伪终端
- 支持 ANSI 转义序列
- 终端 resize 支持
- 交互式输入/输出

### 限制

- 需要 node-pty 依赖
- Linux/macOS 支持良好
- Windows 需要额外配置

### 使用场景

- 需要交互式 CLI 工具
- 需要查看实时终端输出
- 需要终端特定功能（如 vim、less）

## 安全性

### Token 机制

- **Connect Token**: 移动端扫码获取，一次性
- **Bridge Token**: Bridge 注册时生成
- **WS Token**: 移动端连接时生成

### 密钥轮换

App 支持端到端加密和密钥轮换，通过 `keyRotation` 消息类型协调。

## 性能优化

### 1. 消息缓冲

结构化输出消息在 Bridge 端缓冲，达到边界条件时一次性发送到 App。

### 2. JSONL 文件监听

使用 `fs.watch` 而非轮询检测会话变更，100ms 防抖。

### 3. 流式渲染

Android App 支持流式渲染，逐字显示 Claude 输出。

## 扩展点

### 1. 新增 Output Type

在 `output-parser.js` 中添加新的解析规则：

```javascript
case 'new_type':
    this.emitStructuredOutput('new_type', event.content, {
        // metadata
    });
    break;
```

### 2. 自定义渲染

在 Android App 的 `MessageBubble.kt` 中添加新的渲染逻辑：

```kotlin
outputType == OutputType.NEW_TYPE -> NewTypeBubble(message, clipboardManager)
```

### 3. 新增 Bridge 实现

参考 `pty-bridge.js` 创建新的 Bridge 实现。
