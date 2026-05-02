const fs = require('fs');
const crypto = require('crypto');

const PROTOCOL_VERSION = 3;

const FILE_TOOLS = new Set(['Read', 'Write', 'Edit', 'MultiEdit', 'NotebookEdit', 'Glob', 'Grep', 'LS']);

function readJsonLines(filePath) {
  if (!filePath || !fs.existsSync(filePath)) return [];
  return fs.readFileSync(filePath, 'utf-8')
    .split('\n')
    .filter((line) => line.trim())
    .map((line) => {
      try { return JSON.parse(line); } catch { return null; }
    })
    .filter(Boolean);
}

function timestampOf(entry) {
  const value = entry.timestamp || entry.created_at || entry.time;
  const parsed = value ? new Date(value).getTime() : NaN;
  return Number.isFinite(parsed) ? parsed : Date.now();
}

function stableBaseId(entry, index) {
  return entry.uuid || entry.id || `${timestampOf(entry)}-${index}`;
}

function stableFingerprint(parts) {
  return crypto
    .createHash('sha256')
    .update(parts.map((part) => String(part || '')).join('\u001f'))
    .digest('hex')
    .slice(0, 24);
}

function eventMetadata(entry, role, kind, extra = {}) {
  const { content, ...metadataExtra } = extra;
  const fingerprint = stableFingerprint([
    entry.uuid || entry.id || '',
    role,
    kind,
    metadataExtra.toolId || '',
    metadataExtra.toolName || '',
    metadataExtra.filePath || '',
    metadataExtra.command || '',
    content || ''
  ]);

  return {
    sourceKind: 'transcript_jsonl',
    terminality: role === 'user' || kind === 'text' || kind === 'error' ? 'terminal' : 'advisory',
    fingerprint,
    ...metadataExtra
  };
}

function textFromContent(content) {
  if (typeof content === 'string') return content;
  if (!Array.isArray(content)) return '';
  return content
    .map((block) => {
      if (!block) return '';
      if (block.type === 'text') return block.text || '';
      if (typeof block.content === 'string') return block.content;
      return '';
    })
    .filter(Boolean)
    .join('');
}

function eventFromBlock({ entry, baseId, block, blockIndex, seq }) {
  const timestamp = timestampOf(entry);
  const id = `${baseId}-${blockIndex}`;

  if (!block) return null;

  if (block.type === 'text' && block.text) {
    return {
      id,
      seq,
      role: 'assistant',
      kind: 'text',
      content: block.text,
      timestamp,
      metadata: eventMetadata(entry, 'assistant', 'text', { content: block.text })
    };
  }

  if (block.type === 'tool_use') {
    const input = block.input || {};
    const toolName = block.name || 'tool';
    const filePath = input.file_path || input.path || input.notebook_path || null;
    const command = input.command || null;
    const isFileOp = FILE_TOOLS.has(toolName);
    return {
      id,
      seq,
      role: 'assistant',
      kind: isFileOp ? 'file_op' : 'tool_call',
      content: command || filePath || toolName,
      timestamp,
      metadata: eventMetadata(entry, 'assistant', isFileOp ? 'file_op' : 'tool_call', {
        toolId: block.id || null,
        toolName,
        toolType: 'tool_use',
        filePath,
        command,
        parameters: input,
        isFileOp,
        fileOpType: isFileOp ? String(toolName).toLowerCase() : null,
        isCollapsed: true
      })
    };
  }

  if (block.type === 'tool_result') {
    const content = typeof block.content === 'string'
      ? block.content
      : JSON.stringify(block.content || '');
    return {
      id,
      seq,
      role: 'assistant',
      kind: 'system',
      content,
      timestamp,
      metadata: eventMetadata(entry, 'assistant', 'system', {
        toolId: block.tool_use_id || null,
        isToolResult: true,
        isCollapsed: true
      })
    };
  }

  if (block.type === 'thinking' && block.thinking) {
    return {
      id,
      seq,
      role: 'assistant',
      kind: 'thinking',
      content: block.thinking,
      timestamp,
      metadata: eventMetadata(entry, 'assistant', 'thinking', { content: block.thinking })
    };
  }

  return null;
}

function parseTranscriptEntries(entries, { startSeq = 0 } = {}) {
  const events = [];
  let seq = startSeq;

  entries.forEach((entry, entryIndex) => {
    const baseId = stableBaseId(entry, entryIndex);
    const timestamp = timestampOf(entry);

    if (entry.type === 'user') {
      const content = textFromContent(entry.message && entry.message.content);
      if (content) {
        events.push({
          id: baseId,
          seq: seq++,
          role: 'user',
          kind: 'text',
          content,
          timestamp,
          metadata: eventMetadata(entry, 'user', 'text', { content })
        });
      }
      return;
    }

    if (entry.type === 'assistant') {
      const content = entry.message && entry.message.content;
      if (!Array.isArray(content)) return;
      content.forEach((block, blockIndex) => {
        const event = eventFromBlock({ entry, baseId, block, blockIndex, seq });
        if (event) {
          events.push(event);
          seq += 1;
        }
      });
      return;
    }

    if (entry.type === 'system' || entry.type === 'error') {
      const content = entry.message || entry.text || JSON.stringify(entry);
      events.push({
        id: baseId,
        seq: seq++,
        role: 'system',
        kind: entry.type === 'error' ? 'error' : 'system',
        content,
        timestamp,
        metadata: eventMetadata(entry, 'system', entry.type === 'error' ? 'error' : 'system', { content })
      });
    }
  });

  return events;
}

function loadTranscriptEvents(filePath, limit = 200) {
  const entries = readJsonLines(filePath);
  const events = parseTranscriptEntries(entries);
  return limit > 0 ? events.slice(-limit) : events;
}

function snapshotMessage({ relaySessionId, claudeSessionId, events, cursor }) {
  return {
    type: 'transcript_snapshot',
    version: PROTOCOL_VERSION,
    session_id: relaySessionId,
    claude_session_id: claudeSessionId,
    cursor: cursor || (events.length ? events[events.length - 1].seq : 0),
    events
  };
}

function deltaMessage({ relaySessionId, claudeSessionId, events, cursor }) {
  return {
    type: 'transcript_delta',
    version: PROTOCOL_VERSION,
    session_id: relaySessionId,
    claude_session_id: claudeSessionId,
    cursor: cursor || (events.length ? events[events.length - 1].seq : 0),
    events
  };
}

module.exports = {
  PROTOCOL_VERSION,
  parseTranscriptEntries,
  loadTranscriptEvents,
  snapshotMessage,
  deltaMessage
};
