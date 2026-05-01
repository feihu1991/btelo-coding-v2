const fs = require('fs');
const os = require('os');
const path = require('path');

const CLAUDE_HOME = path.join(os.homedir(), '.claude');
const SESSIONS_DIR = path.join(CLAUDE_HOME, 'sessions');
const PROJECTS_DIR = path.join(CLAUDE_HOME, 'projects');

function isProcessAlive(pid) {
  if (!pid) return false;
  try {
    process.kill(pid, 0);
    return true;
  } catch {
    return false;
  }
}

function encodeProjectPath(cwd) {
  return String(cwd || '').replace(/[\\:]/g, '-');
}

function safeReadJson(filePath) {
  try {
    return JSON.parse(fs.readFileSync(filePath, 'utf-8'));
  } catch {
    return null;
  }
}

function findTranscriptPath(cwd, sessionId) {
  if (!cwd || !sessionId) return null;

  const direct = path.join(PROJECTS_DIR, encodeProjectPath(cwd), `${sessionId}.jsonl`);
  if (fs.existsSync(direct)) return direct;

  // Claude Code project path encoding has changed across versions. Fall back to a
  // narrow recursive search by session id so attach mode can still find the transcript.
  const target = `${sessionId}.jsonl`;
  const stack = fs.existsSync(PROJECTS_DIR) ? [PROJECTS_DIR] : [];
  while (stack.length) {
    const dir = stack.pop();
    let entries = [];
    try {
      entries = fs.readdirSync(dir, { withFileTypes: true });
    } catch {
      continue;
    }

    for (const entry of entries) {
      const full = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        stack.push(full);
      } else if (entry.isFile() && entry.name === target) {
        return full;
      }
    }
  }

  return null;
}

function readMessageCount(transcriptPath) {
  if (!transcriptPath || !fs.existsSync(transcriptPath)) return 0;
  try {
    const content = fs.readFileSync(transcriptPath, 'utf-8');
    return content.split('\n').filter(Boolean).length;
  } catch {
    return 0;
  }
}

function discoverSessions() {
  const sessions = [];
  if (!fs.existsSync(SESSIONS_DIR)) return sessions;

  const files = fs.readdirSync(SESSIONS_DIR).filter((file) => file.endsWith('.json'));
  for (const file of files) {
    const filePath = path.join(SESSIONS_DIR, file);
    const data = safeReadJson(filePath);
    if (!data || data.kind !== 'interactive') continue;

    const transcriptPath = findTranscriptPath(data.cwd, data.sessionId);
    const isAlive = isProcessAlive(data.pid);

    sessions.push({
      sessionId: data.sessionId,
      cwd: data.cwd,
      pid: data.pid,
      isAlive,
      transcriptPath,
      jsonlPath: transcriptPath,
      hasHistory: Boolean(transcriptPath && fs.existsSync(transcriptPath)),
      startedAt: data.startedAt || 0,
      messageCount: readMessageCount(transcriptPath)
    });
  }

  sessions.sort((a, b) => {
    if (a.isAlive !== b.isAlive) return a.isAlive ? -1 : 1;
    return (b.startedAt || 0) - (a.startedAt || 0);
  });

  return sessions;
}

function normalizePathForCompare(value) {
  try {
    return path.resolve(value || '').toLowerCase();
  } catch {
    return String(value || '').toLowerCase();
  }
}

function selectBestSession({ workDir, sessionId } = {}) {
  const sessions = discoverSessions();
  if (sessionId) {
    const exact = sessions.find((session) => session.sessionId === sessionId);
    if (exact) return { session: exact, sessions };
  }

  const normalizedWorkDir = normalizePathForCompare(workDir || process.cwd());
  const sameWorkdir = sessions.filter((session) =>
    session.hasHistory && normalizePathForCompare(session.cwd) === normalizedWorkDir
  );

  const best = sameWorkdir.find((session) => session.isAlive) || sameWorkdir[0]
    || sessions.find((session) => session.isAlive && session.hasHistory)
    || sessions.find((session) => session.hasHistory)
    || null;

  return { session: best, sessions };
}

module.exports = {
  CLAUDE_HOME,
  SESSIONS_DIR,
  PROJECTS_DIR,
  discoverSessions,
  selectBestSession,
  isProcessAlive,
  encodeProjectPath,
  findTranscriptPath
};
