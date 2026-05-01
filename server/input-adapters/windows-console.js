const fs = require('fs');
const os = require('os');
const path = require('path');
const { spawn } = require('child_process');

class WindowsConsoleInputAdapter {
  constructor({ scriptPath } = {}) {
    this.scriptPath = scriptPath || path.join(__dirname, '..', 'console-bridge', 'console-sender.ps1');
    this.queue = [];
    this.busy = false;
  }

  sendText(session, text) {
    return new Promise((resolve, reject) => {
      const pid = session && session.pid;
      if (!pid) {
        reject(new Error('No Claude Code PID available for Windows console input'));
        return;
      }
      if (process.platform !== 'win32') {
        reject(new Error('WindowsConsoleInputAdapter only supports Windows'));
        return;
      }

      this.queue.push({ pid, text, resolve, reject });
      this.processQueue();
    });
  }

  processQueue() {
    if (this.busy || this.queue.length === 0) return;
    this.busy = true;

    const item = this.queue.shift();
    const cleanText = String(item.text || '').replace(/[\r\n]+/g, ' ').trim();
    if (!cleanText) {
      this.busy = false;
      item.resolve({ ok: true, skipped: true });
      setImmediate(() => this.processQueue());
      return;
    }

    const tmpFile = path.join(os.tmpdir(), `btelo-input-${Date.now()}-${Math.random().toString(16).slice(2)}.txt`);
    fs.writeFileSync(tmpFile, cleanText, 'utf-8');

    const child = spawn('powershell', [
      '-ExecutionPolicy', 'Bypass',
      '-File', this.scriptPath,
      '-TargetPid', String(item.pid),
      '-TextFile', tmpFile
    ], {
      stdio: ['ignore', 'pipe', 'pipe'],
      windowsHide: true
    });

    let stdout = '';
    let stderr = '';
    child.stdout.on('data', (data) => { stdout += data.toString(); });
    child.stderr.on('data', (data) => { stderr += data.toString(); });

    child.on('close', (code) => {
      try { fs.unlinkSync(tmpFile); } catch { /* ignore */ }
      this.busy = false;

      if (code === 0) {
        item.resolve({ ok: true, stdout: stdout.trim() });
      } else {
        item.reject(new Error((stderr || stdout || `console sender exited with code ${code}`).trim()));
      }

      setTimeout(() => this.processQueue(), 300);
    });

    child.on('error', (err) => {
      try { fs.unlinkSync(tmpFile); } catch { /* ignore */ }
      this.busy = false;
      item.reject(err);
      setTimeout(() => this.processQueue(), 300);
    });
  }
}

module.exports = WindowsConsoleInputAdapter;
