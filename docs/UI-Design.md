# BTELO Coding Android - UI 设计文档

> 参考来源：`ref_screenshots/` 中 8 张 iOS 截图 + `Btelo Coding — Code from anywhere` 产品页 + 知乎文章
>
> 本文档是 Android UI 实现的唯一依据。所有页面、组件、颜色、交互都以本文档为准。

---

## 1. 设计语言

### 1.1 主题

深色主题，接近纯黑背景。

### 1.2 配色方案

| Token | 值 | 用途 |
|-------|-----|------|
| `AppBackground` | `#0A0A0A` | 页面背景 |
| `Surface` | `#111114` | 卡片、输入框背景 |
| `SurfaceElevated` | `#1A1D21` | AI 气泡、浮层 |
| `Border` | `rgba(255,255,255,0.08)` | 边框、分割线 |
| `TextPrimary` | `#F5F5F7` | 主文字 |
| `TextSecondary` | `rgba(245,245,247,0.6)` | 次级文字 |
| `TextTertiary` | `rgba(245,245,247,0.35)` | 辅助文字 |
| `AccentGreen` | `#22C55E` | 用户气泡、成功状态、发送按钮 |
| `AccentGreenDark` | `#16A34A` | 用户气泡渐变终点 |
| `AccentBlue` | `#3B82F6` | 链接、选中态 |
| `AccentPurple` | `#8B5CF6` | 会话标签圆点 |
| `ErrorRed` | `#EF4444` | 错误、断开 |

### 1.3 用户消息气泡

```
背景: linear-gradient(135deg, #22C55E, #16A34A)
文字: #FFFFFF
圆角: top-left 14dp, top-right 14dp, bottom-left 3dp, bottom-right 14dp
内边距: horizontal 14dp, vertical 10dp
```

### 1.4 AI 消息气泡

```
背景: #1A1D21
文字: #F5F5F7
圆角: top-left 14dp, top-right 14dp, bottom-left 14dp, bottom-right 3dp
内边距: horizontal 14dp, vertical 10dp
```

---

## 2. 全局导航结构

### 2.1 底部 Tab 栏

4 个 Tab，固定在页面底部：

| Tab | 图标 | 说明 |
|-----|------|------|
| Agents | 💬 (Chat bubble) | 主聊天页面，默认 Tab |
| Files | 📁 (Folder) | 文件浏览器（MVP 不实现） |
| Browser | 🧭 (Compass) | Web 代理（MVP 不实现） |
| Devices | 🖥️ (Monitor) | 设备管理（MVP 不实现） |

Tab 样式：
- 选中态：图标 + 文字为 `AccentGreen`
- 未选中：图标 + 文字为 `TextTertiary`
- 无背景 indicator

### 2.2 页面路由

```
ScanScreen (起始) → AgentsScreen (连接后) → ChatScreen (会话内)
                     ↕ SessionListScreen (侧栏)
```

---

## 3. 页面详细设计

### 3.1 ScanScreen - 扫码连接页

**已有实现，仅需调整颜色。**

布局：
```
┌─────────────────────────────────────┐
│                                     │
│         ┌──────────┐                │
│         │  C logo  │  ← 绿色圆形    │
│         └──────────┘                │
│                                     │
│       开始对话                       │  ← 标题
│    扫码或输入地址连接服务器            │  ← 副标题
│                                     │
│                                     │
│                                     │
│  ┌─────────────────────────────┐    │
│  │     [ 连接服务器 ]  📷       │    │  ← 主按钮（绿色渐变）
│  └─────────────────────────────┘    │
│                                     │
│       手动输入地址                   │  ← 链接
│                                     │
└─────────────────────────────────────┘
```

功能：
- 点击"连接服务器"→ 请求相机权限 → 打开 QR 扫描器
- 扫描 `btelo://IP:PORT/TOKEN` 格式的二维码
- 连接成功后如果有多个 session → 显示 Session 选择弹窗
- 连接成功且选好 session → 导航到 AgentsScreen
- 点击"手动输入地址"→ 弹出手动输入弹窗

### 3.2 AgentsScreen - 主聊天页面

**核心页面，对标 iOS 截图 01-02。**

整体布局（从上到下）：

```
┌─────────────────────────────────────┐
│ ≡  [ next-dashboard · 0 tokens ] 🔔👤│  ← TopBar
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────┐        │
│  │ AI: Write app/dashboard/ │        │  ← AI 消息气泡
│  │ analytics/DateRange...   │        │
│  │                          │        │
│  │ Segmented picker...      │        │
│  │ 3 tool(s) used · Done    │        │
│  └─────────────────────────┘        │
│                                     │
│        ┌──────────────────────┐     │
│        │ Can you add a date   │     │  ← 用户气泡（绿色）
│        │ range picker...      │     │
│        └──────────────────────┘     │
│                                     │
├─────────────────────────────────────┤
│ ●next-dashboard ●shopify ●go-api + ≡│  ← SessionTabs
├─────────────────────────────────────┤
│ Build analytics  Add SSO  [+]  [≡]  │  ← QuickActions
├─────────────────────────────────────┤
│ Message, / commands, @ history...   │  ← InputField
│ ✨  📎  ⭐  💻⑤  🎲  🎤     [发送] │  ← Toolbar
└─────────────────────────────────────┘
```

#### 3.2.1 AgentsTopBar

```
高度: 56dp
背景: AppBackground
内容:
  左: ≡ 汉堡菜单图标 (TextSecondary)
  中: 会话名 + "· 0 tokens" (pill 形状，Surface 背景)
  右: 🔔 通知图标 + 👤 头像圆形
```

#### 3.2.2 ChatArea (可滚动区域)

占据中间所有空间，LazyColumn 实现。

消息格式：
- AI 消息：左对齐，`SurfaceElevated` 背景气泡
  - 如果有工具调用，显示文件操作列表（绿色 ✓ 图标 + 文件名）
  - 底部显示 "N tool(s) used · Done"
- 用户消息：右对齐，绿色渐变气泡
- 流式输出时：AI 气泡底部显示闪烁光标 + "Claude 正在回复..."

#### 3.2.3 SessionTabsRow

```
高度: 44dp
背景: Surface
滚动: 水平可滚动
内容:
  每个 Tab: [彩色圆点 8dp] [会话名] 
  右侧: [+] 新建按钮 [≡] 列表按钮
圆点颜色: 按顺序循环使用 AvatarColors 列表
选中态: 文字为 TextPrimary，圆点加大到 10dp
未选中: 文字为 TextTertiary
```

#### 3.2.4 QuickActionsRow

```
高度: 40dp
背景: AppBackground
滚动: 水平可滚动
内容:
  每个 Chip: 圆角 8dp，Surface 背景，TextSecondary 文字
  示例: "Build analytics charts page", "Add SSO login"
  末尾: [+] 添加按钮 [≡] 更多按钮
点击: 将 chip 文字填入输入框并发送
```

#### 3.2.5 InputField

```
背景: Surface
圆角: 顶部 18dp
Placeholder: "Message, / commands, @ history, $ files"
文字颜色: TextPrimary
光标颜色: AccentGreen
```

#### 3.2.6 ToolbarRow

```
高度: 44dp
背景: Surface
图标（从左到右）:
  ✨ AI 模式切换 (AccentPurple)
  📎 附件 (TextSecondary)
  ⭐ 收藏/置顶 (TextSecondary)
  💻 代码 + 角标数字 (TextSecondary)
  🎲 随机/工具 (TextSecondary)
  🎤 语音输入 (TextSecondary)
  [发送按钮] 圆形 36dp, 激活时 AccentGreen，未激活时 Surface
```

### 3.3 SessionListScreen - 会话列表侧栏

**对标 iOS 截图 05。从左侧滑出或作为独立页面。**

```
┌──────────────────────────┐
│ ✨ Meet Agent             │  ← Banner (AccentPurple 背景)
├──────────────────────────┤
│ Sessions          [⊞][☰][+]│  ← 标题 + 视图切换按钮
│                          │
│ ⚡ next-dashboard    [2]  │  ← 会话项
│    ~/work/next-dashboard  │
│                          │
│ 🌟 shopify-store     [3]  │
│    ~/work/shopify-store   │
│                          │
│ ⚙️ go-api            [2]  │
│    ~/work/go-api          │
│                          │
│ 🔥 react-native-app       │
│    ~/work/react-native    │
│                          │
├──────────────────────────┤
│ Crons              [+]    │
│                          │
│ ⏰ daily:09:00            │
│   Run test suite...   ●  │  ← 绿色圆点 = 启用
│                          │
│ ⏰ interval:3600          │
│   Sync product data... ● │
│                          │
│ ⏰ daily:02:00            │
│   Backup PostgreSQL... ○  │  ← 橙色圆点 = 禁用
└──────────────────────────┘
```

#### 3.3.1 SessionItem

```
高度: 自适应
背景: 点击时 SurfaceElevated
内容:
  左: emoji 图标 (⚡🌟⚙️🔥 循环)
  中: 会话名 (TextPrimary, SemiBold)
      项目路径 (TextTertiary, bodySmall)
  右: 未读消息数 badge (AccentGreen 背景，白色文字)
点击: 导航到该会话的 ChatScreen
```

### 3.4 ChatScreen - 单会话聊天

**复用 AgentsScreen 的聊天区域，但有独立的 TopBar。**

```
┌─────────────────────────────────────┐
│ ← [会话名]                    HH:MM │  ← TopBar (返回 + 标题 + 时间)
├─────────────────────────────────────┤
│                                     │
│  (消息列表，同 AgentsScreen)         │
│                                     │
├─────────────────────────────────────┤
│ Message, / commands, @ history...   │  ← InputField
│ ✨  📎  ⭐  💻  🎲  🎤     [发送] │  ← Toolbar
└─────────────────────────────────────┘
```

---

## 4. 组件复用关系

```
MessageBubble (复用)
  ├── 用户气泡 (绿色渐变)
  ├── AI 气泡 (SurfaceElevated)
  ├── 工具调用列表 (新增)
  └── "N tool(s) used · Done" (新增)

InputBar (复用，修改 placeholder + toolbar)

SessionTabsRow (新增)
QuickActionsRow (新增)
AgentsTopBar (新增)
```

---

## 5. 数据流

```
ScanScreen
  ↓ 扫码连接
  ScanViewModel.onQrCodeScanned()
  ↓ 保存 serverAddress, wsToken, sessionId
  ↓ 导航到 AgentsScreen
  ↓
AgentsScreen
  ├── AgentsViewModel
  │   ├── loadSessions() → 从服务器获取会话列表
  │   ├── selectSession() → 切换当前会话
  │   └── sendMessage() → 发送消息
  ├── MessageList ← MessageRepository.observeOutput()
  └── SessionTabs ← SessionRepository.getSessions()
```

---

## 6. 服务器重启命令

```bash
# 杀掉旧进程
taskkill //F //PID $(netstat -ano | grep ':8080' | head -1 | awk '{print $5}') 2>/dev/null

# 启动新进程
cd server && nohup node index.js > server.log 2>&1 &

# 验证
cat server/server.log | head -20
```

---

## 7. 构建 & 部署

```bash
# 构建
./gradlew assembleDebug

# 安装到设备
"C:/Users/win/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r app/build/outputs/apk/debug/Yami-Coding-1.0.0-mvp.apk

# 启动应用
"C:/Users/win/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am start -n com.btelo.coding/.MainActivity
```
