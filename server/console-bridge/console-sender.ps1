param(
    [int]$TargetPid,
    [string]$Text
)

$Win32Code = @'
using System;
using System.Runtime.InteropServices;

public class CS
{
    [DllImport("kernel32.dll")]
    public static extern bool AttachConsole(uint pid);
    [DllImport("kernel32.dll")]
    public static extern bool FreeConsole();
    [DllImport("kernel32.dll")]
    public static extern IntPtr GetStdHandle(uint nStdHandle);
    [DllImport("kernel32.dll")]
    public static extern bool WriteConsoleInputW(IntPtr hIn, INPUT_RECORD[] buf, uint len, out uint written);
    [DllImport("user32.dll")]
    public static extern uint MapVirtualKeyW(uint code, uint type);

    [StructLayout(LayoutKind.Sequential)]
    public struct INPUT_RECORD { public ushort EventType; public KEY_EVENT_RECORD KeyEvent; }

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

    public const uint STD_INPUT_HANDLE = 0xFFFFFFF6;
    public const ushort KEY_EVENT = 0x0001;
    public const ushort VK_RETURN = 0x0D;
    public const uint MAPVK_VK_TO_VSC = 0x00;

    public static bool SendText(string text)
    {
        IntPtr hIn = GetStdHandle(STD_INPUT_HANDLE);
        if (hIn == IntPtr.Zero || hIn == (IntPtr)(-1)) return false;

        foreach (char ch in text)
        {
            INPUT_RECORD[] rec = new INPUT_RECORD[1];
            rec[0].EventType = KEY_EVENT;
            rec[0].KeyEvent.bKeyDown = true;
            rec[0].KeyEvent.wRepeatCount = 1;

            char upper = Char.ToUpper(ch);
            if (upper >= 'A' && upper <= 'Z')
                rec[0].KeyEvent.wVirtualKeyCode = (ushort)upper;
            else
                rec[0].KeyEvent.wVirtualKeyCode = (ushort)ch;

            rec[0].KeyEvent.wVirtualScanCode = (ushort)MapVirtualKeyW(
                rec[0].KeyEvent.wVirtualKeyCode, MAPVK_VK_TO_VSC);
            rec[0].KeyEvent.UnicodeChar = ch;
            rec[0].KeyEvent.dwControlKeyState = 0;

            uint w; WriteConsoleInputW(hIn, rec, 1, out w);
            rec[0].KeyEvent.bKeyDown = false;
            WriteConsoleInputW(hIn, rec, 1, out w);
            System.Threading.Thread.Sleep(1);
        }
        return true;
    }

    public static bool SendEnter()
    {
        IntPtr hIn = GetStdHandle(STD_INPUT_HANDLE);
        if (hIn == IntPtr.Zero || hIn == (IntPtr)(-1)) return false;

        INPUT_RECORD[] rec = new INPUT_RECORD[1];
        rec[0].EventType = KEY_EVENT;
        rec[0].KeyEvent.bKeyDown = true;
        rec[0].KeyEvent.wRepeatCount = 1;
        rec[0].KeyEvent.wVirtualKeyCode = VK_RETURN;
        rec[0].KeyEvent.wVirtualScanCode = (ushort)MapVirtualKeyW(VK_RETURN, MAPVK_VK_TO_VSC);
        rec[0].KeyEvent.UnicodeChar = '\r';
        rec[0].KeyEvent.dwControlKeyState = 0;

        uint w; WriteConsoleInputW(hIn, rec, 1, out w);
        rec[0].KeyEvent.bKeyDown = false;
        WriteConsoleInputW(hIn, rec, 1, out w);
        return true;
    }
}
'@

Add-Type -TypeDefinition $Win32Code

[CS]::FreeConsole() | Out-Null

if (-not [CS]::AttachConsole($TargetPid)) {
    Write-Error "AttachConsole failed. Run as Administrator."
    exit 1
}

[CS]::SendText($Text) | Out-Null
[CS]::SendEnter() | Out-Null
[CS]::FreeConsole() | Out-Null
Write-Output "OK"
