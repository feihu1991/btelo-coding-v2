# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 当前状态（2026-05-01）

**核心需求：** 实现统一思考框，所有子类型消息在一个框内显示，展开后可折叠查看。

**已知问题：**
1. 键盘弹出时消息列表不上移（imePadding 问题）
2. 详见 `REQUIREMENTS.md`

## Overview

BTELO Coding is a mobile-to-desktop bridge: the Android app connects to a Node.js relay server running on the same machine as Claude Code CLI, allowing users to interact with Claude Code sessions from their phone via WebSocket.

## Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Install to connected device
adb install -r app/build/outputs/apk/debug/Yami-Coding-1.0.0-mvp.apk

# Launch app
adb shell am start -n com.btelo.coding/.MainActivity

# Run unit tests
./gradlew test

# Run specific test
./gradlew test --tests "com.btelo.coding.data.remote.websocket.MessageProtocolTest"
```

Test framework: JUnit 4.13.2 + MockK 1.13.9 + kotlinx-coroutines-test 1.7.3.

## Relay Server

```bash
# Start relay server (port 8080, override with PORT env var)
cd server && npm start

# Start bridge CLI (resume mode — spawns claude -p per command)
npm run bridge -- --workdir /path/to/project

# Start bridge in PTY mode (interactive pseudo-terminal via node-pty)
npm run pty -- --session <session_id> --workdir /path/to/project

# Restart relay (kills process on 8080, relaunches)
node server/restart.js

# Test output parser interactively (paste stream-json lines)
node server/output-parser.js
```

Environment variables: `PORT` (default 8080), `PUBLIC_IP` (for remote access), `RELAY_SERVER` (default http://localhost:8080).

### Connection flow (auth code)

1. Bridge starts → generates 6-digit auth code → registers with relay
2. Mobile app opens → user enters relay server address → taps "发现设备"
3. Mobile discovers bridges via `GET /bridges` → user selects bridge → enters 6-digit auth code
4. Relay validates auth code → returns WebSocket token → mobile connects via `/ws?token=`
5. Bridge auto-discovers Claude Code sessions from `~/.claude/sessions/`, reads JSONL history

Real-time output: bridge watches `~/.claude/projects/<encoded-path>/<sessionId>.jsonl` via `fs.watch` (100ms debounce), streams new messages to mobile.

### Bridge modes

- **Resume** (default, `bridge.js`): Spawns `claude -p <cmd> -r <sessionId> --output-format stream-json` per command. Good for quick commands.
- **PTY** (`pty-bridge.js`): Uses `node-pty` (optional dep) for an interactive pseudo-terminal. Falls back to `spawn`. Supports terminal resize via `pty_resize` messages. Auto-selects the best matching Claude session on startup.

## Architecture

```
┌─ Android App (Kotlin + Compose) ────────────────────────────────────┐
│                                                                      │
│  UI (Compose screens)                                                │
│  └── ScanScreen → ChatScreen                                        │
│                                                                      │
│  Domain (pure Kotlin, no Android deps)                               │
│  ├── model/: Message, Session, User, Device                         │
│  └── repository/: AuthRepository, MessageRepository, etc. (interfaces)│
│                                                                      │
│  Data (implements domain repositories)                               │
│  ├── local/: Room DB (SessionEntity, MessageEntity, DeviceEntity)   │
│  ├── remote/                                                        │
│  │   ├── api/: AuthApi, SyncApi (manual OkHttp calls, no interface) │
│  │   ├── websocket/: WebSocketClientFactory, EnhancedWebSocketClient│
│  │   │              MessageProtocol (JSON serialization)             │
│  │   └── encryption/: CryptoManager (X25519+ChaCha20), KeyRotation  │
│  └── sync/: SyncManager (offline-first, 60s periodic sync)          │
│                                                                      │
│  DI: Hilt (AppModule.kt provides all singletons)                    │
│  Push: Firebase Cloud Messaging (FcmTokenManager)                   │
│  Auth: 6-digit auth code via manual entry                             │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

Key libraries: Compose BOM 2024.02, Hilt 2.50, Room 2.6.1, OkHttp 4.12, Tink 1.12 (crypto), Retrofit 2.9

## Key patterns

- **Repository pattern**: Domain interfaces in `domain/repository/`, implementations in `data/repository/` wired via Hilt.
- **WebSocket protocol**: All messages are `type`-discriminated JSON. Full protocol defined in `MessageProtocol.kt` (Android) and handled by switch statements in server bridge code.

  | Type | Direction | Purpose |
  |------|-----------|---------|
  | `command` | Mobile → Bridge | User command to Claude Code |
  | `output` | Bridge → Mobile | Raw CLI stdout/stderr |
  | `structured_output` | Bridge → Mobile | Parsed stream-json (6 subtypes) |
  | `sync_history` | Bridge → Mobile | Bulk history from session JSONL |
  | `new_message` | Bridge → Mobile | Real-time message from JSONL watcher |
  | `select_session` | Mobile → Bridge | Switch to specific Claude session |
  | `status` | Bidirectional | Connection state changes |
  | `session_state` | Bidirectional | Peer connection state broadcast |
  | `publicKey` | Bidirectional | E2E encryption key exchange |
  | `keyRotation` | Bidirectional | Key rotation handshake |
  | `encryptedData` | Bidirectional | E2E encrypted payload with key version |
  | `ping`/`pong` | Transport | Heartbeat (30s interval) |

- **Structured output** (`output-parser.js` → `structured_output` messages): Parses Claude Code `stream-json` into 6 `OutputType` subtypes, each rendered differently in `MessageBubble.kt`:

  | OutputType | Render style |
  |------------|-------------|
  | `CLAUDE_RESPONSE` | Standard message bubble |
  | `TOOL_CALL` | Collapsible tool card with metadata |
  | `FILE_OP` | File operation card |
  | `THINKING` | Dashed border, collapsible |
  | `ERROR` | Red error card |
  | `SYSTEM` | Gray system card |

- **Reconnection**: Exponential backoff with jitter — 1s initial, 30s max, 10 attempts, 2x multiplier, 20% jitter. Managed by `EnhancedWebSocketClient`.
- **Encryption**: E2E using X25519 key exchange → HKDF derivation → ChaCha20-Poly1305 AEAD. `KeyRotationManager` rotates keys per session (default 7-day interval, up to 5 history versions). Currently disabled in practice (plaintext in `EnhancedWebSocketClient.kt` line 129).
- **Navigation**: Compose Navigation with sealed `Screen` class — `Scan` and `Chat/{sessionId}`. Start destination is always `Scan`.
- **Room DB** version 3: sessions, messages, devices tables. Migrations: v1→v2 (key version fields), v2→v3 (PRD model fields).
- **Server API** (Express on port 8080):
  - `POST /bridge/register` → bridge registration with 6-digit `auth_code`, returns connect + bridge tokens
  - `GET /bridges` → list registered bridges (for mobile discovery)
  - `POST /bridges/:id/connect` → mobile authenticates via `auth_code`, returns WS token
  - `GET /connect?token=` → legacy token-based mobile auth (auth code is preferred)
  - `GET /sessions` → list Claude Code sessions on this machine
  - `GET /status` → health check
  - `GET`/`POST /restart` → server restart
  - `WebSocket /ws?token=` → mobile client channel
  - `WebSocket /bridge/ws?token=` → bridge client channel
