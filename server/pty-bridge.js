#!/usr/bin/env node

/**
 * BTELO Coding PTY Bridge
 * 
 * PTY (Pseudo-Terminal) mode bridge for Claude Code CLI.
 * Uses node-pty to create a real pseudo-terminal for interactive Claude Code sessions.
 * 
 * Features:
 * - Full PTY support with terminal emulation
 * - Write to PTY (simulate keyboard input)
 * - Read from PTY (capture output)
 * - Better for interactive terminal scenarios
 * 
 * Usage:
 *   node pty-bridge.js --session <session_id> --workdir /path/to/project
 *   node pty-bridge.js --session <session_id> --workdir /path/to/project --output-format stream-json
 */

'use strict';

const WebSocket = require('ws');
const http = require('http');
const path = require('path');
const os = require('os');
const fs = require('fs');
const { spawn, execSync } = require('child_process');
const { v4: uuidv4 } = require('uuid');

// Try to load node-pty (optional dependency)
let pty = null;
try {
  pty = require('node-pty');
} catch (err) {
  console.warn('[PTY] node-pty not available, will use spawn mode');
}

// ============================================================
// Parse CLI arguments
// ============================================================
function parseArgs() {
  const args = process.argv.slice(2);
  const opts = {
    server: 'http://localhost:8080',
    sessionId: null,
    workDir: process.cwd(),
    name: os.hostname(),
    outputFormat: 'stream-json',  // 'stream-json' or 'text'
    verbose: false,
    resume: false
  };

  for (let i = 0; i < args.length; i++) {
    switch (args[i]) {
      case '--server': case '-s':
        opts.server = args[++i];
        break;
      case '--session': case '--resume': case '-r':
        opts.sessionId = args[++i];
        opts.resume = true;
        break;
      case '--workdir': case '-w':
        opts.workDir = args[++i];
        break;
      case '--name': case '-n':
        opts.name = args[++i];
        break;
      case '--format': case '-f':
        opts.outputFormat = args[++i];
        break;
      case '--verbose': case '-v':
        opts.verbose = true;
        break;
      case '--help': case '-h':
        console.log(`
BTELO Coding PTY Bridge

Usage:
  node pty-bridge.js [options]

Options:
  -s, --server <url>      Relay server URL (default: http://localhost:8080)
  -r, --session <id>     Claude session ID to resume
  -w, --workdir <path>   Working directory (default: cwd)
  -n, --name <name>      Device name (default: hostname)
  -f, --format <fmt>     Output format: stream-json or text (default: stream-json)
  -v, --verbose          Verbose output
  -h, --help             Show this help
`);
        process.exit(0);
    }
  }
  return opts;
}

// ============================================================
// HTTP helper
// ============================================================
function httpRequest(url, method, body) {
  return new Promise((resolve, reject) => {
    const urlObj = new URL(url);
    const options = {
      hostname: urlObj.hostname,
      port: urlObj.port,
      path: urlObj.pathname + urlObj.search,
      method,
      headers: { 'Content-Type': 'application/json' }
    };

    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
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

// ============================================================
// Claude Code detection
// ============================================================
function detectClaudeCode() {
  try {
    const isWindows = os.platform() === 'win32';
    const cmd = isWindows ? 'where' : 'which';
    const result = execSync(`${cmd} claude`, { encoding: 'utf-8', timeout: 5000 }).trim().replace(/\r/g, '');
    const version = execSync('claude --version', { encoding: 'utf-8', timeout: 5000 }).trim().replace(/\r/g, '');
    const lines = result.split('\n');
    // On Windows, prefer .cmd wrapper for node-pty compatibility
    const path = isWindows ? (lines.find(l => l.endsWith('.cmd')) || lines[0]) : lines[0];
    return { installed: true, path, version };
  } catch (e) {
    return { installed: false, path: null, version: null };
  }
}

// ============================================================
// Session discovery — find existing Claude Code sessions
// ============================================================
const CLAUDE_HOME = path.join(os.homedir(), '.claude');
const SESSIONS_DIR = path.join(CLAUDE_HOME, 'sessions');
const PROJECTS_DIR = path.join(CLAUDE_HOME, 'projects');

function isProcessAlive(pid) {
  try { process.kill(pid, 0); return true; } catch (e) { return false; }
}

function encodeProjectPath(cwd) {
  return cwd.replace(/[\\:]/g, '-');
}

function discoverSessions() {
  const sessions = [];
  if (!fs.existsSync(SESSIONS_DIR)) return sessions;

  const files = fs.readdirSync(SESSIONS_DIR).filter(f => f.endsWith('.json'));
  for (const file of files) {
    try {
      const filePath = path.join(SESSIONS_DIR, file);
      const data = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
      if (data.kind !== 'interactive') continue;
      const isAlive = isProcessAlive(data.pid);
      const encodedPath = encodeProjectPath(data.cwd);
      const jsonlPath = path.join(PROJECTS_DIR, encodedPath, `${data.sessionId}.jsonl`);
      const hasHistory = fs.existsSync(jsonlPath);
      let messageCount = 0;
      if (hasHistory) {
        try {
          const content = fs.readFileSync(jsonlPath, 'utf-8');
          messageCount = content.split('\n').filter(l => l.trim()).length;
        } catch (e) {}
      }
      sessions.push({ sessionId: data.sessionId, cwd: data.cwd, pid: data.pid, isAlive, hasHistory, jsonlPath, startedAt: data.startedAt, messageCount });
    } catch (e) {}
  }
  sessions.sort((a, b) => (b.startedAt || 0) - (a.startedAt || 0));
  return sessions;
}

function autoSelectSession(discovered, workDir) {
  if (discovered.length === 0) return null;
  const normalizedWorkDir = path.resolve(workDir);
  const candidates = discovered.filter(s => s.hasHistory && path.resolve(s.cwd) === normalizedWorkDir);
  const bestMatch = candidates.find(s => s.isAlive) || candidates[0];
  return bestMatch || null;
}

// ============================================================
// PTY Bridge State
// ============================================================
class PtyBridge {
  constructor(opts) {
    this.opts = opts;
    this.state = {
      sessionId: null,
      bridgeToken: null,
      connectToken: null,
      connectUrl: null,
      ws: null,
      ptyProcess: null,
      claudeInfo: null,
      workDir: opts.workDir,
      outputBuffer: '',
      isProcessing: false,
      commandQueue: [],
      readyForInput: false,
      pendingInput: []
    };
    
    // Output parser for structured messages
    this.outputParser = null;
    if (opts.outputFormat === 'stream-json') {
      try {
        const { OutputParser } = require('./output-parser.js');
        this.outputParser = new OutputParser({
          debug: opts.verbose,
          onMessage: (msg) => this.sendStructuredOutput(msg)
        });
      } catch (err) {
        console.warn('[PTY] Failed to load output-parser:', err.message);
      }
    }
  }
  
  /**
   * Send structured output to mobile via WebSocket
   */
  sendStructuredOutput(msg) {
    if (this.state.ws && this.state.ws.readyState === 1) {
      this.state.ws.send(JSON.stringify(msg));
    }
  }
  
  /**
   * Send raw output to mobile via WebSocket (debounced to avoid flooding)
   */
  sendRawOutput(data, stream = 'STDOUT') {
    if (!this.state.ws || this.state.ws.readyState !== 1) return;

    // Strip ALL ANSI escape sequences
    const clean = data.replace(/\x1b\[[0-9;?]*[a-zA-Z]/g, '')
      .replace(/\x1b\][0-9;]*[^\x07]*\x07/g, '')
      .replace(/\x1b[PX^_].*?\x1b\\/g, '')
      .replace(/\x1b[()][0-9]/g, '')
      .replace(/\x1b[=>]/g, '')
      .replace(/\r\n?/g, '\n')
      .replace(/[\x00-\x08\x0B\x0C\x0E-\x1F]/g, '');

    // Buffer output and debounce sending
    if (!this._outputBuffer) this._outputBuffer = '';
    this._outputBuffer += clean;

    if (this._outputTimer) clearTimeout(this._outputTimer);
    this._outputTimer = setTimeout(() => {
      const text = this._outputBuffer.replace(/\n{3,}/g, '\n\n').trim();
      this._outputBuffer = '';
      if (text) {
        this.state.ws.send(JSON.stringify({
          type: 'output',
          data: text,
          stream: stream
        }));
      }
    }, 400); // Debounce: wait 400ms of silence before sending
  }
  
  /**
   * Start PTY process with Claude Code
   */
  startPty() {
    if (!pty) {
      console.error('[PTY] node-pty not available');
      this.startSpawn();
      return;
    }
    
    const shell = os.platform() === 'win32' ? 'powershell.exe' : 'bash';
    const shellArgs = os.platform() === 'win32' ? [] : ['-l'];
    
    console.log('[PTY] Starting Claude Code in PTY mode...');
    console.log('[PTY] Work dir:', this.state.workDir);
    
    // Build Claude command
    let claudeArgs = [];
    
    if (this.opts.sessionId && this.opts.resume) {
      // Resume mode: claude --resume <session>
      claudeArgs = ['--resume', this.opts.sessionId];
    } else {
      // Interactive mode
      claudeArgs = [];
    }
    
    // Note: --output-format stream-json is only for -p (print) mode, not interactive PTY
    // PTY mode uses raw terminal output — phone displays it as plain text
    
    // Add --no-input if we're piping commands
    // claudeArgs.push('--no-input');
    
    // Resolve claude path (node-pty on Windows needs .cmd wrapper)
    const claudeInfo = detectClaudeCode();
    const claudePath = claudeInfo.path || 'claude';
    console.log('[PTY] Claude path:', claudePath);
    this.state.ptyProcess = pty.spawn(claudePath, claudeArgs, {
      name: 'xterm-color',
      cols: 120,
      rows: 40,
      cwd: this.state.workDir,
      env: {
        ...process.env,
        TERM: 'xterm-256color',
        COLORTERM: 'truecolor'
      }
    });
    
    this.state.ptyProcess.onData((data) => {
      this.handlePtyOutput(data);
    });
    
    this.state.ptyProcess.onExit(({ exitCode, signal }) => {
      console.log('[PTY] Claude Code exited:', exitCode, signal);
      this.state.isProcessing = false;
      
      this.sendRawOutput(`\n[Claude Code] Process exited (code: ${exitCode})\n`);
      
      // Process queued commands
      if (this.state.commandQueue.length > 0) {
        const next = this.state.commandQueue.shift();
        this.executeCommand(next);
      }
    });
    
    console.log('[PTY] PTY started with PID:', this.state.ptyProcess.pid);

    // Forward terminal stdin → PTY so user can type in the claudex terminal
    if (process.stdin.isTTY) {
      try { process.stdin.setRawMode(true); } catch (e) {}
      process.stdin.resume();
      process.stdin.on('data', (data) => {
        if (this.state.ptyProcess) {
          this.state.ptyProcess.write(data.toString());
        }
      });
      console.log('[PTY] Terminal stdin forwarding enabled');
    }

    setTimeout(() => this.flushPendingInput(), 900);
  }
  
  /**
   * Start Claude Code using spawn (fallback if node-pty unavailable)
   */
  startSpawn() {
    console.log('[SPAWN] Starting Claude Code — transparent proxy mode');
    console.log('[SPAWN] Work dir:', this.state.workDir);
    console.log('[SPAWN] Terminal stdin → Claude | Claude stdout → Terminal + Phone | Phone → Claude stdin');

    // Start Claude in INTERACTIVE mode — ONE process shared by terminal and phone
    const args = [];
    console.log('[SPAWN] Command: claude (interactive, shared session)');

    this.state.claudeProcess = spawn('claude', args, {
      cwd: this.state.workDir,
      shell: true,
      stdio: ['pipe', 'pipe', 'pipe'],
      env: { ...process.env, TERM: 'xterm-256color' }
    });

    // Claude stdout → terminal + phone
    this.state.claudeProcess.stdout.on('data', (data) => {
      const str = data.toString();
      process.stdout.write(str);        // → terminal
      this.sendRawOutput(str);          // → phone
    });

    // Claude stderr → terminal + phone
    this.state.claudeProcess.stderr.on('data', (data) => {
      const str = data.toString();
      process.stderr.write(str);
      this.sendRawOutput(str, 'STDERR');
    });

    // Terminal stdin → Claude (only if running in a real terminal)
    if (process.stdin.isTTY) {
      try { process.stdin.setRawMode(true); } catch (e) { /* Windows: setRawMode not available */ }
      process.stdin.resume();
      process.stdin.on('data', (data) => {
        if (this.state.claudeProcess && this.state.claudeProcess.stdin.writable) {
          this.state.claudeProcess.stdin.write(data);
        }
      });
      console.log('[SPAWN] Terminal stdin forwarding enabled');
    } else {
      console.log('[SPAWN] No TTY — terminal stdin forwarding disabled');
    }

    this.state.claudeProcess.on('close', (code) => {
      console.log('[SPAWN] Claude exited with code:', code);
      if (process.stdin.isTTY) { try { process.stdin.setRawMode(false); } catch (e) {} }
      this.state.isProcessing = false;
      this.sendRawOutput(`\n[Claude Code] Process exited (code: ${code})\n`);
      // Restart Claude to keep the session alive
      setTimeout(() => {
        if (this.state.ws && this.state.ws.readyState === 1) {
          console.log('[SPAWN] Restarting Claude...');
          this.startSpawn();
        }
      }, 3000);
    });

    this.state.claudeProcess.on('error', (err) => {
      console.error('[SPAWN] Failed to start Claude:', err.message);
      this.sendRawOutput(`Error: ${err.message}\n`, 'STDERR');
    });

    setTimeout(() => this.flushPendingInput(), 900);
  }
  
  /**
   * Handle output from PTY
   */
  handlePtyOutput(data) {
    const str = data.toString();
    // Echo to terminal
    process.stdout.write(str);

    if (this.outputParser && !this.opts.resume) {
      // Use structured output parser (stream-json mode only)
      this.outputParser.process(str);
    } else {
      // Send as raw output (PTY/resume mode)
      this.sendRawOutput(str);
    }
  }
  
  /**
   * Write to PTY (simulate keyboard input)
   */
  writeToPty(text) {
    if (!this.state.readyForInput) {
      this.state.pendingInput.push(text);
      console.log('[PTY] Queued early input:', text.substring(0, 50));
      return;
    }

    if (this.state.ptyProcess) {
      this.state.ptyProcess.write(text);
      console.log('[PTY] Written:', text.substring(0, 50));
    } else if (this.state.claudeProcess && this.state.claudeProcess.stdin) {
      this.state.claudeProcess.stdin.write(text);
      console.log('[SPAWN] Written:', text.substring(0, 50));
    }
  }

  flushPendingInput() {
    this.state.readyForInput = true;
    while (this.state.pendingInput.length > 0) {
      const text = this.state.pendingInput.shift();
      if (this.state.ptyProcess) {
        this.state.ptyProcess.write(text);
        console.log('[PTY] Flushed queued input:', text.substring(0, 50));
      } else if (this.state.claudeProcess && this.state.claudeProcess.stdin) {
        this.state.claudeProcess.stdin.write(text);
        console.log('[SPAWN] Flushed queued input:', text.substring(0, 50));
      }
    }
  }
  
  /**
   * Resize PTY
   */
  resizePty(cols, rows) {
    if (this.state.ptyProcess) {
      this.state.ptyProcess.resize(cols, rows);
      console.log('[PTY] Resized to', cols, 'x', rows);
    }
  }
  
  /**
   * Execute a command via Claude Code (for resume mode)
   */
  executeCommand(command) {
    if (!this.opts.sessionId) {
      console.error('[PTY] No session ID for resume mode');
      this.sendRawOutput('Error: No session ID specified for resume mode\n', 'STDERR');
      return;
    }
    
    if (this.state.isProcessing) {
      this.state.commandQueue.push(command);
      this.sendRawOutput(`[Queued] Command queued (${this.state.commandQueue.length} in queue)\n`);
      return;
    }
    
    this.state.isProcessing = true;
    this.sendRawOutput('[Claude Code] Processing command...\n');
    
    const args = [
      '-p', command,
      '-r', this.opts.sessionId,
      '--output-format', this.opts.outputFormat,
      '--dangerously-skip-permissions'
    ];
    
    const claudeInfo = detectClaudeCode();
    const claudePath = claudeInfo.path || 'claude';
    console.log('[PTY] Executing:', claudePath, args.map(a => JSON.stringify(a)).join(' '));
    
    const child = spawn(claudePath, args, {
      cwd: this.state.workDir,
      shell: false,
      stdio: ['ignore', 'pipe', 'pipe']
    });
    
    let buffer = '';
    
    child.stdout.on('data', (data) => {
      const str = data.toString();
      buffer += str;
      
      if (this.outputParser) {
        this.outputParser.process(str);
      } else {
        this.sendRawOutput(str);
      }
    });
    
    child.stderr.on('data', (data) => {
      this.sendRawOutput(data.toString(), 'STDERR');
    });
    
    child.on('close', (code) => {
      // Flush buffer
      if (buffer.trim()) {
        if (this.outputParser) {
          this.outputParser.flush();
        } else {
          this.sendRawOutput(buffer);
        }
      }
      
      console.log('[PTY] Command completed with code:', code);
      this.state.isProcessing = false;
      this.sendRawOutput(`\n[Done] Exit code: ${code}\n`);
      
      // Process queued commands
      if (this.state.commandQueue.length > 0) {
        const next = this.state.commandQueue.shift();
        this.executeCommand(next);
      }
    });
    
    child.on('error', (err) => {
      console.error('[PTY] Failed to start:', err.message);
      this.sendRawOutput(`Error: ${err.message}\n`, 'STDERR');
      this.state.isProcessing = false;
    });
  }
  
  /**
   * Stop PTY process
   */
  stopPty() {
    if (process.stdin.isTTY) { try { process.stdin.setRawMode(false); } catch (e) {} }

    if (this.state.ptyProcess) {
      console.log('[PTY] Stopping PTY...');
      try {
        this.state.ptyProcess.kill();
      } catch (err) {
        console.error('[PTY] Error killing process:', err.message);
      }
      this.state.ptyProcess = null;
    }
    
    if (this.state.claudeProcess) {
      console.log('[SPAWN] Stopping process...');
      try {
        this.state.claudeProcess.kill();
      } catch (err) {
        console.error('[SPAWN] Error killing process:', err.message);
      }
      this.state.claudeProcess = null;
    }
  }
  
  /**
   * Connect to relay server
   */
  async connect() {
    console.log('[PTY-BRIDGE] Connecting to relay server...');
    
    // Generate 6-digit auth code
    const authCode = String(Math.floor(100000 + Math.random() * 900000));
    console.log('');
    const boxWidth = 36;
    const codePad = boxWidth - 14 - authCode.length;
    console.log('  ╔' + '═'.repeat(boxWidth) + '╗');
    console.log('  ║' + 'AUTH CODE'.padStart((boxWidth + 9) / 2).padEnd(boxWidth) + '║');
    console.log('  ║' + ' '.repeat(12) + authCode + ' '.repeat(codePad) + '║');
    console.log('  ║' + 'Enter this code in the app'.padStart((boxWidth + 26) / 2).padEnd(boxWidth) + '║');
    console.log('  ╚' + '═'.repeat(boxWidth) + '╝');
    console.log('');

    let regRes;
    try {
      regRes = await httpRequest(`${this.opts.server}/bridge/register`, 'POST', {
        device_name: this.opts.name,
        work_dir: this.opts.workDir,
        mode: 'pty',
        auth_code: authCode
      });
    } catch (err) {
      console.error('[PTY-BRIDGE] Cannot reach relay server:', err.message);
      process.exit(1);
    }
    
    if (regRes.status !== 200 || !regRes.body.success) {
      console.error('[PTY-BRIDGE] Registration failed:', JSON.stringify(regRes.body));
      process.exit(1);
    }
    
    this.state.sessionId = regRes.body.session_id;
    this.state.bridgeToken = regRes.body.bridge_token;
    this.state.connectToken = regRes.body.connect_token;
    this.state.connectUrl = regRes.body.connect_url;
    
    console.log('[PTY-BRIDGE] Registered:', this.state.sessionId);

    console.log('  Relay:     ' + this.opts.server);
    console.log('  Session:   ' + this.state.sessionId);
    console.log('='.repeat(50));
    console.log('  Waiting for mobile to connect via auth code...');
    console.log('');
    
    // Connect WebSocket
    const wsUrl = regRes.body.ws_url;
    console.log('[PTY-BRIDGE] Connecting WebSocket:', wsUrl.split('?')[0] + '...');
    
    this.state.ws = new WebSocket(wsUrl);
    
    this.state.ws.on('open', () => {
      console.log('[PTY-BRIDGE] WebSocket connected');
    });
    
    this.state.ws.on('message', (data) => {
      this.handleWsMessage(data.toString());
    });
    
    this.state.ws.on('close', () => {
      console.log('[PTY-BRIDGE] WebSocket closed');
      this.stopPty();
      setTimeout(() => process.exit(0), 1000);
    });
    
    this.state.ws.on('error', (err) => {
      console.error('[PTY-BRIDGE] WebSocket error:', err.message);
    });
    
    // Start PTY if session specified
    if (this.opts.sessionId && this.opts.resume) {
      console.log('[PTY-BRIDGE] Starting Claude Code with session:', this.opts.sessionId);
      this.startPty();
    } else {
      console.log('[PTY-BRIDGE] PTY mode ready - waiting for commands');
      this.startPty();
    }
  }
  
  /**
   * Handle WebSocket message
   */
  handleWsMessage(data) {
    let msg;
    try {
      msg = JSON.parse(data);
    } catch {
      console.error('[PTY-BRIDGE] Invalid JSON:', data);
      return;
    }
    
    switch (msg.type) {
      case 'status':
        if (msg.connected && msg.role === 'bridge') {
          console.log('[PTY-BRIDGE] Relay confirmed bridge connection');
        } else if (msg.connected && msg.peer === 'mobile') {
          console.log('[PTY-BRIDGE] Mobile connected');
          // Send initial status
          this.state.ws.send(JSON.stringify({
            type: 'status',
            connected: true,
            mode: 'pty',
            session_id: this.opts.sessionId || null,
            work_dir: this.state.workDir
          }));
        } else if (!msg.connected && msg.peer === 'mobile') {
          console.log('[PTY-BRIDGE] Mobile disconnected');
        }
        break;
        
      case 'command':
        if (msg.content === 'ping') {
          this.state.ws.send(JSON.stringify({ type: 'output', data: 'pong\n' }));
          return;
        }
        
        console.log('[PTY-BRIDGE] Command from mobile:', msg.content.substring(0, 80));

        // Write to PTY — simulate Enter key to submit
        this.writeToPty(msg.content + '\r');
        break;
        
      case 'pty_resize':
        if (msg.cols && msg.rows) {
          this.resizePty(msg.cols, msg.rows);
        }
        break;
        
      case 'pty_input':
        if (msg.data) {
          this.writeToPty(msg.data);
        }
        break;
        
      case 'select_session':
        if (msg.session_id) {
          this.opts.sessionId = msg.session_id;
          this.opts.resume = true;
          console.log('[PTY-BRIDGE] Switching to session:', msg.session_id);
          this.state.ws.send(JSON.stringify({
            type: 'status',
            connected: true,
            session_id: msg.session_id,
            message: 'Session changed to: ' + msg.session_id
          }));
        }
        break;
        
      default:
        console.log('[PTY-BRIDGE] Unknown message type:', msg.type);
    }
  }
  
  /**
   * Shutdown gracefully
   */
  shutdown() {
    console.log('[PTY-BRIDGE] Shutting down...');
    this.stopPty();
    if (this.state.ws) {
      this.state.ws.close();
    }
  }
}

// ============================================================
// Main
// ============================================================
async function main() {
  const opts = parseArgs();
  const claudeInfo = detectClaudeCode();
  
  const border = '='.repeat(54);
  console.log('');
  console.log(border);
  console.log('  BTELO Coding PTY Bridge');
  console.log(border);
  console.log(`  Server:     ${opts.server}`);
  console.log(`  Work Dir:   ${opts.workDir}`);
  console.log(`  Device:     ${opts.name}`);
  console.log(`  Format:     ${opts.outputFormat}`);
  if (opts.sessionId) {
    console.log(`  Session:    ${opts.sessionId}`);
  }
  if (claudeInfo.installed) {
    console.log(`  Claude:     ${claudeInfo.version}`);
  } else {
    console.log('  Claude:     NOT FOUND');
  }
  if (pty) {
    console.log('  PTY Mode:   Enabled (node-pty)');
  } else {
    console.log('  PTY Mode:   Fallback (spawn)');
  }
  console.log(border);
  console.log('');

  // Discover and auto-select a Claude session
  const discovered = discoverSessions();
  if (discovered.length > 0) {
    console.log(`  Found ${discovered.length} Claude session(s):`);
    for (const s of discovered) {
      const status = s.isAlive ? '● active' : '○ closed';
      console.log(`    ${status}  ${s.sessionId.substring(0, 12)}...  ${s.cwd}`);
    }
    console.log('');
  }

  if (!opts.sessionId) {
    const selected = autoSelectSession(discovered, opts.workDir);
    if (selected) {
      opts.sessionId = selected.sessionId;
      // Don't resume active sessions — --resume is for deferred sessions only.
      // Start fresh, but use discovered session for history sync.
      const status = selected.isAlive ? 'active' : 'closed';
      console.log(`  Found existing session: ${selected.sessionId.substring(0, 12)}... (${status})`);
      console.log('  Starting fresh Claude session (history will sync from existing session)');
      console.log('');
    } else {
      console.log('  No existing Claude session found. Starting fresh...');
      console.log('');
    }
  }

  const bridge = new PtyBridge(opts);
  
  // Graceful shutdown
  process.on('SIGINT', () => {
    console.log('\n[PTY-BRIDGE] Caught SIGINT');
    bridge.shutdown();
    setTimeout(() => process.exit(0), 500);
  });
  
  process.on('SIGTERM', () => {
    console.log('\n[PTY-BRIDGE] Caught SIGTERM');
    bridge.shutdown();
    setTimeout(() => process.exit(0), 500);
  });
  
  // Connect to relay
  await bridge.connect();
}

if (require.main === module) {
main().catch(err => {
  console.error('[PTY-BRIDGE] Fatal error:', err.message);
  process.exit(1);
});
}
