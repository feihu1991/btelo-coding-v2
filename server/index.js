/**
 * =============================================================================
 * BTELO Coding Server — LEGACY VERSION
 * =============================================================================
 * 
 * !!!!!!! IMPORTANT: THIS FILE IS DEPRECATED !!!!!!!
 * 
 * This file is kept for backward compatibility only.
 * For new installations, please use the relay + bridge architecture:
 * 
 *   1. Start relay server:  node relay.js
 *   2. Start bridge:        node bridge.js
 *   3. Or use PTY mode:      node pty-bridge.js --session <session_id>
 * 
 * New features in v2.0:
 * - PTY (Pseudo-Terminal) support for interactive sessions
 * - Structured output parsing for rich mobile rendering
 * - Heartbeat mechanism for connection stability
 * - Session state broadcasting
 * - Better separation of concerns (relay vs bridge)
 * 
 * If you still need to use this legacy version:
 *   node index.js
 * 
 * =============================================================================
 */

const express = require('express');
const cors = require('cors');
const crypto = require('crypto');
const { v4: uuidv4 } = require('uuid');
const { WebSocketServer } = require('ws');
const http = require('http');
const { spawn, execSync } = require('child_process');
const os = require('os');
const fs = require('fs');
const path = require('path');
const qrcode = require('qrcode-terminal');

const app = express();
app.use(cors());
app.use(express.json());

// ============================================================
// Auto-detect Claude Code
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

const claudeInfo = detectClaudeCode();

// ============================================================
// Get local IP address
// ============================================================
function getLocalIP() {
  if (process.env.PUBLIC_IP) {
    return process.env.PUBLIC_IP;
  }
  const interfaces = os.networkInterfaces();
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        return iface.address;
      }
    }
  }
  return '127.0.0.1';
}

function getLanIP() {
  const interfaces = os.networkInterfaces();
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        return iface.address;
      }
    }
  }
  return '127.0.0.1';
}

// ============================================================
// Session discovery
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
    console.log('[SESSIONS] Sessions directory not found:', SESSIONS_DIR);
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
      console.error(`[SESSIONS] Error reading ${file}:`, e.message);
    }
  }

  sessions.sort((a, b) => (b.startedAt || 0) - (a.startedAt || 0));
  return sessions;
}

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
// JSONL file watcher
// ============================================================
function watchJsonlFile(session) {
  const filePath = session.jsonlPath;
  if (!filePath || !fs.existsSync(filePath)) return;

  let lastSize = fs.statSync(filePath).size;

  session.fileWatcher = fs.watch(filePath, (eventType) => {
    if (eventType !== 'change') return;

    clearTimeout(session.watchDebounce);
    session.watchDebounce = setTimeout(() => {
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
            if (msg && session.mobileWs && session.mobileWs.readyState === 1) {
              session.mobileWs.send(JSON.stringify({
                type: 'new_message',
                session_id: session.claudeSessionId,
                message: msg
              }));
            }
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

function stopWatching(session) {
  if (session.fileWatcher) {
    session.fileWatcher.close();
    session.fileWatcher = null;
  }
  if (session.watchDebounce) {
    clearTimeout(session.watchDebounce);
    session.watchDebounce = null;
  }
}

// ============================================================
// Data stores
// ============================================================
const connectTokens = new Map();
const sessions = new Map();
const CONNECTION_TOKEN = crypto.randomBytes(16).toString('hex');

// ============================================================
// API: Get connection info (simple pairing)
// ============================================================
app.get('/connect', (req, res) => {
  const token = CONNECTION_TOKEN;
  const localIP = getLocalIP();
  const PORT = process.env.PORT || 8080;

  console.log(`[HTTP] Mobile requesting connect info`);

  res.json({
    success: true,
    token: token,
    ws_url: `ws://${localIP}:${PORT}/ws?token=${token}`,
    claude_code: claudeInfo,
    available_sessions: discoverSessions()
  });
});

// ============================================================
// API: List sessions
// ============================================================
app.get('/sessions', (req, res) => {
  res.json({ sessions: discoverSessions() });
});

// ============================================================
// API: Status
// ============================================================
app.get('/status', (req, res) => {
  res.json({
    status: 'running',
    version: 'legacy',
    claude_code: claudeInfo,
    active_sessions: sessions.size
  });
});

// ============================================================
// API: Restart
// ============================================================
app.post('/restart', (req, res) => {
  console.log('[RESTART] Server restart requested (legacy)');
  res.json({ success: true, message: 'Server is restarting...' });

  setTimeout(() => {
    server.close(() => {
      const child = spawn(process.argv[0], process.argv.slice(1), {
        detached: true,
        stdio: 'inherit',
        cwd: process.cwd(),
        env: process.env
      });
      child.unref();
      process.exit(0);
    });
    setTimeout(() => process.exit(1), 3000);
  }, 500);
});

app.get('/restart', (req, res) => {
  console.log('[RESTART] Server restart requested (legacy)');
  res.json({ success: true, message: 'Server is restarting...' });

  setTimeout(() => {
    server.close(() => {
      const child = spawn(process.argv[0], process.argv.slice(1), {
        detached: true,
        stdio: 'inherit',
        cwd: process.cwd(),
        env: process.env
      });
      child.unref();
      process.exit(0);
    });
    setTimeout(() => process.exit(1), 3000);
  }, 500);
});

// ============================================================
// HTTP Server + WebSocket
// ============================================================
const server = http.createServer(app);
const wss = new WebSocketServer({ server, path: '/ws' });

wss.on('connection', (ws, req) => {
  const url = new URL(req.url, 'http://localhost');
  const token = url.searchParams.get('token');

  if (!token) {
    ws.close(4001, 'Token required');
    return;
  }

  const tokenData = connectTokens.get(token);
  if (!tokenData || !tokenData.sessionId) {
    ws.close(4001, 'Invalid token');
    return;
  }

  const sessionId = tokenData.sessionId;
  const session = sessions.get(sessionId);
  if (!session) {
    ws.close(4001, 'Session not found');
    return;
  }

  session.mobileWs = ws;
  console.log(`[WS] Mobile connected to relay session ${sessionId}`);

  ws.send(JSON.stringify({
    type: 'status',
    connected: true,
    claude_code: claudeInfo,
    message: claudeInfo.installed
      ? `Claude Code ${claudeInfo.version} ready`
      : 'Claude Code not found on this computer'
  }));

  ws.on('message', (data) => {
    try {
      const msg = JSON.parse(data.toString());
      handleWsMessage(ws, msg, session, sessionId);
    } catch (e) {
      console.error(`[WS] Parse error:`, e.message);
    }
  });

  ws.on('close', () => {
    session.mobileWs = null;
    stopWatching(session);
    session.commandQueue = [];
    session.processingCommand = false;
    console.log(`[WS] Mobile disconnected from relay session ${sessionId}`);
    sessions.delete(sessionId);
  });
});

function handleWsMessage(ws, msg, session, sessionId) {
  switch (msg.type) {
    case 'command':
      handleCommand(ws, msg, session, sessionId);
      break;
    case 'select_session':
      handleSelectSession(ws, msg, session, sessionId);
      break;
    case 'publicKey':
      break;
    case 'keyRotation':
      break;
    default:
      console.log(`[WS] Unknown message type: ${msg.type}`);
  }
}

function handleSelectSession(ws, msg, session, sessionId) {
  const claudeSessionId = msg.session_id;
  if (!claudeSessionId) {
    ws.send(JSON.stringify({ type: 'error', message: 'session_id required' }));
    return;
  }

  console.log(`[WS] Selecting Claude session: ${claudeSessionId}`);

  const discovered = discoverSessions();
  const target = discovered.find(s => s.sessionId === claudeSessionId);

  if (!target) {
    ws.send(JSON.stringify({ type: 'error', message: 'Session not found' }));
    return;
  }

  if (!target.hasHistory) {
    ws.send(JSON.stringify({ type: 'error', message: 'Session has no history' }));
    return;
  }

  stopWatching(session);

  session.claudeSessionId = claudeSessionId;
  session.jsonlPath = target.jsonlPath;
  session.workDir = target.cwd;

  const history = parseJsonlHistory(target.jsonlPath);
  console.log(`[WS] Sending ${history.length} messages to mobile`);

  ws.send(JSON.stringify({
    type: 'sync_history',
    session_id: claudeSessionId,
    messages: history
  }));

  watchJsonlFile(session);

  ws.send(JSON.stringify({
    type: 'status',
    connected: true,
    claude_code: claudeInfo,
    session_id: claudeSessionId,
    message: `Connected to session (${history.length} messages loaded)`
  }));
}

function handleCommand(ws, msg, session, sessionId) {
  if (!claudeInfo.installed) {
    ws.send(JSON.stringify({
      type: 'output',
      data: 'Error: Claude Code is not installed on this computer.\nInstall it with: npm install -g @anthropic-ai/claude-code',
      stream: 'STDERR'
    }));
    return;
  }

  if (!session.claudeSessionId) {
    ws.send(JSON.stringify({
      type: 'output',
      data: 'Error: No Claude session selected. Please select a session first.',
      stream: 'STDERR'
    }));
    return;
  }

  console.log(`[WS] Command: ${msg.content.substring(0, 80)}...`);

  if (session.processingCommand) {
    session.commandQueue.push(msg.content);
    ws.send(JSON.stringify({
      type: 'output',
      data: `[Queued] Command queued (${session.commandQueue.length} in queue)\n`,
      stream: 'STDOUT'
    }));
    return;
  }

  executeCommand(msg.content, session, sessionId);
}

function executeCommand(command, session, sessionId) {
  const args = [
    '-p', command,
    '-r', session.claudeSessionId,
    '--output-format', 'stream-json',
    '--dangerously-skip-permissions'
  ];

  console.log(`[CLAUDE] Running: claude ${args.map(a => `"${a}"`).join(' ')}`);
  console.log(`[CLAUDE] Work dir: ${session.workDir}`);
  console.log(`[CLAUDE] Session: ${session.claudeSessionId}`);

  sendToMobile(session, '[Claude Code] Processing...\n', 'STDOUT');

  session.processingCommand = true;

  const child = spawn('claude', args, {
    cwd: session.workDir,
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
          if (formatted) {
            sendToMobile(session, formatted.data, formatted.stream);
          }
        } catch {
          sendToMobile(session, line, 'STDOUT');
        }
      }
    }
  });

  child.stderr.on('data', (data) => {
    const text = data.toString().trim();
    if (text) {
      console.log(`[CLAUDE:stderr] ${text}`);
      sendToMobile(session, text, 'STDERR');
    }
  });

  child.on('close', (code) => {
    if (buffer.trim()) {
      try {
        const event = JSON.parse(buffer);
        const formatted = formatClaudeEvent(event);
        if (formatted) {
          sendToMobile(session, formatted.data, formatted.stream);
        }
      } catch {
        sendToMobile(session, buffer, 'STDOUT');
      }
    }

    console.log(`[CLAUDE] Process exited with code ${code}`);
    session.processingCommand = false;

    sendToMobile(session, `\n[Done] Exit code: ${code}\n`, 'STDOUT');

    if (session.commandQueue.length > 0) {
      const next = session.commandQueue.shift();
      console.log(`[CLAUDE] Processing queued command (${session.commandQueue.length} remaining)`);
      executeCommand(next, session, sessionId);
    }
  });

  child.on('error', (err) => {
    console.error(`[CLAUDE] Failed to start: ${err.message}`);
    session.processingCommand = false;
    sendToMobile(session, `Error: Failed to start Claude Code: ${err.message}`, 'STDERR');
  });
}

function sendToMobile(session, data, stream) {
  if (session.mobileWs && session.mobileWs.readyState === 1) {
    session.mobileWs.send(JSON.stringify({
      type: 'output',
      data: data,
      stream: stream
    }));
  }
}

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
// Start server
// ============================================================
const PORT = process.env.PORT || 8080;
const localIP = getLocalIP();

server.listen(PORT, '0.0.0.0', () => {
  const connectUrl = `btelo://${localIP}:${PORT}/${CONNECTION_TOKEN}`;
  const lanIP = getLanIP();
  const hasPublicIP = process.env.PUBLIC_IP && process.env.PUBLIC_IP !== lanIP;

  console.log('');
  console.log('='.repeat(50));
  console.log('  BTELO Coding Server (LEGACY)');
  console.log('='.repeat(50));
  console.log('');
  console.log('  !!!!! WARNING: This is the legacy server !!!!!');
  console.log('  For new installations, use:');
  console.log('    - node relay.js  (relay server)');
  console.log('    - node bridge.js (bridge CLI)');
  console.log('');
  if (hasPublicIP) {
    console.log(`  Public:    http://${localIP}:${PORT}`);
    console.log(`  LAN:       http://${lanIP}:${PORT}`);
    console.log(`  WebSocket: ws://${localIP}:${PORT}/ws`);
  } else {
    console.log(`  HTTP:      http://${localIP}:${PORT}`);
    console.log(`  WebSocket: ws://${localIP}:${PORT}/ws`);
  }
  console.log('');

  if (claudeInfo.installed) {
    console.log(`  Claude Code: ${claudeInfo.version}`);
    console.log(`  Path:        ${claudeInfo.path}`);
  } else {
    console.log('  Claude Code: NOT FOUND');
    console.log('  Install:     npm install -g @anthropic-ai/claude-code');
  }

  const discovered = discoverSessions();
  console.log('');
  console.log(`  Found ${discovered.length} session(s):`);
  for (const s of discovered) {
    const status = s.isAlive ? '● active' : '○ closed';
    console.log(`    ${status}  ${s.sessionId.substring(0, 12)}...  ${s.messageCount} msgs  ${s.cwd}`);
  }

  console.log('');
  console.log('  Scan QR code with BTELO app to connect:');
  console.log('');

  qrcode.generate(connectUrl, { small: true }, (qrText) => {
    console.log(qrText);
    console.log('');
    console.log(`  Connection URL: ${connectUrl}`);
    console.log('='.repeat(50));
    console.log('');
  });
});
