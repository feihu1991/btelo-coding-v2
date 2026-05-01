param(
    [int]$TargetPid,
    [string]$TextFile
)

$Text = Get-Content -Path $TextFile -Raw -Encoding UTF8
if (-not $Text) { Write-Error "No text"; exit 1 }

Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public class K {
    [DllImport("kernel32.dll")] public static extern bool AttachConsole(uint pid);
    [DllImport("kernel32.dll")] public static extern bool FreeConsole();
    [DllImport("kernel32.dll")] public static extern IntPtr GetConsoleWindow();
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int n);
    [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr h);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
    [DllImport("user32.dll")] public static extern bool AllowSetForegroundWindow(int pid);
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr h, out uint pid);
    [DllImport("user32.dll")] public static extern bool AttachThreadInput(uint idAttach, uint idAttachTo, bool fAttach);
    [DllImport("kernel32.dll")] public static extern uint GetCurrentThreadId();

    public static IntPtr FindConsole(uint pid) {
        FreeConsole();
        if (!AttachConsole(pid)) return IntPtr.Zero;
        IntPtr h = GetConsoleWindow();
        FreeConsole();
        return h;
    }

    public static bool ForceFocus(IntPtr hWnd) {
        // Get target window's thread
        uint targetPid;
        uint targetThread = GetWindowThreadProcessId(hWnd, out targetPid);
        uint myThread = GetCurrentThreadId();

        // Attach our thread to target's thread for input sharing
        AttachThreadInput(myThread, targetThread, true);

        // Allow our process to set foreground
        AllowSetForegroundWindow(-1); // ASFW_ANY

        // Ensure visible
        if (IsIconic(hWnd)) ShowWindow(hWnd, 9);
        ShowWindow(hWnd, 5);

        // Now we can set foreground
        bool result = SetForegroundWindow(hWnd);

        // Detach
        AttachThreadInput(myThread, targetThread, false);

        return result;
    }
}
'@

Add-Type -AssemblyName System.Windows.Forms

$hWnd = [K]::FindConsole($TargetPid)
if ($hWnd -eq [IntPtr]::Zero) {
    Write-Error "Cannot find console window for PID $TargetPid"
    exit 1
}

# Force focus using AttachThreadInput trick
[K]::ForceFocus($hWnd) | Out-Null
Start-Sleep -Milliseconds 100

# Send keystrokes
[System.Windows.Forms.SendKeys]::SendWait($Text + "{ENTER}")

Write-Output "OK"
