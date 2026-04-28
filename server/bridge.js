#!/usr/bin/env node

/**
 * BTELO Coding Bridge — standalone CLI tool managing Claude Code.
 *
 * Registers with the relay server, generates a QR code for mobile connection,
 * and manages Claude Code processes (session discovery, command execution, JSONL watching).
 *
 * Usage:
 *   node bridge.js
 *   node bridge.js --server http://relay:8080 --workdir /path/to/project
 *   node bridge.js --mode persistent
 */

const WebSocket = require('ws');
const { spawn, execSync } = require('child_process');
const http = require('http');
const { v4: uuidv4 } = require('uuid');
const os = require('os');
const fs = require('fs');
const path = require('path');
const qrcode = require('qrcode-terminal');

// ============================================================
// Parse CLI arguments
// ============================================================
function parseArgs() {
  const args = process.argv.slice(2);
  const opts = {
    server: 'http://localhost:8080',
    workDir: process.cwd(),
    name: os.hostname(),
    mode: 'resume'  // 'resume' or 'persistent'
  };

  for (let i = 0; i < args.length; i++) {
    switch (args[i]) {
      case '--server': case '-s':
        opts.server = args[++i];
        break;
      case '--workdir': case '-w':
        opts.workDir = args[++i];
        break;
      case '--name': case '-n':
        opts.name = args[++i];
        break;
      case '--mode': case '-m':
        opts.mode = args[++i];
        break;
      case '--help': case '-h':
        console.log(`
BTELO Coding Bridge — Claude Code CLI Manager

Usage:
  node bridge.js [options]

Options:
  -s, --server <url>       Relay server URL (default: http://localhost:8080)
  -w, --workdir <path>     Working directory for Claude Code (default: cwd)
  -n, --name <name>        Device name (default: hostname)
  -m, --mode <mode>        'resume' (spawn per command) or 'persistent' (one process)
  -h, --help               Show this help

Flow:
  1. Bridge registers with relay server
  2. QR code is displayed — scan with BTELO app
  3. Bridge connects to relay via WebSocket
  4. Commands from mobile are forwarded to Claude Code
  5. Claude output is streamed back to mobile
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
    const result = execSync(`${cmd} claude`, { encoding: 'utf-8', timeout: 5000 }).trim();
    const version = execSync('claude --version', { encoding: 'utf-8', timeout: 5000 }).trim();
    return { installed: true, path: result.split('\n')[0], version };
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
  try {
    process.kill(pid, 0);
    return true;
  } catch (e) {
    return false;
  }
}

function encodeProjectPath(cwd) {
  return cwd.replace(/[\\:]/g, '-');
}

function discoverSessions() {
  const sessions = [];

  if (!fs.existsSync(SESSIONS_DIR)) {
    return sessions;
  }

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
      let lastMessage = null;
      if (hasHistory) {
        const content = fs.readFileSync(jsonlPath, 'utf-8');
        const lines = content.split('\n').filter(l => l.trim());
        for (const line of lines) {
          try {
            const entry = JSON.parse(line);
            if (entry.type === 'user') {
              messageCount++;
              lastMessage = {
                content: typeof entry.message.content === 'string'
                  ? entry.message.content.substring(0, 100)
                  : '',
                timestamp: new Date(entry.timestamp).getTime()
              };
            }
          } catch (e) { /* skip bad lines */ }
        }
      }

      sessions.push({
        sessionId: data.sessionId,
        cwd: data.cwd,
        pid: data.pid,
        isAlive,
        hasHistory,
        jsonlPath: hasHistory ? jsonlPath : null,
        startedAt: data.startedAt,
        messageCount,
        lastMessage
      });
    } catch (e) {
      // skip bad files
    }
  }

  sessions.sort((a, b) => (b.startedAt || 0) - (a.startedAt || 0));
  return sessions;
}

// ============================================================
// JSONL parsing
// ============================================================
function parseJsonlEntry(entry) {
  if (entry.type === 'user') {
    const content = entry.message.content;
    return {
      id: entry.uuid,
      content: typeof content === 'string' ? content : '',
      isFromUser: true,
      timestamp: new Date(entry.timestamp).getTime()
    };
  } else if (entry.type === 'assistant') {
    const content = entry.message.content;
    if (!Array.isArray(content)) return null;
    const textBlocks = content.filter(b => b.type === 'text').map(b => b.text);
    if (textBlocks.length > 0) {
      return {
        id: entry.uuid,
        content: textBlocks.join(''),
        isFromUser: false,
        timestamp: new Date(entry.timestamp).getTime()
      };
    }
  }
  return null;
}

function parseJsonlHistory(filePath) {
  if (!fs.existsSync(filePath)) return [];
  const content = fs.readFileSync(filePath, 'utf-8');
  const lines = content.split('\n').filter(l => l.trim());
  const messages = [];
  for (const line of lines) {
    try {
      const entry = JSON.parse(line);
      const msg = parseJsonlEntry(entry);
      if (msg) messages.push(msg);
    } catch (e) { /* skip bad lines */ }
  }
  return messages;
}

// ============================================================
// JSONL file watcher — real-time sync
// ============================================================
function watchJsonlFile(state, onNewMessage) {
  const filePath = state.jsonlPath;
  if (!filePath || !fs.existsSync(filePath)) return;

  let lastSize = fs.statSync(filePath).size;

  state.fileWatcher = fs.watch(filePath, (eventType) => {
    if (eventType !== 'change') return;

    clearTimeout(state.watchDebounce);
    state.watchDebounce = setTimeout(() => {
      try {
        const currentSize = fs.statSync(filePath).size;
        if (currentSize <= lastSize) return;

        const buffer = Buffer.alloc(currentSize - lastSize);
        const fd = fs.openSync(filePath, 'r');
        fs.readSync(fd, buffer, 0, buffer.length, lastSize);
        fs.closeSync(fd);

        const newContent = buffer.toString('utf-8');
        const lines = newContent.split('\n').filter(l => l.trim());

        for (const line of lines) {
          try {
            const entry = JSON.parse(line);
            const msg = parseJsonlEntry(entry);
            if (msg) onNewMessage(msg);
          } catch (e) { /* skip bad lines */ }
        }

        lastSize = currentSize;
      } catch (e) {
        console.error('[WATCHER] Error reading file change:', e.message);
      }
    }, 100);
  });

  console.log(`[WATCHER] Watching ${filePath}`);
}

function stopWatching(state) {
  if (state.fileWatcher) {
    state.fileWatcher.close();
    state.fileWatcher = null;
  }
  if (state.watchDebounce) {
    clearTimeout(state.watchDebounce);
    state.watchDebounce = null;
  }
}

// ============================================================
// Claude event formatting
// ============================================================
function formatClaudeEvent(event) {
  switch (event.type) {
    case 'assistant':
      if (event.message && event.message.content) {
        const texts = event.message.content
          .filter(b => b.type === 'text')
          .map(b => b.text);
        if (texts.length > 0) {
          return { stream: 'STDOUT', data: texts.join('') };
        }
      }
      return null;
    case 'result':
      if (event.result) {
        return { stream: 'STDOUT', data: event.result };
      }
      return null;
    case 'text':
      return { stream: 'STDOUT', data: event.text || '' };
    case 'stderr':
      return { stream: 'STDERR', data: event.text || '' };
    case 'error':
      return { stream: 'STDERR', data: event.text || JSON.stringify(event) };
    default:
      return null;
  }
}

// ============================================================
// Persistent Claude process (mode: persistent)
// ============================================================
function startPersistentProcess(state, onOutput) {
  if (!state.claudeSessionId) {
    state.claudeSessionId = uuidv4();
  }

  const args = [
    '--input-format', 'stream-json',
    '--output-format', 'stream-json',
    '--session-id', state.claudeSessionId,
    '--dangerously-skip-permissions'
  ];

  console.log(`[CLAUDE] Starting persistent process: claude ${args.join(' ')}`);
  console.log(`[CLAUDE] Session ID: ${state.claudeSessionId}`);
  console.log(`[CLAUDE] Work dir: ${state.workDir}`);

  const child = spawn('claude', args, {
    cwd: state.workDir,
    shell: true,
    stdio: ['pipe', 'pipe', 'pipe']
  });

  state.claudeProcess = child;
  state.claudeStdin = child.stdin;
  state.outputBuffer = '';

  child.stdout.on('data', (data) => {
    state.outputBuffer += data.toString();
    const lines = state.outputBuffer.split('\n');
    state.outputBuffer = lines.pop();

    for (const line of lines) {
      if (line.trim()) {
        try {
          const event = JSON.parse(line);
          onOutput(event);
        } catch {
          onOutput({ type: 'text', text: line });
        }
      }
    }
  });

  child.stderr.on('data', (data) => {
    const text = data.toString().trim();
    if (text) {
      console.log(`[CLAUDE:stderr] ${text}`);
      onOutput({ type: 'stderr', text });
    }
  });

  child.on('close', (code) => {
    console.log(`[CLAUDE] Persistent process exited with code ${code}`);
    state.claudeProcess = null;
    state.claudeStdin = null;
    state.processingCommand = false;
  });

  child.on('error', (err) => {
    console.error(`[CLAUDE] Failed to start: ${err.message}`);
    state.claudeProcess = null;
    state.claudeStdin = null;
    state.processingCommand = false;
    onOutput({ type: 'error', text: `Failed to start Claude: ${err.message}` });
  });

  console.log(`[CLAUDE] Persistent process started (PID: ${child.pid})`);
}

function sendToPersistentProcess(state, command, onOutput) {
  if (!state.claudeStdin) {
    startPersistentProcess(state, onOutput);
    if (!state.claudeStdin) {
      onOutput({ type: 'error', text: 'Could not start Claude Code process' });
      return;
    }
  }

  state.processingCommand = true;
  console.log(`[CLAUDE] Sending command via stdin: ${command.substring(0, 80)}...`);

  const jsonMessage = JSON.stringify({
    type: 'user_message',
    content: command
  }) + '\n';

  try {
    state.claudeStdin.write(jsonMessage);
  } catch (err) {
    console.error(`[CLAUDE] Failed to write to stdin: ${err.message}`);
    state.processingCommand = false;
    onOutput({ type: 'error', text: `Failed to send command: ${err.message}` });
  }
}

// ============================================================
// Resume-mode command execution (mode: resume)
// ============================================================
function executeResumeCommand(state, command, onOutput) {
  if (!state.claudeInfo.installed) {
    onOutput({ type: 'error', text: 'Claude Code is not installed on this computer.' });
    return;
  }

  if (!state.claudeSessionId) {
    onOutput({ type: 'error', text: 'No Claude session selected. Please select a session first.' });
    return;
  }

  console.log(`[CLAUDE] Running: claude -p "${command.substring(0, 60)}..." -r ${state.claudeSessionId}`);

  state.processingCommand = true;

  const args = [
    '-p', command,
    '-r', state.claudeSessionId,
    '--output-format', 'stream-json',
    '--dangerously-skip-permissions'
  ];

  const child = spawn('claude', args, {
    cwd: state.workDir,
    shell: true,
    stdio: ['ignore', 'pipe', 'pipe']
  });

  let buffer = '';

  child.stdout.on('data', (data) => {
    buffer += data.toString();
    const lines = buffer.split('\n');
    buffer = lines.pop();

    for (const line of lines) {
      if (line.trim()) {
        try {
          const event = JSON.parse(line);
          const formatted = formatClaudeEvent(event);
          if (formatted) onOutput({ type: 'output', data: formatted.data, stream: formatted.stream });
        } catch {
          onOutput({ type: 'output', data: line, stream: 'STDOUT' });
        }
      }
    }
  });

  child.stderr.on('data', (data) => {
    const text = data.toString().trim();
    if (text) {
      console.log(`[CLAUDE:stderr] ${text}`);
      onOutput({ type: 'output', data: text, stream: 'STDERR' });
    }
  });

  child.on('close', (code) => {
    if (buffer.trim()) {
      try {
        const event = JSON.parse(buffer);
        const formatted = formatClaudeEvent(event);
        if (formatted) onOutput({ type: 'output', data: formatted.data, stream: formatted.stream });
      } catch {
        onOutput({ type: 'output', data: buffer, stream: 'STDOUT' });
      }
    }

    console.log(`[CLAUDE] Process exited with code ${code}`);
    state.processingCommand = false;
    onOutput({ type: 'output', data: `\n[Done] Exit code: ${code}\n`, stream: 'STDOUT' });

    // Process queued commands
    if (state.commandQueue.length > 0) {
      const next = state.commandQueue.shift();
      console.log(`[CLAUDE] Processing queued command (${state.commandQueue.length} remaining)`);
      executeResumeCommand(state, next, onOutput);
    }
  });

  child.on('error', (err) => {
    console.error(`[CLAUDE] Failed to start: ${err.message}`);
    state.processingCommand = false;
    onOutput({ type: 'output', data: `Error: Failed to start Claude Code: ${err.message}`, stream: 'STDERR' });
  });
}

// ============================================================
// Main
// ============================================================
async function main() {
  const opts = parseArgs();
  const claudeInfo = detectClaudeCode();

  // Bridge state
  const state = {
    sessionId: null,
    bridgeToken: null,
    connectToken: null,
    connectUrl: null,
    ws: null,
    claudeInfo,
    claudeSessionId: null,
    jsonlPath: null,
    workDir: opts.workDir,
    fileWatcher: null,
    watchDebounce: null,
    processingCommand: false,
    commandQueue: [],
    claudeProcess: null,
    claudeStdin: null,
    outputBuffer: ''
  };

  console.log('');
  console.log('='.repeat(50));
  console.log('  BTELO Coding Bridge');
  console.log('='.repeat(50));
  console.log(`  Server:    ${opts.server}`);
  console.log(`  Work Dir:  ${opts.workDir}`);
  console.log(`  Device:    ${opts.name}`);
  console.log(`  Mode:      ${opts.mode}`);
  if (claudeInfo.installed) {
    console.log(`  Claude:    ${claudeInfo.version}`);
  } else {
    console.log('  Claude:    NOT FOUND');
  }
  console.log('='.repeat(50));
  console.log('');

  // Step 1: Register with relay
  console.log('[BRIDGE] Registering with relay server...');
  let regRes;
  try {
    regRes = await httpRequest(`${opts.server}/bridge/register`, 'POST', {
      device_name: opts.name,
      work_dir: opts.workDir
    });
  } catch (err) {
    console.error(`[BRIDGE] Cannot reach relay server at ${opts.server}: ${err.message}`);
    process.exit(1);
  }

  if (regRes.status !== 200 || !regRes.body.success) {
    console.error(`[BRIDGE] Registration failed: ${JSON.stringify(regRes.body)}`);
    process.exit(1);
  }

  state.sessionId = regRes.body.session_id;
  state.bridgeToken = regRes.body.bridge_token;
  state.connectToken = regRes.body.connect_token;
  state.connectUrl = regRes.body.connect_url;

  console.log(`[BRIDGE] Registered: session ${state.sessionId}`);
  console.log('');

  // Step 2: Show QR code
  console.log('  Scan QR code with BTELO app to connect:');
  console.log('');
  qrcode.generate(state.connectUrl, { small: true }, (qrText) => {
    console.log(qrText);
    console.log('');
    console.log(`  Connection URL: ${state.connectUrl}`);
    console.log('='.repeat(50));
    console.log('');
  });

  // Step 3: Discover existing sessions and auto-select the best match
  const discovered = discoverSessions();
  console.log(`  Found ${discovered.length} Claude session(s):`);
  for (const s of discovered) {
    const status = s.isAlive ? '● active' : '○ closed';
    console.log(`    ${status}  ${s.sessionId.substring(0, 12)}...  ${s.messageCount} msgs  ${s.cwd}`);
  }
  console.log('');

  // Auto-select: match by cwd, prefer alive processes, then most recent
  const normalizedWorkDir = path.resolve(opts.workDir);
  const candidates = discovered
    .filter(s => s.hasHistory && path.resolve(s.cwd) === normalizedWorkDir);
  const bestMatch = candidates.find(s => s.isAlive) || candidates[0];

  if (bestMatch) {
    state.claudeSessionId = bestMatch.sessionId;
    state.jsonlPath = bestMatch.jsonlPath;
    state.workDir = bestMatch.cwd;
    const status = bestMatch.isAlive ? 'active' : 'closed';
    console.log(`  Auto-selected session: ${bestMatch.sessionId.substring(0, 12)}... (${status}, ${bestMatch.messageCount} msgs)`);
    console.log('');
  }

  // Step 4: Connect WebSocket to relay
  const wsUrl = regRes.body.ws_url;
  console.log(`[BRIDGE] Connecting to relay: ${wsUrl.split('?')[0]}...`);

  state.ws = new WebSocket(wsUrl);

  state.ws.on('open', () => {
    console.log('[BRIDGE] WebSocket connected to relay');
  });

  state.ws.on('message', (data) => {
    let msg;
    try {
      msg = JSON.parse(data.toString());
    } catch {
      console.error('[BRIDGE] Invalid JSON:', data.toString());
      return;
    }

    switch (msg.type) {
      case 'status':
        if (msg.connected && msg.role === 'bridge') {
          console.log('[BRIDGE] Relay confirmed bridge connection');
          console.log('[BRIDGE] Waiting for mobile to connect...\n');
        } else if (msg.connected && msg.peer === 'mobile') {
          console.log('[BRIDGE] Mobile connected via relay');
          // Auto-send history if a session is already selected
          if (state.claudeSessionId && state.jsonlPath) {
            const history = parseJsonlHistory(state.jsonlPath);
            console.log(`[BRIDGE] Auto-sending history: ${state.claudeSessionId.substring(0, 12)}... (${history.length} msgs)`);
            state.ws.send(JSON.stringify({
              type: 'sync_history',
              session_id: state.claudeSessionId,
              messages: history
            }));
            // Start watching for real-time changes
            watchJsonlFile(state, (newMsg) => {
              if (state.ws && state.ws.readyState === 1) {
                state.ws.send(JSON.stringify({
                  type: 'new_message',
                  session_id: state.claudeSessionId,
                  message: newMsg
                }));
              }
            });
          }
        } else if (!msg.connected && msg.peer === 'mobile') {
          console.log('[BRIDGE] Mobile disconnected');
          stopWatching(state);
        }
        break;

      case 'command':
        if (msg.content === 'ping') {
          state.ws.send(JSON.stringify({ type: 'command', content: 'pong', inputType: 'TEXT' }));
          return;
        }

        console.log(`[BRIDGE] Received command: ${msg.content.substring(0, 80)}...`);

        // Send acknowledgment
        state.ws.send(JSON.stringify({
          type: 'output',
          data: '[Claude Code] Processing...\n',
          stream: 'STDOUT'
        }));

        if (state.processingCommand) {
          state.commandQueue.push(msg.content);
          state.ws.send(JSON.stringify({
            type: 'output',
            data: `[Queued] Command queued (${state.commandQueue.length} in queue)\n`,
            stream: 'STDOUT'
          }));
          return;
        }

        if (opts.mode === 'persistent') {
          sendToPersistentProcess(state, msg.content, (event) => {
            const formatted = formatClaudeEvent(event);
            if (formatted) {
              state.ws.send(JSON.stringify({
                type: 'output',
                data: formatted.data,
                stream: formatted.stream
              }));
            }
            if (event.type === 'result') {
              state.processingCommand = false;
              state.ws.send(JSON.stringify({ type: 'output', data: '\n[Done]\n', stream: 'STDOUT' }));
              if (state.commandQueue.length > 0) {
                const next = state.commandQueue.shift();
                console.log(`[BRIDGE] Processing queued command (${state.commandQueue.length} remaining)`);
                sendToPersistentProcess(state, next, () => {});
              }
            }
          });
        } else {
          executeResumeCommand(state, msg.content, (output) => {
            state.ws.send(JSON.stringify(output));
          });
        }
        break;

      case 'select_session':
        handleSelectSession(msg, state);
        break;

      case 'publicKey':
        // Ignore encryption handshake
        break;

      case 'keyRotation':
        // Respond to key rotation
        if (msg.action === 'initiate') {
          state.ws.send(JSON.stringify({
            type: 'keyRotation',
            action: 'accept',
            newPublicKey: Buffer.from('rotated-terminal-key-' + Date.now()).toString('base64'),
            keyVersion: msg.keyVersion,
            timestamp: Date.now()
          }));
          state.ws.send(JSON.stringify({
            type: 'keyRotation',
            action: 'complete',
            newPublicKey: '',
            keyVersion: msg.keyVersion,
            timestamp: Date.now()
          }));
        }
        break;

      default:
        console.log(`[BRIDGE] Unhandled message type: ${msg.type}`);
    }
  });

  state.ws.on('close', (code, reason) => {
    console.log(`[BRIDGE] WebSocket closed: ${code} ${reason}`);
    stopWatching(state);
    if (state.claudeProcess) {
      console.log('[BRIDGE] Shutting down Claude process');
      state.claudeProcess.kill();
      state.claudeProcess = null;
      state.claudeStdin = null;
    }
    setTimeout(() => process.exit(0), 1000);
  });

  state.ws.on('error', (err) => {
    console.error(`[BRIDGE] WebSocket error: ${err.message}`);
  });

  // Graceful shutdown
  process.on('SIGINT', () => {
    console.log('\n[BRIDGE] Shutting down...');
    stopWatching(state);
    if (state.claudeProcess) {
      console.log('[BRIDGE] Shutting down Claude process');
      state.claudeProcess.kill();
      state.claudeProcess = null;
      state.claudeStdin = null;
    }
    state.ws.close();
    setTimeout(() => process.exit(0), 500);
  });
}

// ============================================================
// Handle select_session — attach to an existing Claude session
// ============================================================
function handleSelectSession(msg, state) {
  const claudeSessionId = msg.session_id;
  if (!claudeSessionId) {
    state.ws.send(JSON.stringify({ type: 'error', message: 'session_id required' }));
    return;
  }

  console.log(`[BRIDGE] Selecting Claude session: ${claudeSessionId}`);

  const discovered = discoverSessions();
  const target = discovered.find(s => s.sessionId === claudeSessionId);

  if (!target) {
    state.ws.send(JSON.stringify({ type: 'error', message: 'Session not found' }));
    return;
  }

  if (!target.hasHistory) {
    state.ws.send(JSON.stringify({ type: 'error', message: 'Session has no history' }));
    return;
  }

  // Stop previous watcher
  stopWatching(state);

  // Update state
  state.claudeSessionId = claudeSessionId;
  state.jsonlPath = target.jsonlPath;
  state.workDir = target.cwd;

  // Send history
  console.log(`[BRIDGE] Loading history from ${target.jsonlPath}`);
  const history = parseJsonlHistory(target.jsonlPath);
  console.log(`[BRIDGE] Sending ${history.length} messages to mobile`);

  state.ws.send(JSON.stringify({
    type: 'sync_history',
    session_id: claudeSessionId,
    messages: history
  }));

  // Start watching for real-time changes
  watchJsonlFile(state, (msg) => {
    if (state.ws && state.ws.readyState === 1) {
      state.ws.send(JSON.stringify({
        type: 'new_message',
        session_id: claudeSessionId,
        message: msg
      }));
    }
  });

  // Confirm selection
  state.ws.send(JSON.stringify({
    type: 'status',
    connected: true,
    claude_code: state.claudeInfo,
    session_id: claudeSessionId,
    message: `Connected to session (${history.length} messages loaded)`
  }));
}

main().catch(err => {
  console.error(`[BRIDGE] Fatal error: ${err.message}`);
  process.exit(1);
});
