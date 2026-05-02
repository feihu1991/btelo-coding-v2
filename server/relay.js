#!/usr/bin/env node

/**
 * BTELO Coding Relay Server — pure message forwarder with enhanced features.
 * 
 * Routes JSON messages between mobile WebSocket and bridge WebSocket by session ID.
 * Zero knowledge of Claude Code, JSONL files, or session discovery.
 * 
 * Enhanced features:
 * - structured_output message type support
 * - Connection state broadcasting
 * - Heartbeat mechanism
 * - Session cleanup
 * 
 * Usage:
 *   node relay.js
 *   PORT=9000 node relay.js
 *   PUBLIC_IP=1.2.3.4 node relay.js
 */

const express = require('express');
const cors = require('cors');
const crypto = require('crypto');
const { v4: uuidv4 } = require('uuid');
const { WebSocketServer } = require('ws');
const http = require('http');
const fs = require('fs');
const path = require('path');
const os = require('os');

const app = express();
app.use(cors());
app.use(express.json());

// ============================================================
// Configuration
// ============================================================
const HEARTBEAT_INTERVAL = 30000;  // 30 seconds
const HEARTBEAT_TIMEOUT = 60000;   // 60 seconds (2x interval)
const SESSION_CLEANUP_INTERVAL = 60000;  // 60 seconds
const SESSION_MAX_AGE = 5 * 60 * 1000;  // 5 minutes

const PROJECT_ROOT = path.resolve(__dirname, '..');
const APK_SEARCH_ROOT = process.env.BTELO_APK_DIR || path.join(PROJECT_ROOT, 'app', 'build', 'outputs', 'apk');

// ============================================================
// IP detection
// ============================================================
function getLocalIP() {
  if (process.env.PUBLIC_IP) return process.env.PUBLIC_IP;
  const interfaces = os.networkInterfaces();
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) return iface.address;
    }
  }
  return '127.0.0.1';
}

function getLanIP() {
  const interfaces = os.networkInterfaces();
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) return iface.address;
    }
  }
  return '127.0.0.1';
}

// ============================================================
// Data stores
// ============================================================
const tokens = new Map();   // token -> { type, sessionId, createdAt }
const sessions = new Map(); // sessionId -> { mobileWs, bridgeWs, connectToken, bridgeToken, createdAt, bridgeInfo, mobileHeartbeat, bridgeHeartbeat }

// ============================================================
// Helper: Broadcast session state to both peers
// ============================================================
function broadcastSessionState(session) {
  const stateMsg = JSON.stringify({
    type: 'session_state',
    mobile_connected: session.mobileWs !== null,
    bridge_connected: session.bridgeWs !== null,
    timestamp: Date.now()
  });
  
  if (session.mobileWs && session.mobileWs.readyState === 1) {
    session.mobileWs.send(stateMsg);
  }
  if (session.bridgeWs && session.bridgeWs.readyState === 1) {
    session.bridgeWs.send(stateMsg);
  }
}

// ============================================================
// Helper: Forward message with type checking
// ============================================================
function forwardMessage(targetWs, senderWs, rawMessage, messageType) {
  if (targetWs && targetWs.readyState === 1) {
    targetWs.send(rawMessage);
    return true;
  } else if (senderWs && senderWs.readyState === 1) {
    // Notify sender that peer is not connected
    senderWs.send(JSON.stringify({
      type: 'status',
      connected: false,
      message: 'Peer not connected',
      failed_message_type: messageType
    }));
    return false;
  }
  return false;
}

function readAndroidVersion() {
  const gradlePath = path.join(PROJECT_ROOT, 'app', 'build.gradle.kts');
  try {
    const text = fs.readFileSync(gradlePath, 'utf-8');
    const codeMatch = text.match(/versionCode\s*=\s*(\d+)/);
    const nameMatch = text.match(/versionName\s*=\s*"([^"]+)"/);
    return {
      versionCode: codeMatch ? Number(codeMatch[1]) : 0,
      versionName: nameMatch ? nameMatch[1] : 'unknown'
    };
  } catch {
    return { versionCode: 0, versionName: 'unknown' };
  }
}

function walkFiles(root) {
  if (!fs.existsSync(root)) return [];
  const found = [];
  for (const item of fs.readdirSync(root, { withFileTypes: true })) {
    const fullPath = path.join(root, item.name);
    if (item.isDirectory()) found.push(...walkFiles(fullPath));
    else found.push(fullPath);
  }
  return found;
}

function findLatestApk() {
  const explicit = process.env.BTELO_APK_PATH;
  if (explicit && fs.existsSync(explicit)) return explicit;
  const apks = walkFiles(APK_SEARCH_ROOT)
    .filter((file) => file.toLowerCase().endsWith('.apk'))
    .map((file) => ({ file, stat: fs.statSync(file) }))
    .sort((a, b) => b.stat.mtimeMs - a.stat.mtimeMs);
  return apks.length ? apks[0].file : null;
}

function sha256File(filePath) {
  const hash = crypto.createHash('sha256');
  hash.update(fs.readFileSync(filePath));
  return hash.digest('hex');
}

// ============================================================
// API: Bridge registers itself
// ============================================================
app.post('/bridge/register', (req, res) => {
  const { device_name, work_dir, mode, auth_code, command } = req.body;

  if (!auth_code || auth_code.length !== 6) {
    return res.status(400).json({ error: 'auth_code (6 digits) required' });
  }

  const sessionId = 'sess-' + uuidv4().substring(0, 8);
  const connectToken = 'connect-' + crypto.randomBytes(16).toString('hex');
  const bridgeToken = 'bridge-' + crypto.randomBytes(16).toString('hex');

  const session = {
    sessionId,
    mobileWs: null,
    bridgeWs: null,
    connectToken,
    bridgeToken,
    authCode: auth_code,
    createdAt: Date.now(),
    bridgeInfo: {
      device_name: device_name || 'unknown',
      work_dir: work_dir || process.cwd(),
      mode: mode || 'resume',
      command: command || null
    },
    mobileHeartbeat: null,
    bridgeHeartbeat: Date.now()
  };

  sessions.set(sessionId, session);
  tokens.set(connectToken, { type: 'connect', sessionId, createdAt: Date.now() });
  tokens.set(bridgeToken, { type: 'bridge_ws', sessionId, createdAt: Date.now() });

  const localIP = getLocalIP();
  const PORT = process.env.PORT || 8080;

  console.log(`[RELAY] Bridge registered: ${sessionId} (${device_name || 'unknown'}, auth: ${auth_code})`);

  res.json({
    success: true,
    session_id: sessionId,
    bridge_token: bridgeToken,
    connect_token: connectToken,
    connect_url: `btelo://${localIP}:${PORT}/${connectToken}`,
    ws_url: `ws://${localIP}:${PORT}/bridge/ws?token=${bridgeToken}`
  });
});

// ============================================================
// API: Mobile connects via token (legacy QR flow, auth code is preferred)
// ============================================================
app.get('/connect', (req, res) => {
  const { token } = req.query;

  if (!token) {
    return res.status(400).json({ error: 'Token required' });
  }

  const tokenData = tokens.get(token);
  if (!tokenData || tokenData.type !== 'connect') {
    return res.status(401).json({ error: 'Invalid token' });
  }

  const session = sessions.get(tokenData.sessionId);
  if (!session) {
    return res.status(404).json({ error: 'Session not found' });
  }

  // Generate a WebSocket token for the mobile client
  const wsToken = 'ws-' + crypto.randomBytes(16).toString('hex');
  tokens.set(wsToken, { type: 'mobile_ws', sessionId: session.sessionId, createdAt: Date.now() });

  const localIP = getLocalIP();
  const PORT = process.env.PORT || 8080;

  console.log(`[RELAY] Mobile connecting to session ${session.sessionId}`);

  res.json({
    success: true,
    session_id: session.sessionId,
    ws_token: wsToken,
    ws_url: `ws://${localIP}:${PORT}/ws?token=${wsToken}`,
    server_address: `http://${localIP}:${PORT}`,
    bridge_info: session.bridgeInfo,
    claude_code: null,
    available_sessions: null
  });
});

// ============================================================
// API: List sessions
// ============================================================
app.get('/sessions', (req, res) => {
  const list = [];
  for (const [id, s] of sessions) {
    list.push({
      session_id: id,
      mobile_connected: s.mobileWs !== null,
      bridge_connected: s.bridgeWs !== null,
      bridge_device: s.bridgeInfo?.device_name || 'unknown',
      bridge_mode: s.bridgeInfo?.mode || 'resume',
      created_at: s.createdAt
    });
  }
  res.json({ success: true, sessions: list });
});

// ============================================================
// API: List available bridges (for mobile discovery)
// ============================================================
app.get('/bridges', (req, res) => {
  const list = [];
  const now = Date.now();
  const STALE_AGE = 5 * 60 * 1000; // 5 minutes
  for (const [id, s] of sessions) {
    // Skip if both disconnected and session is older than 5 min
    if (!s.bridgeWs && !s.mobileWs && (now - s.createdAt) > STALE_AGE) continue;
    // Skip if bridge never connected or disconnected and no mobile
    if (!s.bridgeWs && !s.mobileWs && (now - s.createdAt) > 30000) continue;
    list.push({
      id: id,
      device_name: s.bridgeInfo?.device_name || 'unknown',
      work_dir: s.bridgeInfo?.work_dir || '',
      mode: s.bridgeInfo?.mode || 'resume',
      bridge_connected: s.bridgeWs !== null,
      mobile_connected: s.mobileWs !== null,
      created_at: s.createdAt
    });
  }
  res.json({ success: true, bridges: list });
});

// ============================================================
// API: Mobile connects to bridge via auth code
// ============================================================
app.post('/bridges/:id/connect', (req, res) => {
  const { id } = req.params;
  const { auth_code } = req.body;

  if (!auth_code) {
    return res.status(400).json({ error: 'auth_code required' });
  }

  const session = sessions.get(id);
  if (!session) {
    return res.status(404).json({ error: 'Bridge not found' });
  }

  if (session.authCode !== auth_code) {
    return res.status(401).json({ error: 'Invalid auth code' });
  }

  // Generate WebSocket token for mobile client
  const wsToken = 'ws-' + crypto.randomBytes(16).toString('hex');
  tokens.set(wsToken, { type: 'mobile_ws', sessionId: session.sessionId, createdAt: Date.now() });

  const localIP = getLocalIP();
  const PORT = process.env.PORT || 8080;

  console.log(`[RELAY] Mobile authenticated to bridge ${id} via auth code`);

  res.json({
    success: true,
    session_id: session.sessionId,
    ws_token: wsToken,
    ws_url: `ws://${localIP}:${PORT}/ws?token=${wsToken}`,
    bridge_info: session.bridgeInfo
  });
});

// ============================================================
// API: Hook Callback — receives events from btelo-plugin
// ============================================================
app.post('/api/hooks/callback', (req, res) => {
  const { event, session_id, ...data } = req.body;

  if (!event) {
    return res.status(400).json({ error: 'event field required' });
  }

  console.log(`[HOOK] Event: ${event}, Session: ${session_id || 'unknown'}`);

  // Forward to connected mobile client
  for (const [id, session] of sessions) {
    if (session.mobileWs && session.mobileWs.readyState === 1) {
      session.mobileWs.send(JSON.stringify({
        type: 'hook_event',
        event: event,
        data: { session_id, ...data },
        timestamp: Date.now()
      }));
    }
  }

  res.json({ success: true, event });
});

// Permission response — mobile client approves/denies a permission request
app.post('/api/hooks/permission-response', (req, res) => {
  const { session_id, decision } = req.body;

  if (!session_id || !decision) {
    return res.status(400).json({ error: 'session_id and decision required' });
  }

  const permDir = path.join(os.homedir(), '.btelo', 'permissions');
  const permFile = path.join(permDir, `btelo-permission-${session_id}`);
  try {
    fs.mkdirSync(permDir, { recursive: true });
    fs.writeFileSync(permFile, decision);
    console.log(`[HOOK] Permission response: ${decision} for session ${session_id}`);
    res.json({ success: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// ============================================================
// API: Status
// ============================================================
app.get('/status', (req, res) => {
  let active = 0;
  for (const s of sessions.values()) {
    if (s.mobileWs || s.bridgeWs) active++;
  }
  res.json({
    status: 'running',
    active_sessions: active,
    total_sessions: sessions.size,
    uptime: process.uptime(),
    version: '2.0.0'
  });
});

// ============================================================
// API: Android APK update/distribution
// ============================================================
app.get('/app/latest', (req, res) => {
  const apkPath = findLatestApk();
  const version = readAndroidVersion();
  const currentVersionCode = Number(req.query.current_version_code || 0);

  if (!apkPath) {
    return res.json({
      success: true,
      update_available: false,
      version_code: version.versionCode,
      version_name: version.versionName,
      apk_available: false
    });
  }

  const stat = fs.statSync(apkPath);
  const PORT = process.env.PORT || 8080;
  const baseUrl = `http://${getLocalIP()}:${PORT}`;

  res.json({
    success: true,
    update_available: version.versionCode > currentVersionCode,
    apk_available: true,
    version_code: version.versionCode,
    version_name: version.versionName,
    file_name: path.basename(apkPath),
    size_bytes: stat.size,
    sha256: sha256File(apkPath),
    built_at: new Date(stat.mtimeMs).toISOString(),
    download_url: `${baseUrl}/app/apk`
  });
});

app.get('/app/apk', (req, res) => {
  const apkPath = findLatestApk();
  if (!apkPath) return res.status(404).json({ error: 'APK not found. Build the app first.' });
  res.download(apkPath, path.basename(apkPath));
});

// ============================================================
// API: Restart
// ============================================================
function handleRestart(req, res) {
  console.log('[RESTART] Server restart requested');
  res.json({ success: true, message: 'Server is restarting...' });
  setTimeout(() => {
    server.close(() => {
      const { spawn } = require('child_process');
      const child = spawn(process.argv[0], process.argv.slice(1), {
        detached: true, stdio: 'inherit', cwd: process.cwd(), env: process.env
      });
      child.unref();
      process.exit(0);
    });
    setTimeout(() => process.exit(1), 3000);
  }, 500);
}
app.post('/restart', handleRestart);
app.get('/restart', handleRestart);

// ============================================================
// HTTP Server + WebSocket servers
// ============================================================
const server = http.createServer(app);

// Mobile WebSocket: /ws?token=ws_token
const wssMobile = new WebSocketServer({ noServer: true });
// Bridge WebSocket: /bridge/ws?token=bridge_token
const wssBridge = new WebSocketServer({ noServer: true });

// Route upgrade requests to the correct WebSocket server
server.on('upgrade', (request, socket, head) => {
  const url = new URL(request.url, 'http://localhost');
  const pathname = url.pathname;

  if (pathname === '/ws') {
    wssMobile.handleUpgrade(request, socket, head, (ws) => {
      wssMobile.emit('connection', ws, request);
    });
  } else if (pathname === '/bridge/ws') {
    wssBridge.handleUpgrade(request, socket, head, (ws) => {
      wssBridge.emit('connection', ws, request);
    });
  } else {
    socket.destroy();
  }
});

// ============================================================
// Mobile WebSocket handler
// ============================================================
wssMobile.on('connection', (ws, req) => {
  const url = new URL(req.url, 'http://localhost');
  const token = url.searchParams.get('token');

  if (!token) {
    ws.close(4001, 'Token required');
    return;
  }

  const tokenData = tokens.get(token);
  if (!tokenData || tokenData.type !== 'mobile_ws') {
    ws.close(4001, 'Invalid token');
    return;
  }

  const session = sessions.get(tokenData.sessionId);
  if (!session) {
    ws.close(4001, 'Session not found');
    return;
  }

  session.mobileWs = ws;
  session.mobileHeartbeat = Date.now();
  console.log(`[RELAY] Mobile connected to session ${session.sessionId}`);

  // Notify mobile that relay is ready
  ws.send(JSON.stringify({
    type: 'status',
    connected: true,
    message: 'Relay ready',
    session_id: session.sessionId,
    bridge_info: session.bridgeInfo
  }));

  // Notify bridge that mobile connected
  if (session.bridgeWs && session.bridgeWs.readyState === 1) {
    session.bridgeWs.send(JSON.stringify({
      type: 'status',
      connected: true,
      peer: 'mobile',
      mobile_info: {
        connected_at: Date.now()
      }
    }));
  }

  // Broadcast session state
  broadcastSessionState(session);

  ws.on('message', (data) => {
    // Update heartbeat on any message
    session.mobileHeartbeat = Date.now();
    
    // Forward to bridge, preserving the raw message
    forwardMessage(session.bridgeWs, ws, data.toString(), 'mobile_to_bridge');
  });

  ws.on('close', () => {
    console.log(`[RELAY] Mobile disconnected from session ${session.sessionId}`);
    session.mobileWs = null;
    session.mobileHeartbeat = null;
    
    // Notify bridge
    if (session.bridgeWs && session.bridgeWs.readyState === 1) {
      session.bridgeWs.send(JSON.stringify({
        type: 'status',
        connected: false,
        peer: 'mobile',
        reason: 'mobile_disconnected'
      }));
    }
    
    broadcastSessionState(session);
  });

  // Heartbeat pong handler
  ws.on('pong', () => {
    session.mobileHeartbeat = Date.now();
  });
});

// ============================================================
// Bridge WebSocket handler
// ============================================================
wssBridge.on('connection', (ws, req) => {
  const url = new URL(req.url, 'http://localhost');
  const token = url.searchParams.get('token');

  if (!token) {
    ws.close(4001, 'Token required');
    return;
  }

  const tokenData = tokens.get(token);
  if (!tokenData || tokenData.type !== 'bridge_ws') {
    ws.close(4001, 'Invalid token');
    return;
  }

  const session = sessions.get(tokenData.sessionId);
  if (!session) {
    ws.close(4001, 'Session not found');
    return;
  }

  session.bridgeWs = ws;
  session.bridgeHeartbeat = Date.now();
  console.log(`[RELAY] Bridge connected to session ${session.sessionId}`);

  // Notify bridge that relay is ready
  ws.send(JSON.stringify({
    type: 'status',
    connected: true,
    role: 'bridge',
    session_id: session.sessionId,
    features: {
      structured_output: true,
      heartbeat: true,
      session_state: true
    }
  }));

  // Notify mobile that bridge connected
  if (session.mobileWs && session.mobileWs.readyState === 1) {
    session.mobileWs.send(JSON.stringify({
      type: 'status',
      connected: true,
      message: 'Bridge connected',
      bridge_info: session.bridgeInfo
    }));
  }

  // Broadcast session state
  broadcastSessionState(session);

  ws.on('message', (data) => {
    // Update heartbeat on any message
    session.bridgeHeartbeat = Date.now();
    
    let messageType = 'bridge_to_mobile';
    let msgObj;
    try {
      msgObj = JSON.parse(data.toString());
      messageType = msgObj.type || messageType;
    } catch {}
    
    // Forward to mobile, preserving the raw message
    forwardMessage(session.mobileWs, ws, data.toString(), messageType);
  });

  ws.on('close', () => {
    console.log(`[RELAY] Bridge disconnected from session ${session.sessionId}`);
    session.bridgeWs = null;
    session.bridgeHeartbeat = null;
    
    // Notify mobile
    if (session.mobileWs && session.mobileWs.readyState === 1) {
      session.mobileWs.send(JSON.stringify({
        type: 'status',
        connected: false,
        reason: 'bridge_disconnected'
      }));
    }
    
    broadcastSessionState(session);
  });

  // Heartbeat pong handler
  ws.on('pong', () => {
    session.bridgeHeartbeat = Date.now();
  });
});

// ============================================================
// Heartbeat mechanism
// ============================================================
setInterval(() => {
  const now = Date.now();
  
  for (const [id, session] of sessions) {
    // Check mobile heartbeat
    if (session.mobileWs && session.mobileWs.readyState === 1) {
      if (session.mobileHeartbeat && (now - session.mobileHeartbeat) > HEARTBEAT_TIMEOUT) {
        console.log(`[RELAY] Mobile heartbeat timeout: ${id}`);
        session.mobileWs.terminate();
      } else {
        // Send ping
        session.mobileWs.ping();
      }
    }
    
    // Check bridge heartbeat
    if (session.bridgeWs && session.bridgeWs.readyState === 1) {
      if (session.bridgeHeartbeat && (now - session.bridgeHeartbeat) > HEARTBEAT_TIMEOUT) {
        console.log(`[RELAY] Bridge heartbeat timeout: ${id}`);
        session.bridgeWs.terminate();
      } else {
        // Send ping
        session.bridgeWs.ping();
      }
    }
  }
}, HEARTBEAT_INTERVAL);

// ============================================================
// Session cleanup: remove stale sessions
// ============================================================
setInterval(() => {
  const now = Date.now();
  for (const [id, session] of sessions) {
    const bothDisconnected = !session.mobileWs && !session.bridgeWs;
    const age = now - session.createdAt;
    if (bothDisconnected && age > SESSION_MAX_AGE) {
      sessions.delete(id);
      tokens.delete(session.connectToken);
      tokens.delete(session.bridgeToken);
      console.log(`[RELAY] Cleaned up stale session: ${id}`);
    }
  }
}, SESSION_CLEANUP_INTERVAL);

// ============================================================
// Start server
// ============================================================
const PORT = process.env.PORT || 8080;
const localIP = getLocalIP();

server.listen(PORT, '0.0.0.0', () => {
  const lanIP = getLanIP();
  const hasPublicIP = process.env.PUBLIC_IP && process.env.PUBLIC_IP !== lanIP;

  console.log('');
  console.log('='.repeat(50));
  console.log('  BTELO Coding Relay Server v2.0.0');
  console.log('='.repeat(50));
  if (hasPublicIP) {
    console.log(`  Public:    http://${localIP}:${PORT}`);
    console.log(`  LAN:       http://${lanIP}:${PORT}`);
  } else {
    console.log(`  HTTP:      http://${localIP}:${PORT}`);
  }
  console.log(`  Mobile WS: ws://${localIP}:${PORT}/ws`);
  console.log(`  Bridge WS: ws://${localIP}:${PORT}/bridge/ws`);
  console.log('');
  console.log('  Features:');
  console.log('    - structured_output support');
  console.log('    - heartbeat mechanism');
  console.log('    - session state broadcasting');
  console.log('');
  console.log('  Waiting for bridges to register...');
  console.log('='.repeat(50));
  console.log('');
});
