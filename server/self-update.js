'use strict';

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { execSync, spawn } = require('child_process');

const STATE_FILE = path.join(__dirname, '.claudex-state.json');
const WATCHED_FILES = [
  'pty-bridge.js',
  'output-parser.js',
  'bridge-attach.js',
  'bridge.js',
  'relay.js',
  'session-detector.js',
  'transcript-watcher.js',
  'restart.js',
  'package.json'
];

function fileHash(filePath) {
  try {
    const content = fs.readFileSync(filePath);
    return crypto.createHash('md5').update(content).digest('hex');
  } catch {
    return null;
  }
}

function loadState() {
  try {
    return JSON.parse(fs.readFileSync(STATE_FILE, 'utf-8'));
  } catch {
    return { hashes: {} };
  }
}

function saveState(state) {
  fs.writeFileSync(STATE_FILE, JSON.stringify(state, null, 2));
}

function snapshotHashes() {
  const hashes = {};
  for (const file of WATCHED_FILES) {
    const fp = path.join(__dirname, file);
    hashes[file] = fileHash(fp);
  }
  return hashes;
}

function detectChanges() {
  const state = loadState();
  const current = snapshotHashes();
  const changed = [];

  for (const file of WATCHED_FILES) {
    if (current[file] !== state.hashes[file]) {
      changed.push(file);
    }
  }

  return { changed, current };
}

function reinstall() {
  console.log('[SELF-UPDATE] Reinstalling claudex...');
  try {
    execSync('npm install -g .', { cwd: __dirname, stdio: 'inherit', timeout: 30000 });
    console.log('[SELF-UPDATE] Reinstall complete');
    return true;
  } catch (err) {
    console.error('[SELF-UPDATE] Reinstall failed:', err.message);
    return false;
  }
}

function restart(argv) {
  console.log('[SELF-UPDATE] Restarting claudex...');
  const child = spawn(process.argv[0], argv, {
    cwd: process.cwd(),
    stdio: 'inherit',
    detached: true
  });
  child.unref();
  process.exit(0);
}

function checkAndUpdate(argv) {
  const { changed, current } = detectChanges();

  if (changed.length === 0) {
    saveState({ hashes: current, lastCheck: Date.now() });
    return false;
  }

  console.log('[SELF-UPDATE] Detected changes in:', changed.join(', '));

  if (reinstall()) {
    saveState({ hashes: current, lastCheck: Date.now() });
    restart(argv);
    return true;
  }

  return false;
}

function watch(intervalMs, argv) {
  const state = loadState();
  if (!state.hashes || Object.keys(state.hashes).length === 0) {
    saveState({ hashes: snapshotHashes(), lastCheck: Date.now() });
  }

  setInterval(() => {
    try {
      checkAndUpdate(argv);
    } catch (err) {
      console.error('[SELF-UPDATE] Error:', err.message);
    }
  }, intervalMs);

  console.log(`[SELF-UPDATE] Watching for changes every ${intervalMs / 1000}s`);
}

module.exports = { checkAndUpdate, watch, snapshotHashes, saveState };
