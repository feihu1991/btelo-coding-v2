# Issue #4 处理报告：实现密钥轮换机制（前向保密）

**处理时间**: 2026-04-25 04:25
**Issue编号**: #4
**Issue标题**: 实现密钥轮换机制（前向保密）
**状态**: ✅ 已完成实现

---

## 一、需求回顾

### Issue 原始需求
- 实现前向保密（Forward Secrecy）
- ECDH 临时密钥协商
- 定时密钥轮换策略（如每周/每月）
- 密钥轮换时重新协商会话密钥
- 保留历史密钥用于解密旧消息
- 轮换时发送握手消息同步状态

---

## 二、实现方案

### 1. 核心组件：KeyRotationManager

新建 `KeyRotationManager.kt`，负责：
- **密钥版本管理**: 创建、存储多版本密钥
- **定时轮换**: 支持配置轮换间隔（默认7天）
- **ECDH握手**: 实现 `initiate` → `accept` → `complete` 三次握手
- **历史保留**: 保留最多5个历史密钥版本用于解密

```kotlin
// 核心类
class KeyRotationManager {
    fun createNewKeyVersion(sessionId: String): KeyVersion
    fun decryptWithHistory(sessionId: String, ciphertext: ByteArray, keyVersionHint: Int?): ByteArray?
    fun generateRotationHandshake(sessionId: String): KeyRotationMessage
    fun handleRotationHandshake(sessionId: String, message: KeyRotationMessage): KeyRotationMessage?
    fun shouldRotate(sessionId: String): Boolean
    fun setRotationInterval(sessionId: String, days: Int)
}
```

### 2. 消息协议扩展

在 `MessageProtocol.kt` 新增两种消息类型：

```kotlin
// 密钥轮换握手消息
data class KeyRotation(
    val action: String,        // "initiate" | "accept" | "complete"
    val newPublicKey: String,  // Base64编码的新公钥
    val keyVersion: Int,        // 密钥版本号
    val timestamp: Long
)

// 带密钥版本的数据消息
data class EncryptedData(
    val data: String,
    val stream: StreamType,
    val keyVersion: Int        // 用于标识使用哪个密钥版本解密
)
```

### 3. 数据库升级

**SessionEntity 新增字段**:
- `currentKeyVersion: Int = 1` - 当前密钥版本
- `lastKeyRotation: Long = 0` - 上次密钥轮换时间戳
- `rotationIntervalDays: Int = 7` - 轮换间隔（天）

**MessageEntity 新增字段**:
- `keyVersion: Int = 1` - 加密该消息时使用的密钥版本

**数据库迁移**: v1 → v2，支持增量迁移

### 4. WebSocket 集成

**EnhancedWebSocketClient 新增功能**:
- `keyRotationManager` - 密钥轮换管理器（可选注入）
- `triggerKeyRotation()` - 手动触发密钥轮换
- `startKeyRotationCheck()` - 定时检查是否需要轮换
- `getCurrentKeyVersion()` - 获取当前密钥版本

**新增事件**:
```kotlin
data class KeyRotationStarted(val sessionId: String, val newVersion: Int)
data class KeyRotationCompleted(val previousVersion: Int, val newVersion: Int)
data class KeyRotationFailed(val sessionId: String, val error: String)
```

---

## 三、密钥轮换流程

```
客户端                          服务器
  |                              |
  |  1. 发送 keyRotation(initiate, newPublicKey, version=2)  |
  | -----------------------------> |
  |                              |
  |  2. 接收 remotePublicKey，生成新密钥                      |
  |  3. 发送 keyRotation(accept, newPublicKey, version=3)  |
  | <------------------------------ |
  |                              |
  |  4. 双方完成密钥协商，使用新密钥加密后续消息                 |
  |  5. 发送 keyRotation(complete, version=3)              |
  | -----------------------------> |
  |                              |
```

---

## 四、技术特性

| 特性 | 实现 |
|------|------|
| 前向保密 | ✅ X25519 + ECDH |
| 密钥派生 | ✅ HKDF-SHA256 |
| 消息加密 | ✅ ChaCha20-Poly1305 |
| 历史密钥保留 | ✅ 最多5个版本 |
| 定时轮换 | ✅ 可配置间隔 |
| 握手协议 | ✅ 三次握手 |

---

## 五、文件变更清单

### 新增文件
```
app/src/main/java/com/btelo/coding/data/remote/encryption/
└── KeyRotationManager.kt          # 密钥轮换管理器
```

### 修改文件
```
app/src/main/java/com/btelo/coding/data/local/
├── AppDatabase.kt                  # 数据库升级 v2
└── entity/
    ├── SessionEntity.kt            # 新增密钥版本字段
    └── MessageEntity.kt            # 新增密钥版本字段

app/src/main/java/com/btelo/coding/data/remote/websocket/
├── MessageProtocol.kt              # 新增消息类型
└── factory/
    ├── EnhancedWebSocketClient.kt  # 集成密钥轮换
    └── WebSocketConfig.kt          # 新增轮换事件

app/src/main/java/com/btelo/coding/di/
└── AppModule.kt                   # 添加依赖注入
```

---

## 六、使用指南

### 自动轮换
连接建立后，每分钟检查是否到达轮换时间：
```kotlin
// 默认每7天轮换一次
keyRotationManager.setRotationInterval(sessionId, 7)
```

### 手动触发
```kotlin
// 主动触发密钥轮换
webSocketClient.triggerKeyRotation()
```

### 获取轮换状态
```kotlin
// 监听密钥轮换状态
webSocketClient.keyRotationState.collect { state ->
    when (state) {
        is KeyRotationState.Rotating -> { /* 正在轮换 */ }
        is KeyRotationState.Completed -> { /* 轮换完成 */ }
        is KeyRotationState.Error -> { /* 轮换失败 */ }
        else -> {}
    }
}
```

---

## 七、Git 提交信息

```
commit 33d3bc9
feat: 实现密钥轮换机制（前向保密）

Issue #4 实现内容：
- 新增 KeyRotationManager.kt
- 修改 MessageProtocol.kt
- 修改 SessionEntity.kt, MessageEntity.kt
- 修改 EnhancedWebSocketClient.kt
- 修改 WebSocketConfig.kt
- 修改 AppDatabase.kt (数据库迁移)
- 修改 AppModule.kt (依赖注入)
```

**GitHub**: https://github.com/feihu1991/Yami-Coding-Android/commit/33d3bc9

---

## 八、后续优化建议

1. **服务器端支持**: 需要同步更新服务器实现密钥轮换握手协议
2. **密钥版本导出**: 支持导出密钥版本信息用于备份
3. **紧急轮换**: 支持收到安全警告时立即触发轮换
4. **轮换日志**: 记录详细的历史轮换日志便于审计
