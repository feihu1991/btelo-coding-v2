#!/usr/bin/env node

'use strict';

const WebSocket = require('ws');
const http = require('http');
const os = require('os');
const path = require('path');
const { spawn, execSync } = require('child_process');

let pty = null;
try {
  pty = require('node-pty');
} catch {
  pty = null;
}

const DEFAULT_SERVER = process.env.BTELO_RELAY_SERVER || 'http://localhost:8080';
const CLAUDE_ARGV = JSON.parse(process.env.BTELO_CLAUDE_ARGV || '[]');
const WORKDIR = process.env.BTELO_WORKDIR || process.cwd();

function requestJson(url, method = 'GET', body = null) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url);
    const req = http.request({
      hostname: parsed.hostname,
      port: parsed.port,
      path: parsed.pathname + parsed.search,
      method,
      headers: { 'Content-Type': 'application/json' }
    }, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, body: JSON.parse(data) });
        } catch {
          resolve({ status: res.statusCode, body: data });
        }
      });
    });
    req.on('error', reject);
    if (body) req.write(JSON.stringify(body));
    req.end();
  });
}

function detectClaudeBinary() {
  try {
    const lookup = process.platform === 'win32' ? 'where claude' : 'which claude';
    const result = execSync(lookup, { encoding: 'utf-8', timeout: 5000 })
      .replace(/\r/g, '')
      .trim()
      .split('\n')
      .filter(Boolean);
    if (process.platform === 'win32') {
      return result.find((item) => item.toLowerCase().endsWith('.cmd')) || result[0] || 'claude';
    }
    return result[0] || 'claude';
  } catch {
    return 'claude';
  }
}

function stripAnsi(value) {
  return String(value || '')
    .replace(/\x1b\[[0-9;?]*[ -/]*[@-~]/g, '')
    .replace(/\x1b\][^\x07]*(\x07|\x1b\\)/g, '')
    .replace(/\x1b[PX^_].*?\x1b\\/g, '')
    .replace(/\x1b[()][0-9A-Za-z]/g, '')
    .replace(/\x1b[=>]/g, '');
}

class TerminalBridge {
  constructor() {
    this.server = DEFAULT_SERVER;
    this.authCode = String(Math.floor(100000 + Math.random() * 900000));
    this.ws = null;
    this.term = null;
    this.outputBuffer = '';
    this.outputTimer = null;
    this.restarting = false;
  }

  send(message) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    }
  }

  sendTerminalFrame(data, stream = 'stdout') {
    const text = String(data || '');
    if (!text) return;
    this.send({
      type: 'terminal_frame',
      encoding: 'base64',
      stream,
      data: Buffer.from(text, 'utf-8').toString('base64'),
      timestamp: Date.now()
    });

    this.outputBuffer += stripAnsi(text).replace(/\r\n?/g, '\n');
    clearTimeout(this.outputTimer);
    this.outputTimer = setTimeout(() => {
      const output = this.outputBuffer.replace(/\n{3,}/g, '\n\n').trim();
      this.outputBuffer = '';
      if (output) {
        this.send({ type: 'output', data: output, stream: stream === 'stderr' ? 'STDERR' : 'STDOUT' });
      }
    }, 150);
  }

  async register() {
    const response = await requestJson(`${this.server}/bridge/register`, 'POST', {
      device_name: os.hostname(),
      work_dir: WORKDIR,
      mode: 'claudex_terminal',
      auth_code: this.authCode,
      command: {
        binary: 'claude',
        argv: CLAUDE_ARGV
      }
    });

    if (response.status !== 200 || !response.body.success) {
      throw new Error(`Bridge registration failed: ${JSON.stringify(response.body)}`);
    }

    process.stderr.write(`\n[claudex] auth code: ${this.authCode}\n`);
    process.stderr.write(`[claudex] relay: ${this.server}\n`);
    process.stderr.write(`[claudex] workdir: ${WORKDIR}\n\n`);

    this.ws = new WebSocket(response.body.ws_url);
    this.ws.on('open', () => {
      this.send({
        type: 'bridge_status',
        connected: true,
        mode: 'claudex_terminal',
        command: { binary: 'claude', argv: CLAUDE_ARGV },
        message: 'claudex terminal bridge ready'
      });
    });
    this.ws.on('message', (data) => this.handleWsMessage(data.toString()));
    this.ws.on('close', () => this.shutdown());
    this.ws.on('error', (err) => process.stderr.write(`[claudex] websocket error: ${err.message}\n`));
  }

  startClaude() {
    const binary = detectClaudeBinary();
    const env = {
      ...process.env,
      TERM: process.env.TERM || 'xterm-256color',
      COLORTERM: process.env.COLORTERM || 'truecolor'
    };

    if (pty) {
      this.term = pty.spawn(binary, CLAUDE_ARGV, {
        name: 'xterm-256color',
        cols: Number(process.env.BTELO_COLS || 120),
        rows: Number(process.env.BTELO_ROWS || 40),
        cwd: WORKDIR,
        env
      });
      this.term.onData((data) => {
        process.stdout.write(data);
        this.sendTerminalFrame(data);
      });
      this.term.onExit(({ exitCode, signal }) => {
        this.send({ type: 'terminal_exit', exit_code: exitCode, signal: signal || null, timestamp: Date.now() });
        if (!this.restarting) process.exit(exitCode || 0);
      });
      this.attachLocalInput((data) => this.term && this.term.write(data));
      return;
    }

    this.term = spawn(binary, CLAUDE_ARGV, {
      cwd: WORKDIR,
      shell: process.platform === 'win32',
      stdio: ['pipe', 'pipe', 'pipe'],
      env
    });
    this.term.stdout.on('data', (data) => {
      process.stdout.write(data);
      this.sendTerminalFrame(data.toString(), 'stdout');
    });
    this.term.stderr.on('data', (data) => {
      process.stderr.write(data);
      this.sendTerminalFrame(data.toString(), 'stderr');
    });
    this.term.on('exit', (code, signal) => {
      this.send({ type: 'terminal_exit', exit_code: code, signal: signal || null, timestamp: Date.now() });
      if (!this.restarting) process.exit(code || 0);
    });
    this.attachLocalInput((data) => this.term && this.term.stdin && this.term.stdin.write(data));
  }

  attachLocalInput(write) {
    if (!process.stdin.isTTY) return;
    try { process.stdin.setRawMode(true); } catch {}
    process.stdin.resume();
    process.stdin.on('data', write);
  }

  handleWsMessage(raw) {
    let msg = null;
    try { msg = JSON.parse(raw); } catch { return; }

    if (msg.type === 'command' && typeof msg.content === 'string') {
      this.writeInput(`${msg.content}\r`);
      return;
    }
    if (msg.type === 'terminal_input' && typeof msg.data === 'string') {
      this.writeInput(msg.data);
      return;
    }
    if (msg.type === 'terminal_resize') {
      this.resize(Number(msg.cols || 120), Number(msg.rows || 40));
      return;
    }
    if (msg.type === 'bridge_control') {
      this.handleControl(msg);
    }
  }

  writeInput(data) {
    if (!this.term) return;
    if (typeof this.term.write === 'function') this.term.write(data);
    else if (this.term.stdin) this.term.stdin.write(data);
  }

  resize(cols, rows) {
    if (this.term && typeof this.term.resize === 'function') {
      this.term.resize(cols, rows);
    }
  }

  handleControl(msg) {
    switch (msg.action) {
      case 'restart_bridge':
        this.restartSelf();
        break;
      case 'restart_relay':
        requestJson(`${this.server}/restart`, 'POST').catch(() => {});
        this.send({ type: 'bridge_control_result', action: msg.action, success: true });
        break;
      case 'build_apk':
        this.buildApk();
        break;
      default:
        this.send({ type: 'bridge_control_result', action: msg.action || '', success: false, message: 'Unknown action' });
    }
  }

  buildApk() {
    const gradle = process.platform === 'win32' ? 'gradlew.bat' : './gradlew';
    const child = spawn(gradle, ['assembleDebug'], {
      cwd: WORKDIR,
      shell: process.platform === 'win32',
      stdio: ['ignore', 'pipe', 'pipe']
    });
    child.stdout.on('data', (data) => this.sendTerminalFrame(data.toString()));
    child.stderr.on('data', (data) => this.sendTerminalFrame(data.toString(), 'stderr'));
    child.on('exit', (code) => {
      this.send({ type: 'bridge_control_result', action: 'build_apk', success: code === 0, exit_code: code });
    });
  }

  restartSelf() {
    this.restarting = true;
    this.send({ type: 'bridge_control_result', action: 'restart_bridge', success: true, message: 'Restarting claudex bridge' });
    const child = spawn(process.argv[0], process.argv.slice(1), {
      cwd: process.cwd(),
      env: process.env,
      detached: true,
      stdio: 'inherit',
      windowsHide: true
    });
    child.unref();
    this.shutdown();
    setTimeout(() => process.exit(0), 200);
  }

  shutdown() {
    try { if (process.stdin.isTTY) process.stdin.setRawMode(false); } catch {}
    if (this.term) {
      try { this.term.kill(); } catch {}
      this.term = null;
    }
  }
}

async function main() {
  const bridge = new TerminalBridge();
  await bridge.register();
  bridge.startClaude();
  process.on('SIGINT', () => {
    bridge.shutdown();
    process.exit(0);
  });
  process.on('SIGTERM', () => {
    bridge.shutdown();
    process.exit(0);
  });
}

main().catch((err) => {
  process.stderr.write(`[claudex] ${err.stack || err.message}\n`);
  process.exit(1);
});
