#!/usr/bin/env node

/**
 * Claude Code Bridge — connects Claude Code CLI to the BTELO relay server.
 *
 * Usage:
 *   node claude-bridge.js --pair-code 570558
 *   node claude-bridge.js --pair-code 570558 --server http://localhost:8080
 */

const WebSocket = require('ws');
const { spawn } = require('child_process');
const http = require('http');
const { v4: uuidv4 } = require('uuid');

// ============================================================
// Parse CLI arguments
// ============================================================
function parseArgs() {
  const args = process.argv.slice(2);
  const opts = {
    server: 'http://localhost:8080',
    pairCode: null,
    deviceName: 'Claude-Code-Terminal',
    workDir: process.cwd()
  };

  for (let i = 0; i < args.length; i++) {
    switch (args[i]) {
      case '--pair-code':
      case '-p':
        opts.pairCode = args[++i];
        break;
      case '--server':
      case '-s':
        opts.server = args[++i];
        break;
      case '--name':
      case '-n':
        opts.deviceName = args[++i];
        break;
      case '--workdir':
      case '-w':
        opts.workDir = args[++i];
        break;
      case '--help':
      case '-h':
        console.log(`
Claude Code Bridge — BTELO Coding Relay

Usage:
  node claude-bridge.js --pair-code <code> [options]

Options:
  -p, --pair-code <code>   Pairing code from the Android app (required)
  -s, --server <url>       Relay server URL (default: http://localhost:8080)
  -n, --name <name>        Device name (default: Claude-Code-Terminal)
  -w, --workdir <path>     Working directory for Claude Code (default: cwd)
  -h, --help               Show this help
`);
        process.exit(0);
    }
  }

  if (!opts.pairCode) {
    console.error('Error: --pair-code is required. Run with --help for usage.');
    process.exit(1);
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
      path: urlObj.pathname,
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
// Start a persistent Claude Code process
// Uses --input-format stream-json to accept commands via stdin
// ============================================================
let claudeProcess = null;
let claudeSessionId = null;
let claudeStdin = null;
let outputBuffer = '';
let processingCommand = false;

function startClaudeProcess(workDir, onOutput) {
  if (!claudeSessionId) {
    claudeSessionId = uuidv4();
  }

  const args = [
    '--input-format', 'stream-json',
    '--output-format', 'stream-json',
    '--session-id', claudeSessionId,
    '--dangerously-skip-permissions'
  ];

  console.log(`[CLAUDE] Starting persistent process: claude ${args.join(' ')}`);
  console.log(`[CLAUDE] Session ID: ${claudeSessionId}`);
  console.log(`[CLAUDE] Working dir: ${workDir}`);

  const child = spawn('claude', args, {
    cwd: workDir,
    shell: true,
    stdio: ['pipe', 'pipe', 'pipe']
  });

  claudeProcess = child;
  claudeStdin = child.stdin;
  outputBuffer = '';

  child.stdout.on('data', (data) => {
    outputBuffer += data.toString();
    const lines = outputBuffer.split('\n');
    outputBuffer = lines.pop();

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
    claudeProcess = null;
    claudeStdin = null;
    processingCommand = false;
  });

  child.on('error', (err) => {
    console.error(`[CLAUDE] Failed to start: ${err.message}`);
    claudeProcess = null;
    claudeStdin = null;
    processingCommand = false;
    onOutput({ type: 'error', text: `Failed to start Claude: ${err.message}` });
  });

  console.log(`[CLAUDE] Persistent process started (PID: ${child.pid})`);
  return child;
}

// ============================================================
// Send a command to the persistent Claude process via stdin
// ============================================================
function sendToClaude(command, workDir, onOutput) {
  if (!claudeStdin) {
    console.log(`[CLAUDE] No stdin available, starting process`);
    startClaudeProcess(workDir, onOutput);
    if (!claudeStdin) {
      onOutput({ type: 'error', text: 'Could not start Claude Code process' });
      return;
    }
  }

  processingCommand = true;
  console.log(`[CLAUDE] Sending command via stdin: ${command.substring(0, 80)}...`);

  const jsonMessage = JSON.stringify({
    type: 'user_message',
    content: command
  }) + '\n';

  try {
    claudeStdin.write(jsonMessage);
  } catch (err) {
    console.error(`[CLAUDE] Failed to write to stdin: ${err.message}`);
    processingCommand = false;
    onOutput({ type: 'error', text: `Failed to send command: ${err.message}` });
  }
}

// ============================================================
// Format Claude stream event for Android app
// ============================================================
function formatClaudeEvent(event) {
  switch (event.type) {
    case 'assistant':
      // Assistant message with content blocks
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
      // Final result
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
      // Skip other event types (content_block_start, content_block_delta, etc.)
      return null;
  }
}

// ============================================================
// Main
// ============================================================
async function main() {
  const opts = parseArgs();

  console.log('');
  console.log('='.repeat(50));
  console.log('  Claude Code Bridge');
  console.log('='.repeat(50));
  console.log(`  Server:    ${opts.server}`);
  console.log(`  Pair Code: ${opts.pairCode}`);
  console.log(`  Work Dir:  ${opts.workDir}`);
  console.log('='.repeat(50));
  console.log('');

  // Step 1: Pair with the mobile device
  console.log('[BRIDGE] Pairing with mobile device...');
  const pairRes = await httpRequest(`${opts.server}/device/pair`, 'POST', {
    pairing_code: opts.pairCode,
    device_name: opts.deviceName
  });

  if (pairRes.status !== 200 || !pairRes.body.success) {
    console.error(`[BRIDGE] Pairing failed: ${JSON.stringify(pairRes.body)}`);
    process.exit(1);
  }

  const { session_id, token, mobile_device_name } = pairRes.body;
  console.log(`[BRIDGE] Paired with "${mobile_device_name}" in session ${session_id}`);
  console.log(`[BRIDGE] Token: ${token.substring(0, 20)}...`);

  // Step 2: Connect WebSocket
  const wsUrl = opts.server.replace('http://', 'ws://').replace('https://', 'wss://') + `/ws?token=${token}`;
  console.log(`[BRIDGE] Connecting to ${wsUrl.split('?')[0]}...`);

  const ws = new WebSocket(wsUrl);

  let isConnected = false;
  let commandQueue = [];

  ws.on('open', () => {
    console.log('[BRIDGE] WebSocket connected');
    // Send public key (fake for now, matching server protocol)
    ws.send(JSON.stringify({
      type: 'publicKey',
      key: Buffer.from('terminal-key-' + Date.now()).toString('base64')
    }));
  });

  ws.on('message', (data) => {
    let msg;
    try {
      msg = JSON.parse(data.toString());
    } catch {
      console.error('[BRIDGE] Invalid JSON:', data.toString());
      return;
    }

    switch (msg.type) {
      case 'publicKey':
        console.log('[BRIDGE] Key exchange complete');
        break;

      case 'status':
        if (msg.connected) {
          isConnected = true;
          console.log(`[BRIDGE] Ready! Peer: ${msg.peer || 'unknown'}`);
          console.log('[BRIDGE] Waiting for commands from mobile app...\n');
        }
        break;

      case 'command':
        if (msg.content === 'ping') {
          ws.send(JSON.stringify({ type: 'command', content: 'pong', inputType: 'TEXT' }));
          return;
        }

        console.log(`[BRIDGE] Received command: ${msg.content}`);

        // Queue command if another is being processed
        if (processingCommand) {
          commandQueue.push(msg.content);
          ws.send(JSON.stringify({
            type: 'output',
            data: `[Queued] Command queued (${commandQueue.length} in queue)`,
            stream: 'STDOUT'
          }));
          return;
        }

        executeCommand(msg.content);
        break;

      case 'keyRotation':
        // Respond to key rotation
        if (msg.action === 'initiate') {
          ws.send(JSON.stringify({
            type: 'keyRotation',
            action: 'accept',
            newPublicKey: Buffer.from('rotated-terminal-key-' + Date.now()).toString('base64'),
            keyVersion: msg.keyVersion,
            timestamp: Date.now()
          }));
          ws.send(JSON.stringify({
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

  function executeCommand(command) {
    // Start persistent process if not running
    if (!claudeProcess) {
      startClaudeProcess(opts.workDir, (event) => {
        const formatted = formatClaudeEvent(event);
        if (formatted) {
          ws.send(JSON.stringify({
            type: 'output',
            data: formatted.data,
            stream: formatted.stream
          }));
        }

        // Mark command as complete on result
        if (event.type === 'result') {
          processingCommand = false;
          ws.send(JSON.stringify({
            type: 'output',
            data: `\n[Done]\n`,
            stream: 'STDOUT'
          }));

          // Process queued commands
          if (commandQueue.length > 0) {
            const next = commandQueue.shift();
            console.log(`[BRIDGE] Processing queued command (${commandQueue.length} remaining)`);
            executeCommand(next);
          }
        }
      });
    }

    // Send acknowledgment
    ws.send(JSON.stringify({
      type: 'output',
      data: `[Claude Code] Processing...\n`,
      stream: 'STDOUT'
    }));

    // Queue if a command is already being processed
    if (processingCommand) {
      commandQueue.push(command);
      ws.send(JSON.stringify({
        type: 'output',
        data: `[Queued] Command queued (${commandQueue.length} in queue)\n`,
        stream: 'STDOUT'
      }));
      return;
    }

    sendToClaude(command, opts.workDir, (event) => {
      const formatted = formatClaudeEvent(event);
      if (formatted) {
        ws.send(JSON.stringify({
          type: 'output',
          data: formatted.data,
          stream: formatted.stream
        }));
      }

      // Mark command as complete on result
      if (event.type === 'result') {
        processingCommand = false;
        ws.send(JSON.stringify({
          type: 'output',
          data: `\n[Done]\n`,
          stream: 'STDOUT'
        }));

        // Process queued commands
        if (commandQueue.length > 0) {
          const next = commandQueue.shift();
          console.log(`[BRIDGE] Processing queued command (${commandQueue.length} remaining)`);
          executeCommand(next);
        }
      }
    });
  }

  ws.on('close', (code, reason) => {
    console.log(`[BRIDGE] WebSocket closed: ${code} ${reason}`);
    isConnected = false;
    // Kill persistent Claude process
    if (claudeProcess) {
      console.log('[BRIDGE] Shutting down persistent Claude process');
      claudeProcess.kill();
      claudeProcess = null;
      claudeStdin = null;
    }
    // Exit after a short delay to allow output to flush
    setTimeout(() => process.exit(0), 1000);
  });

  ws.on('error', (err) => {
    console.error(`[BRIDGE] WebSocket error: ${err.message}`);
  });

  // Handle graceful shutdown
  process.on('SIGINT', () => {
    console.log('\n[BRIDGE] Shutting down...');
    if (claudeProcess) {
      console.log('[BRIDGE] Shutting down persistent Claude process');
      claudeProcess.kill();
      claudeProcess = null;
      claudeStdin = null;
    }
    ws.close();
    setTimeout(() => process.exit(0), 500);
  });
}

main().catch(err => {
  console.error(`[BRIDGE] Fatal error: ${err.message}`);
  process.exit(1);
});
