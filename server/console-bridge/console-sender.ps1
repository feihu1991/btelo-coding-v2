param(
    [int]$TargetPid,
    [string]$Text
)

Add-Type -AssemblyName System.Windows.Forms
Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
using System.Text;

public class CS
{
    [DllImport("kernel32.dll")]
    public static extern bool AttachConsole(uint pid);
    [DllImport("kernel32.dll")]
    public static extern bool FreeConsole();
    [DllImport("kernel32.dll")]
    public static extern IntPtr GetConsoleWindow();
    [DllImport("user32.dll")]
    public static extern int GetWindowText(IntPtr h, StringBuilder s, int n);
    [DllImport("user32.dll")]
    public static extern uint GetWindowThreadProcessId(IntPtr h, out uint pid);
    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")]
    public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
    [DllImport("user32.dll")]
    public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")]
    public static extern bool PostMessageW(IntPtr hWnd, uint Msg, IntPtr wParam, IntPtr lParam);
    [DllImport("user32.dll")]
    public static extern IntPtr FindWindowEx(IntPtr parent, IntPtr child, string cls, string name);
    [DllImport("user32.dll")]
    public static extern bool EnumWindows(EnumWindowsProc lpEnumFunc, IntPtr lParam);
    public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);

    public const uint WM_KEYDOWN = 0x0100;
    public const uint WM_KEYUP = 0x0101;
    public const uint WM_CHAR = 0x0102;
    public const ushort VK_CONTROL = 0x11;
    public const ushort VK_V = 0x56;
    public const ushort VK_RETURN = 0x0D;
    public const int SW_SHOW = 5;
    public const int SW_RESTORE = 9;

    public static IntPtr FindConsoleWindowForPid(uint targetPid)
    {
        CS.FreeConsole();
        if (CS.AttachConsole(targetPid))
        {
            IntPtr h = GetConsoleWindow();
            FreeConsole();
            if (h != IntPtr.Zero) return h;
        }
        return IntPtr.Zero;
    }

    public static bool PasteToWindow(IntPtr hWnd)
    {
        // Bring window to foreground
        ShowWindow(hWnd, SW_RESTORE);
        SetForegroundWindow(hWnd);
        System.Threading.Thread.Sleep(100);

        // Send Ctrl+V (paste)
        // Ctrl key down
        IntPtr lParamCtrl = (IntPtr)((1 << 0) | (0x1D << 16) | (0 << 29) | (0 << 30) | (0 << 31));
        PostMessageW(hWnd, WM_KEYDOWN, (IntPtr)VK_CONTROL, lParamCtrl);
        System.Threading.Thread.Sleep(10);

        // V key
        IntPtr lParamV = (IntPtr)((1 << 0) | (0x2F << 16) | (0 << 29) | (0 << 30) | (0 << 31));
        PostMessageW(hWnd, WM_KEYDOWN, (IntPtr)VK_V, lParamV);
        System.Threading.Thread.Sleep(10);
        PostMessageW(hWnd, WM_KEYUP, (IntPtr)VK_V, lParamV);

        // Ctrl key up
        PostMessageW(hWnd, WM_KEYUP, (IntPtr)VK_CONTROL, lParamCtrl);

        return true;
    }

    public static bool SendEnter(IntPtr hWnd)
    {
        IntPtr lParam = (IntPtr)((1 << 0) | (0x1C << 16) | (0 << 29) | (0 << 30) | (0 << 31));
        PostMessageW(hWnd, WM_KEYDOWN, (IntPtr)VK_RETURN, lParam);
        System.Threading.Thread.Sleep(10);
        PostMessageW(hWnd, WM_KEYUP, (IntPtr)VK_RETURN, lParam);
        return true;
    }
}
'@

# Step 1: Copy text to clipboard
[System.Windows.Forms.Clipboard]::SetText($Text)

# Step 2: Find console window
$consoleWnd = [CS]::FindConsoleWindowForPid($TargetPid)
if ($consoleWnd -eq [IntPtr]::Zero) {
    # Try parent PID
    try {
        $parent = (Get-CimInstance Win32_Process -Filter "ProcessId=$TargetPid").ParentProcessId
        if ($parent) {
            $consoleWnd = [CS]::FindConsoleWindowForPid($parent)
        }
    } catch {}
}

if ($consoleWnd -eq [IntPtr]::Zero) {
    Write-Error "Cannot find console window for PID $TargetPid"
    exit 1
}

$sb = New-Object System.Text.StringBuilder(256)
[CS]::GetWindowText($consoleWnd, $sb, 256)
Write-Output "ConsoleWindow: $consoleWnd Title: '$($sb.ToString())'"

# Step 3: Paste into window and press Enter
[CS]::PasteToWindow($consoleWnd)
Start-Sleep -Milliseconds 100
[CS]::SendEnter($consoleWnd)

# Step 4: Restore original foreground window
# (no-op - we just paste and leave)

Write-Output "OK:Clipboard"
