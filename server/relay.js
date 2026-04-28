#!/usr/bin/env node

/**
 * BTELO Coding Relay Server — pure message forwarder.
 *
 * Routes JSON messages between mobile WebSocket and bridge WebSocket by session ID.
 * Zero knowledge of Claude Code, JSONL files, or session discovery.
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
const os = require('os');

const app = express();
app.use(cors());
app.use(express.json());

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
const sessions = new Map(); // sessionId -> { mobileWs, bridgeWs, connectToken, bridgeToken, createdAt, bridgeInfo }

// ============================================================
// API: Bridge registers itself
// ============================================================
app.post('/bridge/register', (req, res) => {
  const { device_name, work_dir } = req.body;

  const sessionId = 'sess-' + uuidv4().substring(0, 8);
  const connectToken = 'connect-' + crypto.randomBytes(16).toString('hex');
  const bridgeToken = 'bridge-' + crypto.randomBytes(16).toString('hex');

  const session = {
    sessionId,
    mobileWs: null,
    bridgeWs: null,
    connectToken,
    bridgeToken,
    createdAt: Date.now(),
    bridgeInfo: { device_name: device_name || 'unknown', work_dir: work_dir || process.cwd() }
  };

  sessions.set(sessionId, session);
  tokens.set(connectToken, { type: 'connect', sessionId, createdAt: Date.now() });
  tokens.set(bridgeToken, { type: 'bridge_ws', sessionId, createdAt: Date.now() });

  const localIP = getLocalIP();
  const PORT = process.env.PORT || 8080;

  console.log(`[RELAY] Bridge registered: ${sessionId} (${device_name || 'unknown'})`);

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
// API: Mobile connects via QR code token
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
      created_at: s.createdAt
    });
  }
  res.json({ success: true, sessions: list });
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
    uptime: process.uptime()
  });
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
// Forward raw JSON between mobile and bridge
// ============================================================
function forwardMessage(targetWs, senderWs, rawMessage) {
  if (targetWs && targetWs.readyState === 1) {
    targetWs.send(rawMessage);
  } else if (senderWs && senderWs.readyState === 1) {
    senderWs.send(JSON.stringify({
      type: 'status',
      connected: false,
      message: 'Peer not connected'
    }));
  }
}

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
  console.log(`[RELAY] Mobile connected to session ${session.sessionId}`);

  // Notify mobile that relay is ready
  ws.send(JSON.stringify({
    type: 'status',
    connected: true,
    message: 'Relay ready'
  }));

  // Notify bridge that mobile connected
  if (session.bridgeWs && session.bridgeWs.readyState === 1) {
    session.bridgeWs.send(JSON.stringify({
      type: 'status',
      connected: true,
      peer: 'mobile'
    }));
  }

  ws.on('message', (data) => {
    forwardMessage(session.bridgeWs, ws, data.toString());
  });

  ws.on('close', () => {
    console.log(`[RELAY] Mobile disconnected from session ${session.sessionId}`);
    session.mobileWs = null;
    // Notify bridge
    if (session.bridgeWs && session.bridgeWs.readyState === 1) {
      session.bridgeWs.send(JSON.stringify({
        type: 'status',
        connected: false,
        peer: 'mobile',
        reason: 'mobile_disconnected'
      }));
    }
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
  console.log(`[RELAY] Bridge connected to session ${session.sessionId}`);

  // Notify bridge that relay is ready
  ws.send(JSON.stringify({
    type: 'status',
    connected: true,
    role: 'bridge',
    session_id: session.sessionId
  }));

  // Notify mobile that bridge connected
  if (session.mobileWs && session.mobileWs.readyState === 1) {
    session.mobileWs.send(JSON.stringify({
      type: 'status',
      connected: true,
      message: 'Bridge connected'
    }));
  }

  ws.on('message', (data) => {
    forwardMessage(session.mobileWs, ws, data.toString());
  });

  ws.on('close', () => {
    console.log(`[RELAY] Bridge disconnected from session ${session.sessionId}`);
    session.bridgeWs = null;
    // Notify mobile
    if (session.mobileWs && session.mobileWs.readyState === 1) {
      session.mobileWs.send(JSON.stringify({
        type: 'status',
        connected: false,
        reason: 'bridge_disconnected'
      }));
    }
  });
});

// ============================================================
// Session cleanup: remove stale sessions every 60s
// ============================================================
setInterval(() => {
  const now = Date.now();
  for (const [id, session] of sessions) {
    const bothDisconnected = !session.mobileWs && !session.bridgeWs;
    const age = now - session.createdAt;
    if (bothDisconnected && age > 5 * 60 * 1000) {
      sessions.delete(id);
      tokens.delete(session.connectToken);
      tokens.delete(session.bridgeToken);
      console.log(`[RELAY] Cleaned up stale session: ${id}`);
    }
  }
}, 60 * 1000);

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
  console.log('  BTELO Coding Relay Server');
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
  console.log('  Waiting for bridges to register...');
  console.log('='.repeat(50));
  console.log('');
});
