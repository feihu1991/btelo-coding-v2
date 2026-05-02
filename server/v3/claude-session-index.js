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
  try { return JSON.parse(fs.readFileSync(filePath, 'utf-8')); } catch { return null; }
}

function findTranscriptPath(cwd, sessionId) {
  if (!sessionId) return null;

  if (cwd) {
    const direct = path.join(PROJECTS_DIR, encodeProjectPath(cwd), `${sessionId}.jsonl`);
    if (fs.existsSync(direct)) return direct;
  }

  const target = `${sessionId}.jsonl`;
  const stack = fs.existsSync(PROJECTS_DIR) ? [PROJECTS_DIR] : [];
  while (stack.length) {
    const dir = stack.pop();
    let entries = [];
    try { entries = fs.readdirSync(dir, { withFileTypes: true }); } catch { continue; }
    for (const entry of entries) {
      const full = path.join(dir, entry.name);
      if (entry.isDirectory()) stack.push(full);
      else if (entry.isFile() && entry.name === target) return full;
    }
  }

  return null;
}

function lineCount(filePath) {
  if (!filePath || !fs.existsSync(filePath)) return 0;
  try {
    return fs.readFileSync(filePath, 'utf-8').split('\n').filter((line) => line.trim()).length;
  } catch {
    return 0;
  }
}

function normalizePath(value) {
  try { return path.resolve(value || '').toLowerCase(); }
  catch { return String(value || '').toLowerCase(); }
}

function listClaudeSessions() {
  if (!fs.existsSync(SESSIONS_DIR)) return [];
  return fs.readdirSync(SESSIONS_DIR)
    .filter((file) => file.endsWith('.json'))
    .map((file) => {
      const data = safeReadJson(path.join(SESSIONS_DIR, file));
      if (!data || data.kind !== 'interactive') return null;
      const transcriptPath = findTranscriptPath(data.cwd, data.sessionId);
      return {
        sessionId: data.sessionId,
        cwd: data.cwd || '',
        pid: data.pid || null,
        isAlive: isProcessAlive(data.pid),
        startedAt: data.startedAt || 0,
        transcriptPath,
        hasTranscript: Boolean(transcriptPath && fs.existsSync(transcriptPath)),
        eventCount: lineCount(transcriptPath)
      };
    })
    .filter(Boolean)
    .sort((a, b) => {
      if (a.isAlive !== b.isAlive) return a.isAlive ? -1 : 1;
      return (b.startedAt || 0) - (a.startedAt || 0);
    });
}

function selectClaudeSession({ workDir, sessionId } = {}) {
  const sessions = listClaudeSessions();
  if (sessionId) {
    const exact = sessions.find((session) => session.sessionId === sessionId);
    if (exact) return { session: exact, sessions };
  }

  const normalized = normalizePath(workDir || process.cwd());
  const sameWorkDir = sessions.filter((session) =>
    session.hasTranscript && normalizePath(session.cwd) === normalized
  );

  return {
    session: sameWorkDir.find((session) => session.isAlive)
      || sameWorkDir[0]
      || sessions.find((session) => session.isAlive && session.hasTranscript)
      || sessions.find((session) => session.hasTranscript)
      || null,
    sessions
  };
}

module.exports = {
  CLAUDE_HOME,
  listClaudeSessions,
  selectClaudeSession,
  isProcessAlive
};
