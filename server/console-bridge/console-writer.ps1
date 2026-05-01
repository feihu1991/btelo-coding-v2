param(
    [int]$TargetPid,
    [string]$TextFile
)

$Text = Get-Content -Path $TextFile -Raw -Encoding UTF8
if (-not $Text) { Write-Error "No text"; exit 1 }

# Write directly to the target console input buffer without stealing focus.
# This is more stable than System.Windows.Forms.SendKeys, but it still requires
# the target process to own/attach to a classic Windows console input buffer.
Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;

public class ConsoleInputWriter {
    public const int STD_INPUT_HANDLE = -10;
    public const ushort KEY_EVENT = 0x0001;
    public const ushort VK_RETURN = 0x0D;

    [DllImport("kernel32.dll", SetLastError=true)]
    public static extern bool AttachConsole(uint dwProcessId);

    [DllImport("kernel32.dll", SetLastError=true)]
    public static extern bool FreeConsole();

    [DllImport("kernel32.dll", SetLastError=true)]
    public static extern IntPtr GetStdHandle(int nStdHandle);

    [DllImport("kernel32.dll", SetLastError=true, CharSet=CharSet.Unicode)]
    public static extern bool WriteConsoleInputW(
        IntPtr hConsoleInput,
        INPUT_RECORD[] lpBuffer,
        uint nLength,
        out uint lpNumberOfEventsWritten
    );

    [StructLayout(LayoutKind.Explicit, CharSet=CharSet.Unicode)]
    public struct INPUT_RECORD {
        [FieldOffset(0)] public ushort EventType;
        [FieldOffset(4)] public KEY_EVENT_RECORD KeyEvent;
    }

    [StructLayout(LayoutKind.Sequential, CharSet=CharSet.Unicode)]
    public struct KEY_EVENT_RECORD {
        [MarshalAs(UnmanagedType.Bool)] public bool bKeyDown;
        public ushort wRepeatCount;
        public ushort wVirtualKeyCode;
        public ushort wVirtualScanCode;
        public char UnicodeChar;
        public uint dwControlKeyState;
    }

    public static INPUT_RECORD Key(char ch, bool down, ushort vk) {
        INPUT_RECORD record = new INPUT_RECORD();
        record.EventType = KEY_EVENT;
        record.KeyEvent.bKeyDown = down;
        record.KeyEvent.wRepeatCount = 1;
        record.KeyEvent.wVirtualKeyCode = vk;
        record.KeyEvent.wVirtualScanCode = 0;
        record.KeyEvent.UnicodeChar = ch;
        record.KeyEvent.dwControlKeyState = 0;
        return record;
    }

    public static void WriteText(uint pid, string text) {
        FreeConsole();
        if (!AttachConsole(pid)) {
            throw new System.ComponentModel.Win32Exception(Marshal.GetLastWin32Error(), "AttachConsole failed");
        }

        IntPtr input = GetStdHandle(STD_INPUT_HANDLE);
        if (input == IntPtr.Zero || input == new IntPtr(-1)) {
            int error = Marshal.GetLastWin32Error();
            FreeConsole();
            throw new System.ComponentModel.Win32Exception(error, "GetStdHandle(STD_INPUT_HANDLE) failed");
        }

        System.Collections.Generic.List<INPUT_RECORD> records = new System.Collections.Generic.List<INPUT_RECORD>();
        foreach (char ch in text) {
            records.Add(Key(ch, true, 0));
            records.Add(Key(ch, false, 0));
        }
        records.Add(Key('\r', true, VK_RETURN));
        records.Add(Key('\r', false, VK_RETURN));

        INPUT_RECORD[] arr = records.ToArray();
        uint written;
        bool ok = WriteConsoleInputW(input, arr, (uint)arr.Length, out written);
        int lastError = Marshal.GetLastWin32Error();
        FreeConsole();

        if (!ok || written != arr.Length) {
            throw new System.ComponentModel.Win32Exception(lastError, "WriteConsoleInputW failed or wrote partial input");
        }
    }
}
'@

try {
    [ConsoleInputWriter]::WriteText([uint32]$TargetPid, $Text)
    Write-Output "OK"
    exit 0
} catch {
    Write-Error $_.Exception.Message
    exit 1
}
