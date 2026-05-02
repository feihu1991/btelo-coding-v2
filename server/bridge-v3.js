#!/usr/bin/env node

const WebSocket = require('ws');
const http = require('http');
const os = require('os');
const { execSync } = require('child_process');

const { selectClaudeSession, isProcessAlive } = require('./v3/claude-session-index');
const { loadTranscriptEvents, snapshotMessage } = require('./v3/transcript-events');
const TranscriptTail = require('./v3/transcript-tail');
const WindowsConsoleInput = require('./v3/windows-console-input');

function parseArgs() {
  const args = process.argv.slice(2);
  const opts = {
    server: process.env.RELAY_SERVER || 'http://localhost:8080',
    workDir: process.cwd(),
    name: os.hostname(),
    sessionId: null,
    historyLimit: 240
  };

  for (let i = 0; i < args.length; i++) {
    switch (args[i]) {
      case '--server': case '-s': opts.server = args[++i]; break;
      case '--workdir': case '-w': opts.workDir = args[++i]; break;
      case '--name': case '-n': opts.name = args[++i]; break;
      case '--session': case '-r': opts.sessionId = args[++i]; break;
      case '--history-limit': opts.historyLimit = Number(args[++i]) || 240; break;
      case '--help': case '-h':
        console.log(`
BTELO Coding Bridge v3

Usage:
  node bridge-v3.js --workdir <project> [--server http://localhost:8080]
  node bridge-v3.js --session <claude_session_id>

This bridge is rebuilt around a single rule: Claude Code's transcript JSONL is
the canonical display source. Phone input is injected into the visible Claude
terminal, and the phone UI only commits messages after transcript confirmation.
`);
        process.exit(0);
    }
  }

  return opts;
}

function httpRequest(url, method, body) {
  return new Promise((resolve, reject) => {
    const urlObj = new URL(url);
    const req = http.request({
      hostname: urlObj.hostname,
      port: urlObj.port,
      path: urlObj.pathname + urlObj.search,
      method,
      headers: { 'Content-Type': 'application/json' }
    }, (res) => {
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
    const where = process.platform === 'win32' ? 'where claude' : 'which claude';
    const binary = execSync(where, { encoding: 'utf-8', timeout: 5000 }).trim().split(/\r?\n/)[0];
    const version = execSync('claude --version', { encoding: 'utf-8', timeout: 5000 }).trim();
    return { installed: true, path: binary, version };
  } catch {
    return { installed: false, path: null, version: null };
  }
}

function sendWs(state, message) {
  if (state.ws && state.ws.readyState === WebSocket.OPEN) {
    state.ws.send(JSON.stringify(message));
  }
}

function printSessionList(sessions) {
  console.log(`  Claude sessions: ${sessions.length}`);
  for (const session of sessions) {
    const status = session.isAlive ? 'active' : 'closed';
    const history = session.hasTranscript ? `${session.eventCount} transcript lines` : 'no transcript';
    console.log(`    ${status.padEnd(6)} ${session.sessionId.substring(0, 12)}... PID:${session.pid || '-'} ${history} ${session.cwd}`);
  }
}

function attachCanonicalSession(state, target) {
  if (!target || !target.hasTranscript) {
    sendWs(state, { type: 'error', message: 'No Claude transcript is available for this session' });
    return false;
  }

  if (state.tail) state.tail.stop();

  const events = loadTranscriptEvents(target.transcriptPath, state.historyLimit);
  const nextSeq = events.length ? events[events.length - 1].seq + 1 : 0;

  state.claudeSession = target;
  state.claudeSessionId = target.sessionId;
  state.claudePid = target.pid;
  state.workDir = target.cwd;
  state.transcriptPath = target.transcriptPath;

  console.log(`[BRIDGE:v3] Attached ${target.sessionId.substring(0, 12)}... PID:${target.pid} events:${events.length}`);

  sendWs(state, snapshotMessage({
    relaySessionId: state.relaySessionId,
    claudeSessionId: target.sessionId,
    events,
    cursor: nextSeq - 1
  }));

  state.tail = new TranscriptTail({
    filePath: target.transcriptPath,
    relaySessionId: state.relaySessionId,
    claudeSessionId: target.sessionId,
    startSeq: nextSeq,
    onDelta: (message) => sendWs(state, message),
    onParsedEvent: (event) => {
      if (event.role === 'user') confirmPendingInput(state, event);
    }
  });
  state.tail.start();

  sendWs(state, {
    type: 'bridge_status',
    session_id: state.relaySessionId,
    claude_session_id: target.sessionId,
    connected: true,
    mode: 'canonical_transcript',
    input_mode: process.platform === 'win32' ? 'win32_console' : 'unsupported',
    message: 'Attached to Claude transcript'
  });

  return true;
}

function normalizeInput(value) {
  return String(value || '').replace(/[\r\n]+/g, ' ').trim();
}

function confirmPendingInput(state, event) {
  const normalized = normalizeInput(event.content);
  const match = state.pendingInputs.find((input) => normalizeInput(input.content) === normalized);
  if (!match) return;

  state.pendingInputs = state.pendingInputs.filter((input) => input.id !== match.id);
  sendWs(state, {
    type: 'input_status',
    client_message_id: match.id,
    status: 'committed',
    transcript_event_id: event.id,
    timestamp: Date.now()
  });
}

async function handleCommand(state, content, clientMessageId) {
  const text = normalizeInput(content);
  if (!text) return;

  if (!state.claudeSession || !state.claudePid || !isProcessAlive(state.claudePid)) {
    const { session } = selectClaudeSession({ workDir: state.workDir, sessionId: state.claudeSessionId });
    if (!session || !session.isAlive) {
      sendWs(state, {
        type: 'input_status',
        client_message_id: clientMessageId || null,
        status: 'failed',
        message: 'No running Claude Code terminal found',
        timestamp: Date.now()
      });
      return;
    }
    attachCanonicalSession(state, session);
  }

  const id = clientMessageId || `phone-${Date.now()}`;
  state.pendingInputs.push({ id, content: text, createdAt: Date.now() });
  sendWs(state, {
    type: 'input_status',
    client_message_id: id,
    status: 'injecting',
    timestamp: Date.now()
  });

  try {
    const result = await state.input.sendText(state.claudeSession, text);
    sendWs(state, {
      type: 'input_status',
      client_message_id: id,
      status: 'injected',
      input_mode: result.mode,
      timestamp: Date.now()
    });
  } catch (err) {
    state.pendingInputs = state.pendingInputs.filter((input) => input.id !== id);
    sendWs(state, {
      type: 'input_status',
      client_message_id: id,
      status: 'failed',
      message: err.message,
      timestamp: Date.now()
    });
  }
}

async function main() {
  const opts = parseArgs();
  const claudeInfo = detectClaudeCode();
  const { session, sessions } = selectClaudeSession({ workDir: opts.workDir, sessionId: opts.sessionId });

  const state = {
    relaySessionId: null,
    ws: null,
    workDir: opts.workDir,
    historyLimit: opts.historyLimit,
    claudeInfo,
    claudeSession: session,
    claudeSessionId: session && session.sessionId,
    claudePid: session && session.pid,
    transcriptPath: session && session.transcriptPath,
    tail: null,
    input: new WindowsConsoleInput(),
    pendingInputs: []
  };

  console.log('');
  console.log('='.repeat(58));
  console.log('  BTELO Coding Bridge v3');
  console.log('='.repeat(58));
  console.log(`  Relay:    ${opts.server}`);
  console.log(`  Workdir:  ${opts.workDir}`);
  console.log(`  Claude:   ${claudeInfo.installed ? claudeInfo.version : 'NOT FOUND'}`);
  printSessionList(sessions);

  const authCode = String(Math.floor(100000 + Math.random() * 900000));
  console.log('');
  console.log(`  Auth code: ${authCode}`);
  console.log('');

  let registration;
  try {
    registration = await httpRequest(`${opts.server}/bridge/register`, 'POST', {
      device_name: opts.name,
      work_dir: opts.workDir,
      mode: 'canonical_transcript_v3',
      auth_code: authCode
    });
  } catch (err) {
    console.error(`[BRIDGE:v3] Cannot reach relay ${opts.server}: ${err.message}`);
    process.exit(1);
  }

  if (registration.status !== 200 || !registration.body.success) {
    console.error(`[BRIDGE:v3] Registration failed: ${JSON.stringify(registration.body)}`);
    process.exit(1);
  }

  state.relaySessionId = registration.body.session_id;
  console.log(`  Relay session: ${state.relaySessionId}`);
  console.log(`  App URL:       ${registration.body.connect_url}`);
  console.log('='.repeat(58));

  state.ws = new WebSocket(registration.body.ws_url);
  state.ws.on('open', () => console.log('[BRIDGE:v3] WebSocket connected'));

  state.ws.on('message', async (raw) => {
    let msg = null;
    try { msg = JSON.parse(raw.toString()); } catch { return; }

    switch (msg.type) {
      case 'status':
        if (msg.connected && msg.peer === 'mobile') {
          const selected = selectClaudeSession({ workDir: opts.workDir, sessionId: state.claudeSessionId }).session;
          if (selected) attachCanonicalSession(state, selected);
          else sendWs(state, { type: 'error', message: 'Start Claude Code in this project before connecting.' });
        }
        break;
      case 'select_session': {
        const selected = selectClaudeSession({ workDir: opts.workDir, sessionId: msg.session_id }).session;
        if (selected) attachCanonicalSession(state, selected);
        else sendWs(state, { type: 'error', message: 'Requested Claude session not found' });
        break;
      }
      case 'command':
        await handleCommand(state, msg.content, msg.client_message_id);
        break;
      default:
        break;
    }
  });

  state.ws.on('close', (code, reason) => {
    console.log(`[BRIDGE:v3] WebSocket closed: ${code} ${reason}`);
    if (state.tail) state.tail.stop();
    process.exit(0);
  });

  state.ws.on('error', (err) => console.error(`[BRIDGE:v3] WebSocket error: ${err.message}`));

  process.on('SIGINT', () => {
    if (state.tail) state.tail.stop();
    if (state.ws) state.ws.close();
    setTimeout(() => process.exit(0), 300);
  });
}

main().catch((err) => {
  console.error(`[BRIDGE:v3] Fatal error: ${err.stack || err.message}`);
  process.exit(1);
});
