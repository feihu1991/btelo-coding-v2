#Requires -Version 5.0
<#
.SYNOPSIS
    BTELO Console Sender - 通过 Win32 Console API 向指定进程的控制台发送按键
.DESCRIPTION
    使用 PowerShell Add-Type 内联 C# 调用 Win32 Console API，
    直接向 Claude Code 进程的控制台写入键盘输入。
.PARAMETER Pid
    目标进程 PID
.PARAMETER Text
    要发送的文本
.EXAMPLE
    powershell -ExecutionPolicy Bypass -File console-sender.ps1 -Pid 12345 -Text "hello"
#>

param(
    [Parameter(Mandatory=$true)]
    [int]$Pid,
    [Parameter(Mandatory=$true)]
    [string]$Text
)

# Win32 API 定义（内联 C#）
$Win32Code = @'
using System;
using System.Runtime.InteropServices;

public class ConsoleSender
{
    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool AttachConsole(uint dwProcessId);

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool FreeConsole();

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern IntPtr GetStdHandle(uint nStdHandle);

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool WriteConsoleInputW(
        IntPtr hConsoleInput,
        INPUT_RECORD[] lpBuffer,
        uint nLength,
        out uint lpNumberOfEventsWritten
    );

    [DllImport("user32.dll")]
    public static extern uint MapVirtualKeyW(uint uCode, uint uMapType);

    [StructLayout(LayoutKind.Sequential)]
    public struct INPUT_RECORD
    {
        public ushort EventType;
        public KEY_EVENT_RECORD KeyEvent;
    }

    [StructLayout(LayoutKind.Explicit)]
    public struct KEY_EVENT_RECORD
    {
        [FieldOffset(0)] public bool bKeyDown;
        [FieldOffset(4)] public ushort wRepeatCount;
        [FieldOffset(8)] public ushort wVirtualKeyCode;
        [FieldOffset(10)] public ushort wVirtualScanCode;
        [FieldOffset(12)] public char UnicodeChar;
        [FieldOffset(16)] public uint dwControlKeyState;
    }

    public const uint STD_INPUT_HANDLE = unchecked((uint)-10);
    public const ushort KEY_EVENT = 0x0001;
    public const ushort VK_RETURN = 0x0D;
    public const uint MAPVK_VK_TO_VSC = 0x00;

    public static bool SendText(string text)
    {
        IntPtr hInput = GetStdHandle(STD_INPUT_HANDLE);
        if (hInput == IntPtr.Zero || hInput == new IntPtr(-1))
            return false;

        foreach (char ch in text)
        {
            // KeyDown
            INPUT_RECORD[] records = new INPUT_RECORD[1];
            records[0].EventType = KEY_EVENT;
            records[0].KeyEvent.bKeyDown = true;
            records[0].KeyEvent.wRepeatCount = 1;

            char upper = Char.ToUpper(ch);
            if (upper >= 'A' && upper <= 'Z')
                records[0].KeyEvent.wVirtualKeyCode = (ushort)upper;
            else
                records[0].KeyEvent.wVirtualKeyCode = (ushort)ch;

            records[0].KeyEvent.wVirtualScanCode = (ushort)MapVirtualKeyW(
                records[0].KeyEvent.wVirtualKeyCode, MAPVK_VK_TO_VSC);
            records[0].KeyEvent.UnicodeChar = ch;
            records[0].KeyEvent.dwControlKeyState = 0;

            uint written;
            WriteConsoleInputW(hInput, records, 1, out written);

            // KeyUp
            records[0].KeyEvent.bKeyDown = false;
            WriteConsoleInputW(hInput, records, 1, out written);

            System.Threading.Thread.Sleep(1);
        }
        return true;
    }

    public static bool SendEnter()
    {
        IntPtr hInput = GetStdHandle(STD_INPUT_HANDLE);
        if (hInput == IntPtr.Zero || hInput == new IntPtr(-1))
            return false;

        INPUT_RECORD[] records = new INPUT_RECORD[1];
        records[0].EventType = KEY_EVENT;

        // KeyDown
        records[0].KeyEvent.bKeyDown = true;
        records[0].KeyEvent.wRepeatCount = 1;
        records[0].KeyEvent.wVirtualKeyCode = VK_RETURN;
        records[0].KeyEvent.wVirtualScanCode = (ushort)MapVirtualKeyW(VK_RETURN, MAPVK_VK_TO_VSC);
        records[0].KeyEvent.UnicodeChar = '\r';
        records[0].KeyEvent.dwControlKeyState = 0;

        uint written;
        WriteConsoleInputW(hInput, records, 1, out written);

        // KeyUp
        records[0].KeyEvent.bKeyDown = false;
        WriteConsoleInputW(hInput, records, 1, out written);

        return true;
    }
}
'@

# 编译 C# 代码
Add-Type -TypeDefinition $Win32Code -ErrorAction Stop

# 分离当前控制台（如果有的话）
[ConsoleSender]::FreeConsole() | Out-Null

# 附加到目标进程
$success = [ConsoleSender]::AttachConsole($Pid)
if (-not $success) {
    $err = [System.Runtime.InteropServices.Marshal]::GetLastWin32Error()
    Write-Error "AttachConsole failed: error code $err. Run as Administrator?"
    exit 1
}

# 发送文本
[ConsoleSender]::SendText($Text) | Out-Null

# 发送 Enter 键换行
[ConsoleSender]::SendEnter() | Out-Null

# 分离
[ConsoleSender]::FreeConsole() | Out-Null

Write-Output "OK"
