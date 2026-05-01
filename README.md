# BTELO Coding

> 手机远程控制 Claude Code — 通过手机 App 随时随地与 Claude 交互，实时查看代码变更。

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](./LICENSE)
[![Android](https://img.shields.io/badge/Android-10%2B-green.svg)](https://developer.android.com)
[![Claude Code](https://img.shields.io/badge/Claude%20Code-v3.5-blue.svg)](https://docs.anthropic.com/claude-code)

## 项目介绍

BTELO Coding 是一个让你在手机上远程控制 Claude Code 的解决方案。它包含两个部分：

1. **Android App** (Kotlin + Jetpack Compose)
   - 完整的聊天界面，支持流式输出
   - 会话管理
   - 结构化消息渲染（工具调用、思考过程等）

2. **Server** (Node.js)
   - **Relay Server** (`relay.js`)：消息转发器
   - **Bridge CLI** (`bridge.js`)：Claude Code CLI 管理器
   - **PTY Bridge** (`pty-bridge.js`)：伪终端模式
   - **Console Bridge** (`console-bridge/`)：Win32 Console API 控制方案

## 架构图

```
┌─────────────────┐      WebSocket       ┌─────────────────┐
│   Mobile App    │ ←──────────────────→ │  Relay Server   │
│  (Kotlin/UI)   │                      │   (relay.js)    │
└─────────────────┘                      └────────┬────────┘
                                                  │
                                                  WebSocket
                                                  │
                                           ┌──────▼────────┐
                                           │ Bridge CLI     │
                                           │ (bridge.js /   │
                                           │  pty-bridge.js)│
                                           └──────┬────────┘
                                                  │
                                           ┌──────▼────────┐
                                           │ Claude Code    │
                                           │ (CLI)          │
                                           └────────────────┘
```

## 三通道控制方案

BTELO Coding v2 采用三通道方案实现对 Claude Code 的控制：

| 通道 | 方式 | 适用场景 | 优点 | 缺点 |
|------|------|----------|------|------|
| **Resume 通道** | `claude -p` 命令 | 快速命令执行 | 简单可靠 | 无交互 |
| **PTY 通道** | node-pty 伪终端 | 交互式操作 | 真终端体验 | 平台差异 |
| **Console API 通道** | Win32 Console API | Windows 深度控制 | 可读写控制台 | 仅 Windows |

### Console API 通道 (POC 阶段)

> 🔬 **当前状态**：概念验证 (POC) 阶段

使用 Win32 Console API (`AttachConsole` / `WriteConsoleInput` / `ReadConsoleOutput`) 直接读写 Claude Code 控制台缓冲区：

```powershell
# PowerShell POC 测试
powershell -ExecutionPolicy Bypass -File server/console-bridge/test-console-bridge.ps1
```

**核心 API**：
- `AttachConsole(pid)` - 附加到目标进程控制台
- `WriteConsoleInput()` - 模拟键盘输入
- `ReadConsoleOutput()` - 读取屏幕内容
- `FreeConsole()` - 分离控制台

详见 [Console Bridge 文档](./server/console-bridge/README.md)

## 快速开始

### 1. 安装 Claude Code

```bash
npm install -g @anthropic-ai/claude-code
claude --version  # 确认安装成功
```

### 2. 启动 Relay Server

```bash
cd server
npm install
npm start
```

### 3. 启动 Bridge CLI

```bash
# Resume 模式（推荐）
npm run bridge -- --workdir /path/to/project

# PTY 模式（交互式终端）
npm run pty -- --session <session_id> --workdir /path/to/project
```

### 4. 扫码连接

Bridge 启动后会显示 QR 码，用 Android App 扫码即可连接。

## 两种运行模式

### Resume 模式 (默认)

使用 `claude -p <command> -r <session_id>` 执行命令：
- 每次命令启动新进程
- 通过 `--output-format stream-json` 获取结构化输出
- 适合快速命令执行

```bash
node bridge.js --mode resume --workdir /path/to/project
```

### PTY 模式

使用 node-pty 创建伪终端：
- 真正的交互式终端体验
- 支持终端resize
- 适合需要交互的场景

```bash
node pty-bridge.js --session <session_id> --workdir /path/to/project
```

## 结构化输出

BTELO Coding v2 支持解析 Claude Code 的 `stream-json` 输出，转换为结构化消息：

| Output Type | 说明 | App 渲染 |
|-------------|------|----------|
| `claude_response` | 普通文本响应 | 标准消息气泡 |
| `tool_call` | 工具调用 | 可折叠工具卡片 |
| `file_op` | 文件操作 | 文件操作卡片 |
| `thinking` | 思考过程 | 虚线框，可折叠 |
| `error` | 错误信息 | 红色错误卡片 |
| `system` | 系统消息 | 灰色系统卡片 |

## 文件结构

```
Yami-Coding-Android-new/
├── app/                          # Android App
│   └── src/main/java/com/btelo/coding/
│       ├── domain/model/          # 领域模型 (Message, Session, etc.)
│       ├── data/
│       │   ├── local/             # Room 数据库
│       │   ├── remote/            # WebSocket 客户端
│       │   └── repository/        # Repository 实现
│       └── ui/
│           ├── chat/              # 聊天界面
│           ├── session/           # 会话列表
│           └── theme/             # Compose 主题
│
├── server/                       # Node.js Server
│   ├── relay.js                  # 消息转发服务器
│   ├── bridge.js                 # Bridge CLI (resume 模式)
│   ├── pty-bridge.js            # PTY 模式 bridge
│   ├── output-parser.js          # CLI 输出解析器
│   └── console-bridge/           # Win32 Console API POC
│       ├── test-console-bridge.ps1  # PowerShell POC
│       ├── console_bridge.cpp     # N-API C++ 源码
│       ├── binding.gyp            # node-gyp 配置
│       ├── index.js               # Node.js 封装
│       └── test.js                # 测试脚本
│
└── docs/                         # 设计文档
```

## 环境要求

- **Android**: 10+ (API 29)
- **Node.js**: 16+
- **Claude Code**: 最新版本
- **平台**: Linux/macOS/Windows

## 配置

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `PORT` | Server 端口 | 8080 |
| `PUBLIC_IP` | 公网 IP (远程访问) | 自动检测 |
| `RELAY_SERVER` | Relay Server 地址 | http://localhost:8080 |

### App 连接配置

首次连接需要配置 Server 地址和 Token。

## 开发

### 构建 Android App

```bash
./gradlew assembleDebug
```

### 运行 Server

```bash
cd server
node relay.js
```

### 测试 Console Bridge POC (Windows)

```bash
cd server/console-bridge

# PowerShell 版本（无需编译）
powershell -ExecutionPolicy Bypass -File test-console-bridge.ps1

# Node.js 版本（需要编译）
npm install
npx node-gyp rebuild
node test.js
```

### 测试结构化输出解析器

```bash
node server/output-parser.js
# 然后粘贴 Claude Code 的 stream-json 输出
```

## License

MIT License - see [LICENSE](./LICENSE) 文件
