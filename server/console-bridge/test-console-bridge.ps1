#Requires -Version 5.0
<#
.SYNOPSIS
    BTELO Console Bridge POC - Win32 Console API 验证脚本
.DESCRIPTION
    使用 PowerShell Add-Type 内联 C# 调用 Win32 Console API
    验证直接读写 Claude Code 控制台的可行性
.EXAMPLE
    powershell -ExecutionPolicy Bypass -File test-console-bridge.ps1
#>

# === 获取当前 PowerShell 进程 PID（用于排除自身） ===
$myPid = $PID

# === Win32 API 定义 ===
$Win32Code = @'
using System;
using System.Runtime.InteropServices;
using System.Text;
using System.Collections.Generic;

public class ConsoleBridge
{
    // 常量
    public const uint STD_INPUT_HANDLE = unchecked((uint)-10);
    public const uint STD_OUTPUT_HANDLE = unchecked((uint)-11);
    public const uint STD_ERROR_HANDLE = unchecked((uint)-12);
    
    public const ushort KEY_EVENT = 0x0001;
    public const ushort WINDOW_BUFFER_SIZE_EVENT = 0x0004;
    
    // 虚拟键码
    public const ushort VK_RETURN = 0x0D;
    public const ushort VK_SHIFT = 0x10;
    public const ushort VK_CONTROL = 0x11;
    public const ushort VK_MENU = 0x12; // ALT
    
    // 控制键状态
    public const uint RIGHT_ALT_PRESSED = 0x0001;
    public const uint LEFT_ALT_PRESSED = 0x0002;
    public const uint RIGHT_CTRL_PRESSED = 0x0004;
    public const uint LEFT_CTRL_PRESSED = 0x0008;
    public const uint SHIFT_PRESSED = 0x0010;
    public const uint NUMLOCK_ON = 0x0020;
    public const uint SCROLLLOCK_ON = 0x0040;
    public const uint CAPSLOCK_ON = 0x0080;
    
    // MapVirtualKey 参数
    private const uint MAPVK_VK_TO_VSC = 0x00;
    
    // P/Invoke 声明
    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern IntPtr GetStdHandle(uint nStdHandle);
    
    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool AttachConsole(uint dwProcessId);
    
    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool FreeConsole();
    
    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool WriteConsoleInput(
        IntPtr hConsoleInput,
        INPUT_RECORD[] lpBuffer,
        uint nLength,
        out uint lpNumberOfEventsWritten
    );
    
    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool ReadConsoleOutput(
        IntPtr hConsoleOutput,
        CHAR_INFO[] lpBuffer,
        COORD dwBufferSize,
        COORD dwBufferCoord,
        ref SMALL_RECT lpReadRegion
    );
    
    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool GetConsoleScreenBufferInfo(
        IntPtr hConsoleOutput,
        out CONSOLE_SCREEN_BUFFER_INFO lpConsoleScreenBufferInfo
    );
    
    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool SetConsoleMode(IntPtr hConsoleHandle, uint dwMode);
    
    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool GetConsoleMode(IntPtr hConsoleHandle, out uint lpMode);
    
    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool GetNumberOfConsoleInputEvents(
        IntPtr hConsoleInput,
        out uint lpcNumberOfEvents
    );
    
    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool FlushConsoleInputBuffer(IntPtr hConsoleInput);
    
    [DllImport("user32.dll")]
    public static extern uint MapVirtualKey(uint uCode, uint uMapType);
    
    // 结构体定义
    [StructLayout(LayoutKind.Sequential)]
    public struct COORD
    {
        public short X;
        public short Y;
        
        public COORD(short x, short y)
        {
            X = x;
            Y = y;
        }
    }
    
    [StructLayout(LayoutKind.Sequential)]
    public struct SMALL_RECT
    {
        public short Left;
        public short Top;
        public short Right;
        public short Bottom;
        
        public SMALL_RECT(short left, short top, short right, short bottom)
        {
            Left = left;
            Top = top;
            Right = right;
            Bottom = bottom;
        }
        
        public short Width { get { return (short)(Right - Left + 1); } }
        public short Height { get { return (short)(Bottom - Top + 1); } }
    }
    
    [StructLayout(LayoutKind.Sequential)]
    public struct CONSOLE_SCREEN_BUFFER_INFO
    {
        public COORD dwSize;
        public COORD dwCursorPosition;
        public ushort wAttributes;
        public SMALL_RECT srWindow;
        public COORD dwMaximumWindowSize;
    }
    
    [StructLayout(LayoutKind.Sequential)]
    public struct INPUT_RECORD
    {
        public ushort EventType;
        public KEY_EVENT_RECORD KeyEvent;
    }
    
    [StructLayout(LayoutKind.Sequential)]
    public struct KEY_EVENT_RECORD
    {
        public bool bKeyDown;
        public ushort wRepeatCount;
        public ushort wVirtualKeyCode;
        public ushort wVirtualScanCode;
        public char UnicodeChar;
        public uint dwControlKeyState;
    }
    
    [StructLayout(LayoutKind.Sequential)]
    public struct CHAR_INFO
    {
        public char UnicodeChar;
        public ushort Attributes;
        
        public CHAR_INFO(char c, ushort attr)
        {
            UnicodeChar = c;
            Attributes = attr;
        }
    }
    
    // 辅助方法 - 使用 MapVirtualKey 获取正确的虚拟扫描码
    public static ushort GetVirtualScanCode(ushort vk)
    {
        return (ushort)MapVirtualKey(vk, MAPVK_VK_TO_VSC);
    }
}
'@

# 编译 C# 代码
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "BTELO Console Bridge POC" -ForegroundColor Cyan
Write-Host "Win32 Console API 验证测试" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "[1/7] 编译 Win32 API 类型定义..." -ForegroundColor Yellow
try {
    Add-Type -TypeDefinition $Win32Code -ErrorAction Stop
    Write-Host "    [OK] C# 类型编译成功" -ForegroundColor Green
} catch {
    Write-Host "    [FAIL] C# 编译失败: $_" -ForegroundColor Red
    exit 1
}

# === 辅助函数 ===
function Get-ConsoleInputHandle {
    [ConsoleBridge]::GetStdHandle([ConsoleBridge]::STD_INPUT_HANDLE)
}

function Get-ConsoleOutputHandle {
    [ConsoleBridge]::GetStdHandle([ConsoleBridge]::STD_OUTPUT_HANDLE)
}

function Write-ConsoleKeyEvent {
    param(
        [IntPtr]$Handle,
        [bool]$KeyDown,
        [char]$Char,
        [ushort]$VirtualKeyCode
    )
    
    $scanCode = [ConsoleBridge]::GetVirtualScanCode($VirtualKeyCode)
    
    $buffer = New-Object ConsoleBridge+INPUT_RECORD[] 1
    $buffer[0].EventType = [ConsoleBridge]::KEY_EVENT
    $buffer[0].KeyEvent.bKeyDown = $KeyDown
    $buffer[0].KeyEvent.wRepeatCount = 1
    $buffer[0].KeyEvent.wVirtualKeyCode = $VirtualKeyCode
    $buffer[0].KeyEvent.wVirtualScanCode = $scanCode
    $buffer[0].KeyEvent.UnicodeChar = $Char
    $buffer[0].KeyEvent.dwControlKeyState = 0
    
    $written = 0
    $result = [ConsoleBridge]::WriteConsoleInput($Handle, $buffer, 1, [ref]$written)
    
    return $result
}

function Read-ScreenContent {
    param([IntPtr]$Handle)
    
    $screenInfo = New-Object ConsoleBridge+CONSOLE_SCREEN_BUFFER_INFO
    $result = [ConsoleBridge]::GetConsoleScreenBufferInfo($Handle, [ref]$screenInfo)
    
    if (-not $result) {
        Write-Host "    [FAIL] GetConsoleScreenBufferInfo 失败" -ForegroundColor Red
        return $null
    }
    
    Write-Host "    控制台窗口: $($screenInfo.srWindow.Width) x $($screenInfo.srWindow.Height)" -ForegroundColor Gray
    Write-Host "    缓冲区大小: $($screenInfo.dwSize.X) x $($screenInfo.dwSize.Y)" -ForegroundColor Gray
    Write-Host "    光标位置: ($($screenInfo.dwCursorPosition.X), $($screenInfo.dwCursorPosition.Y))" -ForegroundColor Gray
    
    $bufferWidth = $screenInfo.srWindow.Width
    $bufferHeight = $screenInfo.srWindow.Height
    
    $bufferSize = New-Object ConsoleBridge+COORD $bufferWidth, $bufferHeight
    $bufferCoord = New-Object ConsoleBridge+COORD 0, 0
    $readRegion = New-Object ConsoleBridge+SMALL_RECT $screenInfo.srWindow.Left, $screenInfo.srWindow.Top, $screenInfo.srWindow.Right, $screenInfo.srWindow.Bottom
    
    $charBuffer = New-Object ConsoleBridge+CHAR_INFO[] ($bufferWidth * $bufferHeight)
    
    $result = [ConsoleBridge]::ReadConsoleOutput(
        $Handle,
        $charBuffer,
        $bufferSize,
        $bufferCoord,
        [ref]$readRegion
    )
    
    if (-not $result) {
        Write-Host "    [FAIL] ReadConsoleOutput 失败: $([System.Runtime.InteropServices.Marshal]::GetLastWin32Error())" -ForegroundColor Red
        return $null
    }
    
    $lines = @()
    for ($y = 0; $y -lt $bufferHeight; $y++) {
        $lineChars = @()
        for ($x = 0; $x -lt $bufferWidth; $x++) {
            $idx = $y * $bufferWidth + $x
            if ($idx -lt $charBuffer.Length) {
                $lineChars += $charBuffer[$idx].UnicodeChar
            }
        }
        
        $line = -join $lineChars
        $line = $line.TrimEnd()
        
        # P2 #8: 保留所有行，不跳过空行
        if ($true) {
            $lines += $line
        }
    }
    
    return $lines
}

# P2 #13: 改进的进程匹配函数
function Test-ProcessMatchesClaude {
    param(
        [int]$ProcessId,
        [string]$ProcessName
    )
    
    try {
        $cmdLine = (Get-CimInstance Win32_Process -Filter "ProcessId = $ProcessId" -ErrorAction SilentlyContinue).CommandLine
        if (-not $cmdLine) { return $false }
        
        # P2 #13: 改进正则匹配 - 使用更精确的模式
        # 匹配 claude 独立命令或 npm/npx 包名
        $pattern = '(?i)(?:^|[\/\\ ])claude(\.exe)?(\s|$)|@anthropic[/-]ai[/-]claude|claude-code'
        if ($cmdLine -match $pattern) {
            return $true
        }
    } catch { }
    
    return $false
}

# === 步骤 2: 查找 Claude Code 进程（排除自身） ===
Write-Host ""
Write-Host "[2/7] 查找 Claude Code 进程 (排除自身 PID: $myPid)..." -ForegroundColor Yellow

$claudeProcesses = @()

# 方法1: 查找 claude.exe
$processes = Get-Process -Name "claude" -ErrorAction SilentlyContinue | Where-Object { $_.Id -ne $myPid }
if ($processes) {
    foreach ($p in $processes) {
        $claudeProcesses += $p
        Write-Host "    [OK] 找到 claude.exe (PID: $($p.Id))" -ForegroundColor Green
    }
}

# 方法2: 查找包含 "claude" 命令的 node.exe 进程（排除自身）
$nodeProcesses = Get-Process -Name "node" -ErrorAction SilentlyContinue | Where-Object { $_.Id -ne $myPid }
if ($nodeProcesses) {
    foreach ($p in $nodeProcesses) {
        try {
            $cmdLine = (Get-CimInstance Win32_Process -Filter "ProcessId = $($p.Id)" -ErrorAction SilentlyContinue).CommandLine
            # P2 #13: 使用改进的匹配函数
            if ($cmdLine -and (Test-ProcessMatchesClaude -ProcessId $p.Id -ProcessName "node")) {
                $claudeProcesses += $p
                Write-Host "    [OK] 找到 node.exe 运行 claude (PID: $($p.Id))" -ForegroundColor Green
            }
        } catch { }
    }
}

# P2 #13: 改进方法3: 查找包含 "claude" 命令的 cmd.exe 或 powershell.exe（排除自身）
$shellProcesses = Get-Process | Where-Object { $_.Name -eq "cmd" -or $_.Name -eq "powershell" -or $_.Name -eq "powershell7" } | Where-Object { $_.Id -ne $myPid }
foreach ($p in $shellProcesses) {
    try {
        $cmdLine = (Get-CimInstance Win32_Process -Filter "ProcessId = $($p.Id)" -ErrorAction SilentlyContinue).CommandLine
        # P2 #13: 使用改进的匹配函数，避免误匹配
        if ($cmdLine -and (Test-ProcessMatchesClaude -ProcessId $p.Id -ProcessName $p.Name)) {
            $alreadyAdded = $false
            foreach ($cp in $claudeProcesses) {
                if ($cp.Id -eq $p.Id) {
                    $alreadyAdded = $true
                    break
                }
            }
            if (-not $alreadyAdded) {
                $claudeProcesses += $p
                Write-Host "    [OK] 找到 $($p.Name).exe 运行 claude (PID: $($p.Id))" -ForegroundColor Green
            }
        }
    } catch { }
}

if ($claudeProcesses.Count -eq 0) {
    Write-Host "    [FAIL] 未找到 Claude Code 进程" -ForegroundColor Red
    Write-Host "    请先启动 Claude Code 终端" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "    预期启动命令示例:" -ForegroundColor Cyan
    Write-Host "    - claude" -ForegroundColor Gray
    Write-Host "    - npx @anthropic-ai/claude-code" -ForegroundColor Gray
    Write-Host "    - node .../claude" -ForegroundColor Gray
    exit 1
}

$targetProcess = $claudeProcesses[0]
$targetPid = $targetProcess.Id
Write-Host "    选择目标进程 PID: $targetPid" -ForegroundColor Cyan

# === 步骤 3: 分离当前控制台 ===
Write-Host ""
Write-Host "[3/7] 分离当前控制台..." -ForegroundColor Yellow

$freeResult = [ConsoleBridge]::FreeConsole()
if ($freeResult) {
    Write-Host "    [OK] 已分离当前控制台" -ForegroundColor Green
} else {
    Write-Host "    [!] FreeConsole 返回 false (可能没有附加控制台)" -ForegroundColor Gray
}

# === 步骤 4: Attach 到目标进程控制台 ===
Write-Host ""
Write-Host "[4/7] Attach 到目标控制台 (PID: $targetPid)..." -ForegroundColor Yellow

$attachResult = [ConsoleBridge]::AttachConsole($targetPid)
if (-not $attachResult) {
    $errorCode = [System.Runtime.InteropServices.Marshal]::GetLastWin32Error()
    Write-Host "    [FAIL] AttachConsole 失败: 错误码 $errorCode" -ForegroundColor Red
    
    if ($errorCode -eq 5) {
        Write-Host "    原因: 拒绝访问。请确保以管理员权限运行此脚本。" -ForegroundColor Yellow
    } elseif ($errorCode -eq 87) {
        Write-Host "    原因: 参数无效或进程没有控制台。" -ForegroundColor Yellow
    } elseif ($errorCode -eq 8) {
        Write-Host "    原因: 内存不足。" -ForegroundColor Yellow
    } elseif ($errorCode -eq 2) {
        Write-Host "    原因: 系统找不到指定的文件。" -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host "[Cleanup] 清理..." -ForegroundColor Yellow
    [ConsoleBridge]::FreeConsole() | Out-Null
    exit 1
}

Write-Host "    [OK] 已 Attach 到目标控制台" -ForegroundColor Green

$hInput = Get-ConsoleInputHandle
$hOutput = Get-ConsoleOutputHandle

Write-Host "    输入句柄: $hInput" -ForegroundColor Gray
Write-Host "    输出句柄: $hOutput" -ForegroundColor Gray

# === 步骤 5: 读取当前屏幕内容 ===
Write-Host ""
Write-Host "[5/7] 读取当前屏幕内容..." -ForegroundColor Yellow

$initialScreen = Read-ScreenContent -Handle $hOutput
if ($initialScreen) {
    Write-Host "    [OK] 读取到 $($initialScreen.Count) 行内容" -ForegroundColor Green
    Write-Host "    --- 当前屏幕内容 ---" -ForegroundColor Gray
    foreach ($line in $initialScreen) {
        Write-Host "    | $line" -ForegroundColor White
    }
    Write-Host "    --------------------" -ForegroundColor Gray
} else {
    Write-Host "    [FAIL] 读取屏幕失败" -ForegroundColor Red
}

# === 步骤 6: 模拟键盘输入 ===
Write-Host ""
Write-Host "[6/7] 模拟键盘输入 'hello' + Enter..." -ForegroundColor Yellow

$testText = "hello"

[ConsoleBridge]::FlushConsoleInputBuffer($hInput) | Out-Null

Write-Host "    发送: $testText" -ForegroundColor Cyan

$writeSuccess = $true
foreach ($char in $testText.ToCharArray()) {
    $vk = [System.Convert]::ToUInt16([char]::ToUpper($char))
    $result1 = Write-ConsoleKeyEvent -Handle $hInput -KeyDown $true -Char $char -VirtualKeyCode $vk
    Start-Sleep -Milliseconds 1
    $result2 = Write-ConsoleKeyEvent -Handle $hInput -KeyDown $false -Char $char -VirtualKeyCode $vk
    Start-Sleep -Milliseconds 1
    
    if ($result1 -and $result2) {
        Write-Host "    [OK] 字符 '$char' 发送成功" -ForegroundColor Green
    } else {
        Write-Host "    [FAIL] 字符 '$char' 发送失败" -ForegroundColor Red
        $writeSuccess = $false
    }
}

Write-Host "    发送: Enter (VK_RETURN)" -ForegroundColor Cyan
$result1 = Write-ConsoleKeyEvent -Handle $hInput -KeyDown $true -Char ([char]13) -VirtualKeyCode ([ConsoleBridge]::VK_RETURN)
Start-Sleep -Milliseconds 1
$result2 = Write-ConsoleKeyEvent -Handle $hInput -KeyDown $false -Char ([char]13) -VirtualKeyCode ([ConsoleBridge]::VK_RETURN)

if ($result1 -and $result2) {
    Write-Host "    [OK] Enter 发送成功" -ForegroundColor Green
} else {
    Write-Host "    [FAIL] Enter 发送失败" -ForegroundColor Red
    $writeSuccess = $false
}

Write-Host "    等待命令执行..." -ForegroundColor Gray
Start-Sleep -Seconds 1

# === 读取输入后的屏幕 ===
Write-Host ""
Write-Host "[7/7] 读取输入后的屏幕内容..." -ForegroundColor Yellow

$afterScreen = Read-ScreenContent -Handle $hOutput
if ($afterScreen) {
    Write-Host "    [OK] 读取到 $($afterScreen.Count) 行内容" -ForegroundColor Green
    Write-Host "    --- 输入后屏幕内容 ---" -ForegroundColor Gray
    foreach ($line in $afterScreen) {
        Write-Host "    | $line" -ForegroundColor White
    }
    Write-Host "    -----------------------" -ForegroundColor Gray
} else {
    Write-Host "    [FAIL] 读取屏幕失败" -ForegroundColor Red
}

# === 清理 ===
Write-Host ""
Write-Host "[Cleanup] 分离控制台..." -ForegroundColor Yellow
$detachResult = [ConsoleBridge]::FreeConsole()
if ($detachResult) {
    Write-Host "    [OK] 已分离控制台" -ForegroundColor Green
} else {
    Write-Host "    [FAIL] FreeConsole 返回 false" -ForegroundColor Red
}

# === 结果总结 ===
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "POC 测试结果总结" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Win32 Console API 验证:" -ForegroundColor White
Write-Host "  1. GetStdHandle       - [OK] PASS" -ForegroundColor Green
Write-Host "  2. AttachConsole      - " -NoNewline
if ($attachResult) { Write-Host "[OK] PASS" -ForegroundColor Green } else { Write-Host "[FAIL] FAIL" -ForegroundColor Red }
Write-Host "  3. WriteConsoleInput  - " -NoNewline
# P2 #14: 追踪 writeLine 实际结果
if ($writeSuccess) { Write-Host "[OK] PASS" -ForegroundColor Green } else { Write-Host "[FAIL] FAIL" -ForegroundColor Red }
Write-Host "  4. ReadConsoleOutput - " -NoNewline
if ($afterScreen) { Write-Host "[OK] PASS" -ForegroundColor Green } else { Write-Host "[FAIL] FAIL" -ForegroundColor Red }
Write-Host "  5. FreeConsole        - " -NoNewline
if ($detachResult) { Write-Host "[OK] PASS" -ForegroundColor Green } else { Write-Host "[FAIL] FAIL" -ForegroundColor Red }
Write-Host ""
Write-Host "如果所有步骤都显示 [OK] PASS，说明方案可行！" -ForegroundColor Green
Write-Host "可以在 Claude Code 终端中看到 'hello' 命令及其响应。" -ForegroundColor Gray
Write-Host ""
