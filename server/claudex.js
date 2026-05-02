#!/usr/bin/env node

'use strict';

const http = require('http');
const path = require('path');
const { spawn } = require('child_process');

const relay = process.env.BTELO_RELAY_SERVER || 'http://localhost:8080';
const cwd = process.cwd();
const claudeArgv = process.argv.slice(2);

function isLocalRelay(url) {
  try {
    const parsed = new URL(url);
    return ['localhost', '127.0.0.1', '::1'].includes(parsed.hostname);
  } catch {
    return false;
  }
}

function request(url) {
  return new Promise((resolve) => {
    const req = http.get(url, (res) => {
      res.resume();
      res.on('end', () => resolve(res.statusCode >= 200 && res.statusCode < 500));
    });
    req.on('error', () => resolve(false));
    req.setTimeout(1500, () => {
      req.destroy();
      resolve(false);
    });
  });
}

async function ensureRelay() {
  if (await request(`${relay}/status`)) return null;
  if (!isLocalRelay(relay)) return null;

  const child = spawn(process.argv[0], [path.join(__dirname, 'relay.js')], {
    cwd: __dirname,
    env: process.env,
    detached: true,
    stdio: 'ignore',
    windowsHide: true
  });
  child.unref();

  for (let i = 0; i < 20; i += 1) {
    await new Promise((resolve) => setTimeout(resolve, 250));
    if (await request(`${relay}/status`)) return child;
  }
  return child;
}

async function main() {
  await ensureRelay();

  const child = spawn(process.argv[0], [path.join(__dirname, 'terminal-bridge.js')], {
    cwd,
    env: {
      ...process.env,
      BTELO_RELAY_SERVER: relay,
      BTELO_WORKDIR: cwd,
      BTELO_CLAUDE_ARGV: JSON.stringify(claudeArgv)
    },
    stdio: 'inherit'
  });

  child.on('exit', (code, signal) => {
    if (signal) process.kill(process.pid, signal);
    else process.exit(code || 0);
  });
}

main().catch((err) => {
  process.stderr.write(`[claudex] ${err.stack || err.message}\n`);
  process.exit(1);
});
