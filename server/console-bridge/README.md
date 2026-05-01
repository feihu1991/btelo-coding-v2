# BTELO Console Bridge - Win32 Console API POC

> 使用 Win32 Console API 直接读写 Claude Code 控制台缓冲区，实现零侵入的双向消息同步。

## 🎯 目标

验证通过 `AttachConsole` + `WriteConsoleInput` + `ReadConsoleOutput` API 直接操控 Claude Code 终端的可行性。

## 📁 项目结构

```
console-bridge/
├── test-console-bridge.ps1   # PowerShell POC (零编译，直接运行)
├── console_bridge.cpp         # Node.js N-API C++ Addon 源码
├── binding.gyp               # node-gyp 构建配置
├── index.js                  # Node.js 封装层
├── test.js                   # Node.js 测试脚本
└── README.md                 # 本文件
```

## 🚀 快速开始

### 方法一：PowerShell POC（推荐，无需编译环境）

1. 在一个终端启动 Claude Code：
   ```powershell
   claude
   # 或
   npx @anthropic-ai/claude-code
   ```

2. 另开一个 PowerShell 终端运行 POC：
   ```powershell
   cd path/to/console-bridge
   powershell -ExecutionPolicy Bypass -File test-console-bridge.ps1
   ```

### 方法二：Node.js 版本（需要编译）

1. 安装依赖：
   ```powershell
   npm install
   ```

2. 编译原生模块：
   ```powershell
   npx node-gyp rebuild
   ```

3. 运行测试：
   ```powershell
   node test.js
   ```

## 📋 验证项目

运行测试后，验证以下功能是否正常工作：

| API | 功能 | 预期结果 |
|-----|------|----------|
| `AttachConsole` | 附加到 Claude Code 进程 | ✅ 连接成功 |
| `WriteConsoleInput` | 模拟键盘输入 "hello" | ✅ 终端收到输入 |
| `ReadConsoleOutput` | 读取屏幕内容 | ✅ 获取到屏幕文本 |
| `FreeConsole` | 分离控制台 | ✅ 分离成功 |

## 🔧 核心 Win32 API

### AttachConsole
```c
BOOL AttachConsole(DWORD dwProcessId);
```
- 附加到指定 PID 的控制台
- 需要先调用 `FreeConsole()` 分离当前控制台
- 错误码 5 需要管理员权限

### WriteConsoleInput
```c
BOOL WriteConsoleInput(
    HANDLE hConsoleInput,
    INPUT_RECORD *lpBuffer,
    DWORD nLength,
    LPDWORD lpNumberOfEventsWritten
);
```
- 模拟键盘输入
- 每个字符需要 KeyDown + KeyUp 两个事件
- Enter 键 VK_RETURN = 0x0D

### ReadConsoleOutput
```c
BOOL ReadConsoleOutput(
    HANDLE hConsoleOutput,
    CHAR_INFO *lpBuffer,
    COORD dwBufferSize,
    COORD dwBufferCoord,
    SMALL_RECT *lpReadRegion
);
```
- 读取控制台缓冲区内容
- 需要先调用 `GetConsoleScreenBufferInfo` 获取窗口大小

## ⚠️ 已知限制

1. **权限要求**：AttachConsole 可能需要管理员权限
2. **进程要求**：目标进程必须拥有控制台窗口
3. **竞态条件**：Claude Code 内部处理可能有延迟
4. **GUI 程序**：无法读写 GUI 程序的控制台

## 🔍 调试技巧

### 查看进程命令行
```powershell
# 使用 WMIC
wmic process where "name='node.exe'" get processid,commandline

# 使用 PowerShell
Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match "claude" } | Format-List ProcessId, CommandLine
```

### 常见错误码
| 错误码 | 含义 | 解决方案 |
|--------|------|----------|
| 5 | 拒绝访问 | 以管理员身份运行 |
| 87 | 参数无效 | 检查 PID 是否正确 |
| 2 | 文件未找到 | 检查进程是否存在 |

## 📊 POC 结果记录

请在此记录你的测试结果：

- [ ] AttachConsole 成功
- [ ] WriteConsoleInput 成功（可在终端看到输入）
- [ ] ReadConsoleOutput 成功（获取到屏幕内容）
- [ ] FreeConsole 成功
- [ ] 整体方案可行

**测试环境**：
- Windows 版本：
- Claude Code 版本：
- 测试日期：

## 🔗 相关文档

- [Windows Console API](https://docs.microsoft.com/en-us/windows/console/)
- [INPUT_RECORD Structure](https://docs.microsoft.com/en-us/windows/console/input-record-str)
- [CHAR_INFO Structure](https://docs.microsoft.com/en-us/windows/console/char-info-str)

## 📝 License

MIT
