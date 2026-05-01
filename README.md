# BTELO Coding

> 手机远程控制 Claude Code — 通过手机 App 随时随地与 Claude 交互，实时同步到终端。

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](./LICENSE)
[![Android](https://img.shields.io/badge/Android-10%2B-green.svg)](https://developer.android.com)

## 工作原理

手机消息通过 WebSocket 发送到本地 Relay Server，Bridge 用 `SendKeys`（Windows SendInput API）直接把按键输入到当前 Claude Code 终端，回复通过 JSONL 文件监听实时回传手机。

```
手机 App ──WebSocket──▶ Relay Server ◀──WebSocket── Bridge CLI
                                                    │
                                           SendKeys (PowerShell)
                                                    │
                                           ┌────────▼────────┐
                                           │  Claude Code     │
                                           │  (当前终端)       │
                                           └────────┬────────┘
                                                    │
                                          JSONL 文件监听
                                                    │
                                           ◀── 回复回传手机 ──
```

## 快速开始

### 1. 启动 Relay Server

```bash
cd server
npm install
npm start          # 启动 relay，默认 8080 端口
```

### 2. 启动 Bridge CLI

```bash
cd server
node bridge.js --workdir /path/to/project
```

Bridge 启动后显示 6 位 Auth Code。

### 3. 手机连接

1. 打开 BTELO App
2. 输入服务器地址（默认已填 ngrok 公网地址）
3. 点击 **发现设备** → 选择 Bridge → 输入 Auth Code
4. 连接成功，开始收发消息

### 4. 公网访问（可选）

```bash
# ngrok（固定域名，免费 1 端点在线）
ngrok config add-authtoken <your-token>
ngrok http 8080

# localtunnel（每次重启 URL 变化）
npx localtunnel --port 8080
```

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `PORT` | Server 端口 | 8080 |
| `PUBLIC_IP` | 公网 IP | 自动检测局域网 |
| `RELAY_SERVER` | Relay 地址 | http://localhost:8080 |

## 消息类型

| 类型 | 方向 | 说明 | 手机渲染 |
|------|------|------|----------|
| `command` | 手机 → Bridge | 用户命令 | 右侧渐变色气泡 |
| `new_message` | Bridge → 手机 | 文字回复 | 左侧深色气泡 |
| `structured_output` | Bridge → 手机 | 结构化消息 | 根据子类型渲染 |
| `sync_history` | Bridge → 手机 | 批量历史（最近100条） | — |
| `hook_event` | Plugin → Relay → 手机 | Claude Code 生命周期事件 | — |
| `session_state` | Bidirectional | 连接状态广播 | 状态指示器 |

### 结构化输出子类型

| Output Type | 触发条件 | 渲染 |
|-------------|----------|------|
| `thinking` | Claude 思考过程 | 🟣 可折叠 Thinking 气泡（紫色虚线框，旋转灯泡图标） |
| `tool_call` | 工具调用（Read/Bash/Write等） | 🟡 可折叠工具卡片（展开查看参数） |
| `claude_response` | 普通文本回复 | 标准 AI 消息气泡（支持 markdown） |

## 项目结构

```
├── app/                          # Android App (Kotlin + Compose)
│   └── src/main/java/com/btelo/coding/
│       ├── domain/model/          # Message, Session, MessageMetadata
│       ├── data/
│       │   ├── local/             # Room DB + DataStore
│       │   ├── remote/            # WebSocket, MessageProtocol, encryption
│       │   └── repository/        # MessageRepositoryImpl, SessionRepository
│       └── ui/
│           ├── chat/              # ChatScreen, MessageList, MessageBubble, InputBar
│           ├── scan/              # ScanScreen, ScanViewModel (bridge discovery)
│           └── theme/             # Compose 主题和颜色
│
├── server/                       # Node.js Server
│   ├── relay.js                  # 消息转发服务器（Express + WebSocket）
│   ├── bridge.js                 # Bridge CLI（auth code + JSONL watcher）
│   ├── index.js                  # Legacy 兼容服务器
│   ├── output-parser.js          # stream-json 输出解析器
│   ├── btelo-plugin/             # Claude Code 插件（生命周期钩子）
│   │   ├── hooks.json            # 钩子配置
│   │   └── scripts/              # 钩子脚本
│   └── console-bridge/           # Win32 Console API 方案
│       ├── console-sender.ps1    # SendKeys 按键注入脚本
│       └── console_bridge.cpp    # N-API C++ 源码（可选编译）
│
└── CLAUDE.md                     # Claude Code 工作指引
```

## 构建

```bash
# Android APK
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/Yami-Coding-1.0.0-mvp.apk

# 运行测试
./gradlew test
```

## 技术栈

**Android:** Kotlin, Jetpack Compose, Room, Hilt, OkHttp, Coil
**Server:** Node.js, Express, WebSocket (ws)
**通信:** PowerShell SendKeys (Windows), Win32 Console API, JSONL 文件监听

## License

MIT
