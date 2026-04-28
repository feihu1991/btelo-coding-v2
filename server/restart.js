#!/usr/bin/env node
const { execSync, spawn } = require('child_process');
const net = require('net');

const PORT = 8080;

console.log(`[restart] Killing process on port ${PORT}...`);

// Find and kill process on the port
try {
  const output = execSync(`netstat -ano`, { encoding: 'utf-8' });
  const pids = new Set();
  for (const line of output.split('\n')) {
    if (line.includes(`:${PORT}`) && line.includes('LISTENING')) {
      const parts = line.trim().split(/\s+/);
      const pid = parts[parts.length - 1];
      if (pid && pid !== '0') pids.add(pid);
    }
  }
  for (const pid of pids) {
    try {
      execSync(`taskkill /F /PID ${pid}`, { stdio: 'ignore' });
      console.log(`[restart] Killed PID ${pid}`);
    } catch (e) { /* already dead */ }
  }
  if (pids.size === 0) console.log(`[restart] No process found on port ${PORT}`);
} catch (e) {
  console.log(`[restart] Error finding process: ${e.message}`);
}

// Wait for port to be free, then start
function waitAndStart(retries = 20) {
  const tester = net.createServer();
  tester.once('error', () => {
    if (retries <= 0) {
      console.error(`[restart] Port ${PORT} still in use after 10s, giving up`);
      process.exit(1);
    }
    setTimeout(() => waitAndStart(retries - 1), 500);
  });
  tester.once('listening', () => {
    tester.close(() => {
      console.log(`[restart] Port free, starting server...\n`);
      const child = spawn(process.argv[0], ['relay.js'], {
        stdio: 'inherit',
        cwd: __dirname
      });
      child.on('exit', (code) => process.exit(code));
    });
  });
  tester.listen(PORT, '0.0.0.0');
}

waitAndStart();
