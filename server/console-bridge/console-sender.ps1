param(
    [int]$TargetPid,
    [string]$TextFile
)

# Read text from file (avoids CLI encoding issues with Unicode)
$Text = Get-Content -Path $TextFile -Raw -Encoding UTF8

if (-not $Text) {
    Write-Error "No text to send"
    exit 1
}

Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;

public class K
{
    [DllImport("kernel32.dll")] public static extern bool AttachConsole(uint pid);
    [DllImport("kernel32.dll")] public static extern bool FreeConsole();
    [DllImport("kernel32.dll")] public static extern IntPtr GetConsoleWindow();
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int n);
    [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr h);

    public static IntPtr FindConsole(uint pid)
    {
        FreeConsole();
        if (!AttachConsole(pid)) return IntPtr.Zero;
        IntPtr h = GetConsoleWindow();
        FreeConsole();
        return h;
    }

    public static void Focus(IntPtr hWnd)
    {
        if (IsIconic(hWnd)) ShowWindow(hWnd, 9);
        ShowWindow(hWnd, 5);
        SetForegroundWindow(hWnd);
    }
}
'@

Add-Type -AssemblyName System.Windows.Forms

$hWnd = [K]::FindConsole($TargetPid)
if ($hWnd -eq [IntPtr]::Zero) {
    Write-Error "Cannot find console window for PID $TargetPid"
    exit 1
}

[K]::Focus($hWnd)
Start-Sleep -Milliseconds 50

# SendKeys via SendInput API
[System.Windows.Forms.SendKeys]::SendWait($Text + "{ENTER}")

Write-Output "OK"
