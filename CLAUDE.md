# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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

## Relay Server

```bash
# Start (dependency: Node.js + Claude Code CLI installed)
cd server && npm start

# Restart
node server/restart.js
```

The server auto-discovers Claude Code interactive sessions from `~/.claude/sessions/`, presents them to the mobile client, and relays commands/responses in real time by watching `~/.claude/projects/<encoded-path>/<sessionId>.jsonl`.

## Architecture

```
┌─ Android App (Kotlin + Compose) ────────────────────────────────────┐
│                                                                      │
│  UI (Compose screens)                                                │
│  ├── ScanScreen → AgentsScreen → ChatScreen                         │
│  └── SessionListScreen, NotificationSettingsScreen                  │
│                                                                      │
│  Domain (pure Kotlin, no Android deps)                               │
│  ├── model/: Message, Session, User, Device                         │
│  └── repository/: AuthRepository, MessageRepository, etc. (interfaces)│
│                                                                      │
│  Data (implements domain repositories)                               │
│  ├── local/: Room DB (SessionEntity, MessageEntity, DeviceEntity)   │
│  ├── remote/                                                        │
│  │   ├── api/: AuthApi, SyncApi (manual Retrofit, no interface)     │
│  │   ├── websocket/: WebSocketClientFactory, EnhancedWebSocketClient│
│  │   │              MessageProtocol (JSON serialization)             │
│  │   └── encryption/: CryptoManager (X25519+ChaCha20), KeyRotation  │
│  └── sync/: SyncManager                                             │
│                                                                      │
│  DI: Hilt (AppModule.kt provides all singletons)                    │
│  Push: Firebase Cloud Messaging (FcmTokenManager)                   │
│  QR Scan: CameraX + ML Kit Barcode Scanning                          │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

Key libraries: Compose BOM 2024.02, Hilt 2.50, Room 2.6.1, OkHttp 4.12, Tink 1.12 (crypto), Retrofit 2.9, CameraX 1.3.1, ML Kit 17.2

## Key patterns

- **Repository pattern**: Domain interfaces in `domain/repository/`, implementations in `data/repository/` wired via Hilt
- **WebSocket protocol**: All messages are `type`-discriminated JSON (command, output, status, sync_history, new_message, select_session, keyRotation). Serialized/deserialized by `MessageProtocol`.
- **Encryption**: E2E using X25519 key exchange → HKDF derivation → ChaCha20-Poly1305 AEAD. `KeyRotationManager` rotates keys per session.
- **Navigation**: Compose Navigation with sealed `Screen` class defining routes. Start destination depends on whether `ws_token` exists in SharedPreferences (auto-connect vs. scan).
- **Room DB** at version 2: sessions, messages, devices tables. Uses destructive migration fallback.
- **Server API** (Express on port 8080):
  - `GET /connect?token=` → creates relay session, returns WebSocket URL
  - `GET /sessions` → list Claude Code sessions on this machine
  - `GET /status` → health check
  - `POST /restart` → server restart
  - `GET /restart` → server restart (GET alias)
  - `WebSocket /ws?token=` → main communication channel
