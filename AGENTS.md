# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Overview

BTELO Coding is a mobile-to-desktop bridge: the Android app connects to a Node.js relay server running on the same machine as Codex CLI, allowing users to interact with Codex sessions from their phone via WebSocket.

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

# Start bridge CLI (resume mode — spawns Codex -p per command)
npm run bridge -- --workdir /path/to/project

# Start bridge in PTY mode (interactive pseudo-terminal via node-pty)
npm run pty -- --session <session_id> --workdir /path/to/project

# Restart relay (kills process on 8080, relaunches)
node server/restart.js

# Test output parser interactively (paste stream-json lines)
node server/output-parser.js
```

Environment variables: `PORT` (default 8080), `PUBLIC_IP` (for remote access QR), `RELAY_SERVER` (default http://localhost:8080).

The server auto-discovers Codex interactive sessions from `~/.Codex/sessions/`, presents them to the mobile client, and relays commands/responses in real time by watching `~/.Codex/projects/<encoded-path>/<sessionId>.jsonl` via `fs.watch` (100ms debounce).

### Bridge modes

- **Resume** (default, `bridge.js`): Spawns `Codex -p <cmd> -r <sessionId> --output-format stream-json` per command. Good for quick commands.
- **PTY** (`pty-bridge.js`): Uses `node-pty` (optional dep) for an interactive pseudo-terminal. Falls back to `spawn`. Supports terminal resize via `pty_resize` messages.

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
│  QR Scan: CameraX + ML Kit Barcode Scanning                          │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

Key libraries: Compose BOM 2024.02, Hilt 2.50, Room 2.6.1, OkHttp 4.12, Tink 1.12 (crypto), Retrofit 2.9, CameraX 1.3.1, ML Kit 17.2

## Key patterns

- **Repository pattern**: Domain interfaces in `domain/repository/`, implementations in `data/repository/` wired via Hilt.
- **WebSocket protocol**: All messages are `type`-discriminated JSON. Full protocol defined in `MessageProtocol.kt` (Android) and handled by switch statements in server bridge code.

  | Type | Direction | Purpose |
  |------|-----------|---------|
  | `command` | Mobile → Bridge | User command to Codex |
  | `output` | Bridge → Mobile | Raw CLI stdout/stderr |
  | `structured_output` | Bridge → Mobile | Parsed stream-json (6 subtypes) |
  | `sync_history` | Bridge → Mobile | Bulk history from session JSONL |
  | `new_message` | Bridge → Mobile | Real-time message from JSONL watcher |
  | `select_session` | Mobile → Bridge | Switch to specific Codex session |
  | `status` | Bidirectional | Connection state changes |
  | `session_state` | Bidirectional | Peer connection state broadcast |
  | `publicKey` | Bidirectional | E2E encryption key exchange |
  | `keyRotation` | Bidirectional | Key rotation handshake |
  | `encryptedData` | Bidirectional | E2E encrypted payload with key version |
  | `ping`/`pong` | Transport | Heartbeat (30s interval) |

- **Structured output** (`output-parser.js` → `structured_output` messages): Parses Codex `stream-json` into 6 `OutputType` subtypes, each rendered differently in `MessageBubble.kt`:

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
  - `POST /bridge/register` → bridge registration, returns connect + bridge tokens
  - `GET /connect?token=` → mobile client auth, returns WebSocket URL + token
  - `GET /sessions` → list Codex sessions on this machine
  - `GET /status` → health check
  - `GET`/`POST /restart` → server restart
  - `WebSocket /ws?token=` → mobile client channel
  - `WebSocket /bridge/ws?token=` → bridge client channel
