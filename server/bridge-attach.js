#!/usr/bin/env node

/**
 * BTELO Coding Windows Attach Bridge
 *
 * Windows-only bridge for the user's target workflow:
 * 1. User opens a normal Claude Code terminal first.
 * 2. Bridge attaches to that running Claude session.
 * 3. Android and terminal stay synchronized through Claude's transcript JSONL.
 * 4. Phone input is injected into the same visible Claude terminal.
 */

const WebSocket = require('ws');
const http = require('http');
const os = require('os');
const path = require('path');
const { execSync } = require('child_process');

const { discoverSessions, selectBestSession, isProcessAlive } = require('./session-detector');
const { TranscriptWatcher, toBridgeMessage } = require('./transcript-watcher');
const WindowsConsoleInputAdapter = require('./input-adapters/windows-console');
let OutputParser = null;
try { OutputParser = require('./output-parser.js'); } catch { /* optional */ }

function parseArgs() {
  const args = process.argv.slice(2);
  const opts = {
    server: process.env.RELAY_SERVER || 'http://localhost:8080',
    workDir: process.cwd(),
    name: os.hostname(),
    sessionId: null,
    verbose: false,
    historyLimit: 120
  };

  for (let i = 0; i < args.length; i++) {
    switch (args[i]) {
      case '--server': case '-s': opts.server = args[++i]; break;
      case '--workdir': case '-w': opts.workDir = args[++i]; break;
      case '--name': case '-n': opts.name = args[++i]; break;
      case '--session': case '-r': opts.sessionId = args[++i]; break;
      case '--history-limit': opts.historyLimit = Number(args[++i]) || 120; break;
      case '--verbose': case '-v': opts.verbose = true; break;
      case '--help': case '-h':
        console.log(`
BTELO Coding Windows Attach Bridge

Usage:
  node bridge-attach.js --workdir <project> [--server http://localhost:8080]
  node bridge-attach.js --session <claude_session_id>

Workflow:
  1. Open Claude Code normally in a Windows terminal.
  2. Run this bridge with the same --workdir.
  3. Enter the displayed 6-digit auth code in the Android app.

This bridge keeps the visible Claude terminal and phone synchronized by using
Claude's transcript JSONL as the source of truth and injecting phone input into
the existing Windows console.
`);
        process.exit(0);
    }
  }
  return opts;
}

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
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        try { resolve({ status: res.statusCode, body: JSON.parse(data) }); }
        catch { resolve({ status: res.statusCode, body: data }); }
      });
    });
    req.on('error', reject);
    if (body) req.write(JSON.stringify(body));
    req.end();
  });
}

function detectClaudeCode() {
  try {
    const result = execSync('where claude', { encoding: 'utf-8', timeout: 5000 }).trim();
    const version = execSync('claude --version', { encoding: 'utf-8', timeout: 5000 }).trim();
    return { installed: true, path: result.split('\n')[0], version };
  } catch {
    return { installed: false, path: null, version: null };
  }
}

function sendWs(state, message) {
  if (state.ws && state.ws.readyState === 1) {
    state.ws.send(JSON.stringify(message));
  }
}

function printSessions(sessions) {
  console.log(`  Found ${sessions.length} Claude session(s):`);
  for (const session of sessions) {
    const status = session.isAlive ? '● active' : '○ closed';
    const marker = session.hasHistory ? `${session.messageCount} events` : 'no transcript';
    console.log(`    ${status}  ${session.sessionId.substring(0, 12)}...  PID:${session.pid || '-'}  ${marker}  ${session.cwd}`);
  }
  console.log('');
}

function attachToSession(state, target) {
  if (!target || !target.hasHistory) {
    sendWs(state, { type: 'error', message: 'No attachable Claude Code session with transcript found' });
    return false;
  }

  if (state.watcher) state.watcher.stop();

  state.claudeSession = target;
  state.claudeSessionId = target.sessionId;
  state.claudePid = target.pid;
  state.workDir = target.cwd;
  state.transcriptPath = target.transcriptPath;

  console.log(`[ATTACH] Attached to Claude session ${target.sessionId.substring(0, 12)}...`);
  console.log(`[ATTACH] PID: ${target.pid}, alive: ${target.isAlive}`);
  console.log(`[ATTACH] Transcript: ${target.transcriptPath}`);

  state.watcher = new TranscriptWatcher({
    filePath: target.transcriptPath,
    relaySessionId: state.sessionId,
    historyLimit: state.historyLimit,
    onMessage: (message) => sendWs(state, message),
    onParsedMessage: (parsed) => {
      if (parsed.isFromUser) {
        state.pendingInputs = state.pendingInputs.filter((pending) => {
          if (pending.confirmed) return false;
          const matched = normalizeInput(pending.content) === normalizeInput(parsed.content);
          if (matched) {
            pending.confirmed = true;
            console.log(`[SYNC] Phone input confirmed in transcript: ${parsed.content.substring(0, 60)}`);
          }
          return !matched;
        });
      }
    }
  });

  const history = state.watcher.loadHistory();
  const simpleHistory = history
    .filter((item) => item.msgType === 'text')
    .map((item) => ({
      id: item.id,
      content: item.content,
      isFromUser: item.isFromUser,
      timestamp: item.timestamp
    }));

  sendWs(state, {
    type: 'sync_history',
    session_id: state.sessionId,
    messages: simpleHistory
  });

  for (const item of history.filter((entry) => entry.msgType !== 'text')) {
    sendWs(state, toBridgeMessage(state.sessionId, item));
  }

  state.watcher.start();

  sendWs(state, {
    type: 'status',
    connected: true,
    session_id: state.sessionId,
    claude_session_id: target.sessionId,
    claude_code: state.claudeInfo,
    bridge_mode: 'windows_attach',
    message: `Attached to Claude terminal (${simpleHistory.length} text messages loaded)`
  });

  return true;
}

function normalizeInput(value) {
  return String(value || '').replace(/[\r\n]+/g, ' ').trim();
}

async function handleCommand(state, content) {
  const text = normalizeInput(content);
  if (!text) return;

  if (!state.claudeSession || !state.claudePid || !isProcessAlive(state.claudePid)) {
    const { session } = selectBestSession({ workDir: state.workDir, sessionId: state.claudeSessionId });
    if (!session || !session.isAlive) {
      sendWs(state, {
        type: 'output',
        data: 'Error: No running Claude Code terminal found for attach mode.\n',
        stream: 'STDERR'
      });
      return;
    }
    attachToSession(state, session);
  }

  const clientMessageId = `phone-${Date.now()}`;
  state.pendingInputs.push({ id: clientMessageId, content: text, createdAt: Date.now(), confirmed: false });

  sendWs(state, {
    type: 'status',
    connected: true,
    message: 'Injecting input into Claude terminal...',
    pending_message_id: clientMessageId
  });

  try {
    await state.inputAdapter.sendText(state.claudeSession, text);
    console.log(`[INPUT] Injected phone input into Claude terminal: ${text.substring(0, 80)}`);
    // Do not echo a fake user message here. The transcript watcher will confirm
    // and mirror the real terminal message so phone and terminal share one source of truth.
  } catch (err) {
    state.pendingInputs = state.pendingInputs.filter((item) => item.id !== clientMessageId);
    sendWs(state, {
      type: 'output',
      data: `Error: Failed to inject input into Claude terminal: ${err.message}\n`,
      stream: 'STDERR'
    });
  }
}

function createOutputParser(state, verbose) {
  if (!OutputParser || typeof OutputParser !== 'function') return null;
  try {
    return new OutputParser({
      debug: verbose,
      onMessage: (message) => sendWs(state, message)
    });
  } catch (err) {
    console.warn('[BRIDGE] Failed to create output parser:', err.message);
    return null;
  }
}

async function main() {
  const opts = parseArgs();

  if (process.platform !== 'win32') {
    console.error('[BRIDGE] bridge-attach.js is Windows-only. Use legacy bridge.js or a tmux adapter on other platforms.');
    process.exit(1);
  }

  const claudeInfo = detectClaudeCode();
  const { session: selectedSession, sessions } = selectBestSession({ workDir: opts.workDir, sessionId: opts.sessionId });

  const state = {
    sessionId: null,
    bridgeToken: null,
    connectToken: null,
    connectUrl: null,
    ws: null,
    workDir: opts.workDir,
    historyLimit: opts.historyLimit,
    claudeInfo,
    claudeSession: selectedSession,
    claudeSessionId: selectedSession && selectedSession.sessionId,
    claudePid: selectedSession && selectedSession.pid,
    transcriptPath: selectedSession && selectedSession.transcriptPath,
    watcher: null,
    pendingInputs: [],
    inputAdapter: new WindowsConsoleInputAdapter(),
    outputParser: null
  };

  state.outputParser = createOutputParser(state, opts.verbose);

  console.log('');
  console.log('='.repeat(58));
  console.log('  BTELO Coding Windows Attach Bridge');
  console.log('='.repeat(58));
  console.log(`  Server:    ${opts.server}`);
  console.log(`  Work Dir:  ${opts.workDir}`);
  console.log(`  Device:    ${opts.name}`);
  console.log(`  Mode:      windows_attach`);
  console.log(`  Claude:    ${claudeInfo.installed ? claudeInfo.version : 'NOT FOUND'}`);
  console.log('');
  printSessions(sessions);

  if (!selectedSession) {
    console.log('[ATTACH] No matching Claude session found yet. Start Claude Code in this workdir, then connect or send a command.');
  } else {
    console.log(`[ATTACH] Initial target: ${selectedSession.sessionId.substring(0, 12)}... PID:${selectedSession.pid}`);
  }

  const authCode = String(Math.floor(100000 + Math.random() * 900000));
  console.log('');
  console.log('  ╔══════════════════════════════════════╗');
  console.log('  ║          AUTH CODE                   ║');
  console.log(`  ║            ${authCode}                      ║`);
  console.log('  ║     Enter this code in the app       ║');
  console.log('  ╚══════════════════════════════════════╝');
  console.log('');

  let regRes;
  try {
    regRes = await httpRequest(`${opts.server}/bridge/register`, 'POST', {
      device_name: opts.name,
      work_dir: opts.workDir,
      mode: 'windows_attach',
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

  console.log(`[BRIDGE] Registered relay session: ${state.sessionId}`);
  console.log(`  App URL:   ${state.connectUrl}`);
  console.log('='.repeat(58));
  console.log('  Waiting for mobile to connect...');
  console.log('');

  state.ws = new WebSocket(regRes.body.ws_url);

  state.ws.on('open', () => console.log('[BRIDGE] WebSocket connected to relay'));

  state.ws.on('message', async (data) => {
    let msg = null;
    try { msg = JSON.parse(data.toString()); }
    catch {
      console.error('[BRIDGE] Invalid JSON:', data.toString());
      return;
    }

    switch (msg.type) {
      case 'status':
        if (msg.connected && msg.role === 'bridge') {
          console.log('[BRIDGE] Relay confirmed bridge connection');
        } else if (msg.connected && msg.peer === 'mobile') {
          console.log('[BRIDGE] Mobile connected via relay');
          const { session } = selectBestSession({ workDir: opts.workDir, sessionId: state.claudeSessionId });
          if (session) attachToSession(state, session);
          else sendWs(state, { type: 'error', message: 'No Claude Code session available. Start Claude Code first.' });
        } else if (!msg.connected && msg.peer === 'mobile') {
          console.log('[BRIDGE] Mobile disconnected');
        }
        break;

      case 'select_session': {
        const requested = msg.session_id;
        const { session } = selectBestSession({ workDir: opts.workDir, sessionId: requested === state.sessionId ? state.claudeSessionId : requested });
        if (session) attachToSession(state, session);
        else sendWs(state, { type: 'error', message: 'Requested Claude session not found' });
        break;
      }

      case 'command':
        if (msg.content === 'ping') {
          sendWs(state, { type: 'command', content: 'pong', inputType: 'TEXT' });
        } else {
          await handleCommand(state, msg.content);
        }
        break;

      case 'keyRotation':
      case 'publicKey':
        // Encryption handshake is currently plaintext in the Android client.
        break;

      default:
        console.log(`[BRIDGE] Unhandled message type: ${msg.type}`);
    }
  });

  state.ws.on('close', (code, reason) => {
    console.log(`[BRIDGE] WebSocket closed: ${code} ${reason}`);
    if (state.watcher) state.watcher.stop();
    process.exit(0);
  });

  state.ws.on('error', (err) => console.error(`[BRIDGE] WebSocket error: ${err.message}`));

  process.on('SIGINT', () => {
    console.log('\n[BRIDGE] Shutting down...');
    if (state.watcher) state.watcher.stop();
    if (state.ws) state.ws.close();
    setTimeout(() => process.exit(0), 500);
  });
}

main().catch((err) => {
  console.error(`[BRIDGE] Fatal error: ${err.stack || err.message}`);
  process.exit(1);
});
