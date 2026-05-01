#!/usr/bin/env node

/**
 * BTELO Coding Bridge — standalone CLI tool managing Claude Code.
 * 
 * Registers with the relay server, generates an auth code for mobile connection,
 * and manages Claude Code processes (session discovery, command execution, JSONL watching).
 * 
 * Two execution modes:
 * - resume mode: spawns Claude Code per command with --resume flag
 * - pty mode: uses pty-bridge.js for full pseudo-terminal support
 * 
 * Structured output support:
 * - Parses Claude Code's stream-json output
 * - Sends structured messages to mobile for rich rendering
 * 
 * Usage:
 *   node bridge.js
 *   node bridge.js --server http://relay:8080 --workdir /path/to/project
 *   node bridge.js --mode pty --session <session_id>
 *   node bridge.js --structured-output false
 */

const WebSocket = require('ws');
const { spawn, execSync } = require('child_process');
const http = require('http');
const { v4: uuidv4 } = require('uuid');
const os = require('os');
const fs = require('fs');
const path = require('path');
// Output parser (optional)
let OutputParser = null;
try {
  const parserModule = require('./output-parser.js');
  OutputParser = parserModule.OutputParser;
} catch (err) {
  console.warn('[BRIDGE] output-parser.js not available, using basic mode');
}

// PTY Bridge (optional)
let PtyBridge = null;
try {
  PtyBridge = require('./pty-bridge.js');
} catch (err) {
  console.warn('[BRIDGE] pty-bridge.js not available');
}

// ============================================================
// Parse CLI arguments
// ============================================================
function parseArgs() {
  const args = process.argv.slice(2);
  const opts = {
    server: process.env.RELAY_SERVER || 'http://localhost:8080',
    workDir: process.cwd(),
    name: os.hostname(),
    mode: 'resume',  // 'resume' or 'pty'
    sessionId: null, // Claude session ID to use
    structuredOutput: true,
    outputFormat: 'stream-json',
    verbose: false
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
      case '--session': case '--resume': case '-r':
        opts.sessionId = args[++i];
        break;
      case '--structured-output': case '--structured':
        opts.structuredOutput = args[++i].toLowerCase() !== 'false';
        break;
      case '--format': case '-f':
        opts.outputFormat = args[++i];
        break;
      case '--verbose': case '-v':
        opts.verbose = true;
        break;
      case '--help': case '-h':
        console.log(`
BTELO Coding Bridge — Claude Code CLI Manager

Usage:
  node bridge.js [options]

Options:
  -s, --server <url>         Relay server URL (default: http://localhost:8080)
  -w, --workdir <path>       Working directory for Claude Code (default: cwd)
  -n, --name <name>          Device name (default: hostname)
  -m, --mode <mode>          'resume' (spawn per command) or 'pty' (pseudo-terminal)
  -r, --session <id>         Claude session ID to resume
  --structured-output <bool> Enable structured output parsing (default: true)
  -f, --format <fmt>         Output format: stream-json or text (default: stream-json)
  -v, --verbose              Verbose output
  -h, --help                 Show this help

Flow:
  1. Bridge registers with relay server
  2. Auth code is displayed — enter in BTELO app
  3. Bridge connects to relay via WebSocket
  4. Commands from mobile are forwarded to Claude Code
  5. Claude output is streamed back to mobile (structured or raw)
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
// Claude Code event formatter
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
      if (event.result) return { stream: 'STDOUT', data: event.result };
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
// Send keystrokes directly to Claude Code console via PowerShell
// ============================================================
function sendConsoleInput(pid, text, state) {
  const scriptPath = path.join(__dirname, 'console-bridge', 'console-sender.ps1');

  // Escape text for PowerShell: double-quote and escape embedded double quotes
  const escapedText = text.replace(/"/g, '""');
  const psArgs = [
    '-ExecutionPolicy', 'Bypass',
    '-File', scriptPath,
    '-Pid', String(pid),
    '-Text', escapedText
  ];

  console.log(`[BRIDGE] Sending keystrokes to PID ${pid}...`);

  const child = spawn('powershell', psArgs, {
    stdio: ['ignore', 'pipe', 'pipe'],
    windowsHide: true
  });

  let stderr = '';
  child.stderr.on('data', (data) => {
    stderr += data.toString();
  });

  child.on('close', (code) => {
    if (code === 0) {
      console.log(`[BRIDGE] Keystrokes sent to PID ${pid}`);
      state.ws.send(JSON.stringify({
        type: 'output',
        data: `[Sent] ${text}\n`,
        stream: 'STDOUT'
      }));
    } else {
      console.error(`[BRIDGE] Console send failed (code ${code}): ${stderr.trim()}`);
      state.ws.send(JSON.stringify({
        type: 'output',
        data: `Error: Failed to send input (code ${code}). Run as Administrator?\n${stderr.trim()}\n`,
        stream: 'STDERR'
      }));
    }
  });

  child.on('error', (err) => {
    console.error(`[BRIDGE] Failed to start PowerShell: ${err.message}`);
    state.ws.send(JSON.stringify({
      type: 'output',
      data: `Error: PowerShell not available: ${err.message}\n`,
      stream: 'STDERR'
    }));
  });
}

// ============================================================
// Execute Claude Code command via --resume (fallback for non-Windows)
// ============================================================
function executeResumeCommand(state, command, onOutput, outputParser) {
  if (!state.claudeSessionId) {
    onOutput({ type: 'output', data: 'Error: No session selected\n', stream: 'STDERR' });
    return;
  }

  const args = [
    '-p', command,
    '-r', state.claudeSessionId,
    '--output-format', 'stream-json',
    '--verbose',
    '--dangerously-skip-permissions'
  ];

  console.log(`[CLAUDE] Running: claude ${args.map(a => `"${a}"`).join(' ')}`);
  console.log(`[CLAUDE] Work dir: ${state.workDir}`);

  state.processingCommand = true;

  const child = spawn('claude', args, {
    cwd: state.workDir,
    shell: true,
    stdio: ['ignore', 'pipe', 'pipe']
  });

  let buffer = '';

  child.stdout.on('data', (data) => {
    const str = data.toString();
    // Echo to bridge terminal
    process.stdout.write(str);

    if (outputParser) {
      // Use structured output parser
      outputParser.process(str);
    } else {
      // Fallback to basic JSON parsing
      buffer += str;
      const lines = buffer.split('\n');
      buffer = lines.pop();

      for (const line of lines) {
        if (line.trim()) {
          try {
            const event = JSON.parse(line);
            const formatted = formatClaudeEvent(event);
            if (formatted) {
              onOutput({ type: 'output', data: formatted.data, stream: formatted.stream });
            }
          } catch {
            onOutput({ type: 'output', data: line, stream: 'STDOUT' });
          }
        }
      }
    }
  });

  child.stderr.on('data', (data) => {
    const text = data.toString().trim();
    if (text) {
      process.stderr.write(data.toString());
      console.log(`[CLAUDE:stderr] ${text}`);
      if (outputParser) {
        outputParser.emitStructuredOutput && outputParser.emitStructuredOutput('error', text);
      } else {
        onOutput({ type: 'output', data: text, stream: 'STDERR' });
      }
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

    if (state.commandQueue.length > 0) {
      const next = state.commandQueue.shift();
      console.log(`[CLAUDE] Processing queued command (${state.commandQueue.length} remaining)`);
      executeResumeCommand(state, next, onOutput, outputParser);
    }
  });

  child.on('error', (err) => {
    console.error(`[CLAUDE] Failed to start: ${err.message}`);
    state.processingCommand = false;
    onOutput({ type: 'output', data: `Error: Failed to start Claude Code: ${err.message}`, stream: 'STDERR' });
  });
}

// ============================================================
// Handle select_session — attach to an existing Claude session
// ============================================================
function handleSelectSession(msg, state) {
  const requestedId = msg.session_id;
  if (!requestedId) {
    state.ws.send(JSON.stringify({ type: 'error', message: 'session_id required' }));
    return;
  }

  console.log(`[BRIDGE] Select session requested: ${requestedId}`);

  const discovered = discoverSessions();
  // Try to match the requested ID (could be relay session ID or Claude session ID)
  let target = discovered.find(s => s.sessionId === requestedId);

  if (!target) {
    // Fall back to auto-selected session
    if (state.claudeSessionId) {
      target = discovered.find(s => s.sessionId === state.claudeSessionId);
      console.log(`[BRIDGE] No match for "${requestedId}", using auto-selected: ${state.claudeSessionId.substring(0, 12)}...`);
    }
    if (!target) {
      state.ws.send(JSON.stringify({ type: 'error', message: 'No Claude session available' }));
      return;
    }
  }

  if (!target.hasHistory) {
    state.ws.send(JSON.stringify({ type: 'error', message: 'Session has no history' }));
    return;
  }

  stopWatching(state);

  state.claudeSessionId = target.sessionId;
  state.jsonlPath = target.jsonlPath;
  state.workDir = target.cwd;

  console.log(`[BRIDGE] Loading history from ${target.jsonlPath}`);
  const history = parseJsonlHistory(target.jsonlPath);
  console.log(`[BRIDGE] Sending ${history.length} messages to mobile`);

  state.ws.send(JSON.stringify({
    type: 'sync_history',
    session_id: state.sessionId,
    messages: history
  }));

  watchJsonlFile(state, (msg) => {
    if (state.ws && state.ws.readyState === 1) {
      state.ws.send(JSON.stringify({
        type: 'new_message',
        session_id: state.sessionId,
        message: msg
      }));
    }
  });

  state.ws.send(JSON.stringify({
    type: 'status',
    connected: true,
    claude_code: state.claudeInfo,
    session_id: state.sessionId,
    message: `Connected to session (${history.length} messages loaded)`
  }));
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
    claudeSessionId: opts.sessionId || null,
    jsonlPath: null,
    workDir: opts.workDir,
    fileWatcher: null,
    watchDebounce: null,
    processingCommand: false,
    commandQueue: [],
    outputParser: null,
    structuredOutput: opts.structuredOutput && OutputParser !== null
  };

  // Create output parser if enabled
  if (state.structuredOutput && OutputParser) {
    state.outputParser = new OutputParser({
      debug: opts.verbose,
      onMessage: (msg) => {
        if (state.ws && state.ws.readyState === 1) {
          state.ws.send(JSON.stringify(msg));
        }
      }
    });
    console.log('[BRIDGE] Structured output enabled');
  } else if (!OutputParser) {
    console.log('[BRIDGE] output-parser.js not found, using basic mode');
  }

  console.log('');
  console.log('='.repeat(50));
  console.log('  BTELO Coding Bridge');
  console.log('='.repeat(50));
  console.log(`  Server:    ${opts.server}`);
  console.log(`  Work Dir:  ${opts.workDir}`);
  console.log(`  Device:    ${opts.name}`);
  console.log(`  Mode:      ${opts.mode}`);
  if (opts.sessionId) {
    console.log(`  Session:   ${opts.sessionId}`);
  }
  if (claudeInfo.installed) {
    console.log(`  Claude:    ${claudeInfo.version}`);
  } else {
    console.log('  Claude:    NOT FOUND');
  }
  console.log('='.repeat(50));
  console.log('');

  // Step 1: Register with relay
  const authCode = String(Math.floor(100000 + Math.random() * 900000));
  console.log('[BRIDGE] Generating auth code...');
  console.log('');

  console.log('  ╔══════════════════════════════════════╗');
  console.log('  ║          AUTH CODE                   ║');
  console.log(`  ║            ${authCode}                      ║`);
  console.log('  ║     Enter this code in the app       ║');
  console.log('  ╚══════════════════════════════════════╝');
  console.log('');

  console.log(`[BRIDGE] Registering with relay server (auth: ${authCode})...`);
  let regRes;
  try {
    regRes = await httpRequest(`${opts.server}/bridge/register`, 'POST', {
      device_name: opts.name,
      work_dir: opts.workDir,
      mode: opts.mode,
      auth_code: authCode
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

  // Step 2: Show connection summary
  console.log(`  Relay:     ${opts.server}`);
  console.log(`  Session:   ${state.sessionId}`);
  console.log(`  App URL:   ${state.connectUrl}`);
  console.log('='.repeat(50));
  console.log('  Waiting for mobile to connect via auth code...');
  console.log('');

  // Step 3: Discover existing sessions and auto-select the best match
  const discovered = discoverSessions();
  console.log(`  Found ${discovered.length} Claude session(s):`);
  for (const s of discovered) {
    const status = s.isAlive ? '● active' : '○ closed';
    console.log(`    ${status}  ${s.sessionId.substring(0, 12)}...  ${s.messageCount} msgs  ${s.cwd}`);
  }
  console.log('');

  // Auto-select: match by cwd, prefer alive processes, then most recent
  if (!state.claudeSessionId) {
    const normalizedWorkDir = path.resolve(opts.workDir);
    const candidates = discovered
      .filter(s => s.hasHistory && path.resolve(s.cwd) === normalizedWorkDir);
    const bestMatch = candidates.find(s => s.isAlive) || candidates[0];

    if (bestMatch) {
      state.claudeSessionId = bestMatch.sessionId;
      state.jsonlPath = bestMatch.jsonlPath;
      state.workDir = bestMatch.cwd;
      state.claudePid = bestMatch.pid;
      const status = bestMatch.isAlive ? 'active' : 'closed';
      console.log(`  Auto-selected session: ${bestMatch.sessionId.substring(0, 12)}... (${status}, ${bestMatch.messageCount} msgs, PID: ${bestMatch.pid})`);
      console.log('');
    }
  } else {
    // Use specified session
    const target = discovered.find(s => s.sessionId === state.claudeSessionId);
    if (target && target.hasHistory) {
      state.jsonlPath = target.jsonlPath;
      state.workDir = target.cwd;
    }
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
          if (state.claudeSessionId && state.jsonlPath) {
            const history = parseJsonlHistory(state.jsonlPath);
            console.log(`[BRIDGE] Auto-sending history: ${state.claudeSessionId.substring(0, 12)}... (${history.length} msgs)`);
            state.ws.send(JSON.stringify({
              type: 'sync_history',
              session_id: state.sessionId,
              messages: history
            }));
            watchJsonlFile(state, (newMsg) => {
              if (state.ws && state.ws.readyState === 1) {
                state.ws.send(JSON.stringify({
                  type: 'new_message',
                  session_id: state.sessionId,
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

        // Send keystrokes directly to Claude Code console via PowerShell
        if (state.claudePid && isProcessAlive(state.claudePid)) {
          sendConsoleInput(state.claudePid, msg.content, state);
        } else {
          // Fallback: try to find a Claude process and send to it
          const discovered = discoverSessions();
          const alive = discovered.find(s => s.isAlive);
          if (alive && alive.pid) {
            state.claudePid = alive.pid;
            state.claudeSessionId = alive.sessionId;
            sendConsoleInput(alive.pid, msg.content, state);
          } else {
            state.ws.send(JSON.stringify({
              type: 'output',
              data: 'Error: No running Claude Code session found.\n',
              stream: 'STDERR'
            }));
          }
        }
        break;

      case 'select_session':
        handleSelectSession(msg, state);
        break;

      case 'publicKey':
        break;

      case 'permission_response': {
        const permDir = path.join(os.homedir(), '.btelo', 'permissions');
        const permFile = path.join(permDir, `btelo-permission-${msg.session_id || state.sessionId}`);
        try {
          fs.mkdirSync(permDir, { recursive: true });
          fs.writeFileSync(permFile, msg.decision || 'deny');
          console.log(`[BRIDGE] Permission response: ${msg.decision} for session ${msg.session_id}`);
        } catch (e) {
          console.error(`[BRIDGE] Failed to write permission response:`, e.message);
        }
        break;
      }

      case 'keyRotation':
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
    setTimeout(() => process.exit(0), 1000);
  });

  state.ws.on('error', (err) => {
    console.error(`[BRIDGE] WebSocket error: ${err.message}`);
  });

  // Graceful shutdown
  process.on('SIGINT', () => {
    console.log('\n[BRIDGE] Shutting down...');
    stopWatching(state);
    state.ws.close();
    setTimeout(() => process.exit(0), 500);
  });
}

main().catch(err => {
  console.error(`[BRIDGE] Fatal error: ${err.message}`);
  process.exit(1);
});
