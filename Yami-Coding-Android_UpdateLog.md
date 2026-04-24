# Yami-Coding-Android 功能更新日志

**版本**: v1.1.0  
**更新日期**: 2024年  
**项目**: BTELO Coding Android

---

## 功能增强清单

### P0 优先级（高）

#### 1. WebSocket重连机制 ✅
- **指数退避重连**: 1s → 2s → 4s → 8s → 16s → 30s (最大延迟)
- **抖动机制**: 20% 随机抖动，避免多客户端同时重连
- **网络状态监听**: 使用 ConnectivityManager 实时监听网络状态
- **自动恢复**: 网络恢复后自动重连
- **重连状态回调**: `WebSocketEvent.Reconnecting` 通知UI层

#### 2. Room持久化 ✅
- **SessionEntity**: 会话信息持久化
  - 支持离线访问会话列表
  - 自动同步连接状态
  - 记录最后活跃时间
  
- **MessageEntity**: 消息记录持久化
  - 支持离线查看历史消息
  - 外键关联会话，自动级联删除
  - 支持消息清理（保留指定天数）
  
- **DeviceEntity**: 设备绑定信息持久化
  - 设备在线状态管理
  - 最后活跃时间记录

#### 3. 独立WebSocketClient ✅
- **每个会话独立的WebSocket实例**: 使用 `WebSocketClientFactory` 管理
- **会话与WebSocket映射**: 通过 sessionId 关联
- **生命周期管理**: 自动创建、销毁、重连
- **多会话支持**: 支持同时连接多个会话

---

### P1 优先级（中）

#### 4. JSON解析异常处理 ✅
- **统一异常封装**: `AppException` sealed class
  - `JsonParseException`: JSON解析失败
  - `NetworkException`: 网络相关错误
  - `WebSocketException`: WebSocket错误
  - `CryptoException`: 加密相关错误
  - `DatabaseException`: 数据库错误
  - `KeyStoreException`: 密钥存储错误
  
- **日志记录**: `Logger` 工具类统一管理日志
- **用户友好提示**: 在UI层显示友好的错误信息

#### 5. Android Keystore密钥存储 ✅
- **私钥安全存储**: 使用 Android Keystore 加密存储私钥
- **AES-GCM加密**: 使用 256-bit AES-GCM 加密私钥
- **会话隔离**: 每个会话独立的密钥别名
- **兼容性处理**: 支持 API 23+

---

## 技术改进

### 架构改进
- **Clean Architecture 强化**: 清晰的分层架构
- **依赖注入完善**: 使用 Hilt 进行依赖管理
- **响应式编程**: 使用 Flow 进行状态管理

### 代码质量
- **单元测试**: 新增多个单元测试类
  - `AppExceptionTest`: 异常处理测试
  - `MessageProtocolTest`: 消息协议测试
  - `WebSocketConfigTest`: WebSocket配置测试
- **错误处理**: 全面的异常捕获和日志记录
- **代码注释**: KDoc 风格文档注释

---

## 文件变更清单

### 新增文件
```
app/src/main/java/com/btelo/coding/
├── util/
│   ├── AppException.kt          # 统一异常封装
│   └── Logger.kt                 # 日志工具
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt        # Room数据库
│   │   ├── EntityMappers.kt      # 实体映射
│   │   ├── dao/
│   │   │   ├── SessionDao.kt     # 会话DAO
│   │   │   ├── MessageDao.kt     # 消息DAO
│   │   │   └── DeviceDao.kt      # 设备DAO
│   │   └── entity/
│   │       ├── SessionEntity.kt  # 会话实体
│   │       ├── MessageEntity.kt  # 消息实体
│   │       └── DeviceEntity.kt   # 设备实体
│   ├── remote/
│   │   ├── network/
│   │   │   └── NetworkMonitor.kt # 网络状态监听
│   │   └── websocket/
│   │       └── factory/
│   │           ├── WebSocketConfig.kt       # WebSocket配置
│   │           ├── EnhancedWebSocketClient.kt # 增强WebSocket客户端
│   │           └── WebSocketClientFactory.kt  # WebSocket工厂
│   └── repository/
│       ├── SessionRepositoryImpl.kt # 会话仓库实现
│       └── MessageRepositoryImpl.kt # 消息仓库实现
└── data/remote/encryption/
    └── SecureKeyStore.kt           # Android Keystore密钥存储

app/src/test/java/com/btelo/coding/
├── util/
│   └── AppExceptionTest.kt        # 异常测试
└── data/remote/websocket/
    ├── MessageProtocolTest.kt      # 消息协议测试
    └── factory/
        └── WebSocketConfigTest.kt   # WebSocket配置测试
```

### 修改文件
```
app/build.gradle.kts                # 添加Room依赖
app/src/main/java/com/btelo/coding/
├── di/AppModule.kt                 # 更新DI配置
├── data/remote/websocket/
│   └── MessageProtocol.kt         # 添加异常处理
├── domain/repository/
│   ├── SessionRepository.kt       # 添加新方法
│   └── MessageRepository.kt        # 添加新方法
```

---

## 使用指南

### WebSocket连接
```kotlin
// 创建配置
val config = WebSocketConfig(
    sessionId = "session-123",
    serverAddress = "wss://example.com",
    token = "your-token",
    reconnectConfig = ReconnectConfig(
        initialDelayMs = 1000L,
        maxDelayMs = 30000L,
        multiplier = 2.0
    )
)

// 获取或创建客户端
val client = webSocketFactory.getOrCreate(config)

// 监听连接状态
scope.launch {
    client.connectionState.collect { state ->
        when (state) {
            is ConnectionState.Connected -> { /* 已连接 */ }
            is ConnectionState.Reconnecting -> { /* 重连中，attempt=${state.attempt} */ }
            is ConnectionState.Error -> { /* 错误: ${state.message} */ }
            else -> { /* 其他状态 */ }
        }
    }
}
```

### 消息持久化
```kotlin
// 获取会话消息（自动从Room加载）
messageRepository.getMessages(sessionId).collect { messages ->
    // 处理消息列表
}

// 清理旧消息（保留30天）
messageRepository.cleanOldMessages(sessionId, keepDays = 30)
```

### 密钥存储
```kotlin
// 生成并存储密钥对
val keyPair = secureKeyStore.generateAndStoreKeyPair(sessionId)

// 获取存储的密钥对
val cachedKeyPair = secureKeyStore.getKeyPair(sessionId)

// 删除密钥
secureKeyStore.deleteKeyPair(sessionId)
```

---

## 向后兼容性

- ✅ 保持原有 API 不变
- ✅ 新增功能完全向后兼容
- ✅ 数据库迁移使用 `fallbackToDestructiveMigration()`

---

## 未来规划

- [ ] 实现密钥轮换机制（前向保密）
- [ ] 添加消息加密的 HKDF 密钥派生
- [ ] 实现消息离线同步
- [ ] 添加 WebSocket 连接池优化
- [ ] 完善 UI 层连接状态显示

---

## 贡献者

AI Code Assistant
