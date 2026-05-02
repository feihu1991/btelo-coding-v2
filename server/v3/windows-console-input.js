const fs = require('fs');
const os = require('os');
const path = require('path');
const { spawn } = require('child_process');

class WindowsConsoleInput {
  constructor({
    focusFreeScript = path.join(__dirname, '..', 'console-bridge', 'console-writer.ps1'),
    focusFallbackScript = path.join(__dirname, '..', 'console-bridge', 'console-sender.ps1')
  } = {}) {
    this.focusFreeScript = focusFreeScript;
    this.focusFallbackScript = focusFallbackScript;
    this.queue = [];
    this.busy = false;
  }

  sendText(session, text) {
    return new Promise((resolve, reject) => {
      if (process.platform !== 'win32') {
        reject(new Error('WindowsConsoleInput only supports Windows'));
        return;
      }
      if (!session || !session.pid) {
        reject(new Error('No live Claude Code PID is available'));
        return;
      }
      const cleaned = String(text || '').replace(/[\r\n]+/g, ' ').trim();
      if (!cleaned) {
        resolve({ ok: true, skipped: true });
        return;
      }
      this.queue.push({ session, text: cleaned, resolve, reject });
      this.processQueue();
    });
  }

  processQueue() {
    if (this.busy || this.queue.length === 0) return;
    this.busy = true;
    const item = this.queue.shift();

    this.runScript(this.focusFreeScript, item.session.pid, item.text)
      .then((result) => item.resolve({ ...result, mode: 'focus_free' }))
      .catch((focusFreeError) => {
        this.runScript(this.focusFallbackScript, item.session.pid, item.text)
          .then((result) => item.resolve({
            ...result,
            mode: 'focus_fallback',
            focusFreeError: focusFreeError.message
          }))
          .catch((fallbackError) => item.reject(new Error(
            `focus-free failed: ${focusFreeError.message}; fallback failed: ${fallbackError.message}`
          )));
      })
      .finally(() => {
        this.busy = false;
        setTimeout(() => this.processQueue(), 200);
      });
  }

  runScript(scriptPath, pid, text) {
    return new Promise((resolve, reject) => {
      const tmpFile = path.join(os.tmpdir(), `btelo-v3-input-${Date.now()}-${Math.random().toString(16).slice(2)}.txt`);
      fs.writeFileSync(tmpFile, text, 'utf-8');

      const child = spawn('powershell', [
        '-NoProfile',
        '-ExecutionPolicy', 'Bypass',
        '-File', scriptPath,
        '-TargetPid', String(pid),
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
        if (code === 0) {
          resolve({ ok: true, stdout: stdout.trim() });
        } else {
          reject(new Error((stderr || stdout || `script exited with code ${code}`).trim()));
        }
      });

      child.on('error', (err) => {
        try { fs.unlinkSync(tmpFile); } catch { /* ignore */ }
        reject(err);
      });
    });
  }
}

module.exports = WindowsConsoleInput;
