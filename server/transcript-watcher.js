const fs = require('fs');

function readJsonLines(filePath) {
  if (!filePath || !fs.existsSync(filePath)) return [];
  try {
    return fs.readFileSync(filePath, 'utf-8')
      .split('\n')
      .filter((line) => line.trim())
      .map((line) => {
        try { return JSON.parse(line); } catch { return null; }
      })
      .filter(Boolean);
  } catch {
    return [];
  }
}

function getTimestamp(entry) {
  const value = entry.timestamp || entry.created_at || entry.time;
  const parsed = value ? new Date(value).getTime() : NaN;
  return Number.isFinite(parsed) ? parsed : Date.now();
}

function textFromContent(content) {
  if (typeof content === 'string') return content;
  if (Array.isArray(content)) {
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
  return '';
}

function parseTranscriptEntry(entry) {
  const timestamp = getTimestamp(entry);
  const baseId = entry.uuid || entry.id || `${timestamp}-${Math.random().toString(16).slice(2)}`;

  if (entry.type === 'user') {
    const content = textFromContent(entry.message && entry.message.content);
    if (!content) return [];
    return [{
      id: baseId,
      role: 'user',
      msgType: 'text',
      content,
      isFromUser: true,
      timestamp
    }];
  }

  if (entry.type === 'assistant') {
    const content = entry.message && entry.message.content;
    if (!Array.isArray(content)) return [];

    const result = [];
    content.forEach((block, index) => {
      if (!block) return;
      const id = `${baseId}-${index}`;
      if (block.type === 'text' && block.text) {
        result.push({ id, role: 'assistant', msgType: 'text', content: block.text, isFromUser: false, timestamp });
      } else if (block.type === 'tool_use') {
        const input = block.input || {};
        const filePath = input.file_path || input.path || input.notebook_path || null;
        result.push({
          id,
          role: 'assistant',
          msgType: isFileTool(block.name) ? 'file_op' : 'tool_use',
          content: input.command || filePath || block.name || 'tool',
          isFromUser: false,
          timestamp,
          toolName: block.name,
          toolId: block.id,
          input,
          filePath,
          command: input.command || null,
          fileOpType: isFileTool(block.name) ? String(block.name).toLowerCase() : null
        });
      } else if (block.type === 'tool_result') {
        const text = typeof block.content === 'string' ? block.content : JSON.stringify(block.content || '');
        result.push({
          id,
          role: 'assistant',
          msgType: 'system',
          content: text,
          isFromUser: false,
          timestamp,
          toolId: block.tool_use_id || null,
          isToolResult: true
        });
      } else if (block.type === 'thinking' && block.thinking) {
        result.push({ id, role: 'assistant', msgType: 'thinking', content: block.thinking, isFromUser: false, timestamp });
      }
    });

    return result;
  }

  return [];
}

function isFileTool(toolName) {
  return ['Read', 'Write', 'Edit', 'MultiEdit', 'NotebookEdit', 'Glob', 'Grep', 'LS'].includes(toolName);
}

function toBridgeMessage(relaySessionId, parsed) {
  switch (parsed.msgType) {
    case 'tool_use':
      return {
        type: 'structured_output',
        session_id: relaySessionId,
        output_type: 'tool_call',
        content: parsed.content,
        metadata: {
          toolId: parsed.toolId || null,
          toolName: parsed.toolName || null,
          toolType: 'tool_use',
          command: parsed.command || null,
          parameters: parsed.input || {},
          isCollapsed: true
        },
        timestamp: String(parsed.timestamp)
      };

    case 'file_op':
      return {
        type: 'structured_output',
        session_id: relaySessionId,
        output_type: 'file_op',
        content: parsed.content,
        metadata: {
          toolId: parsed.toolId || null,
          toolName: parsed.toolName || null,
          toolType: 'tool_use',
          filePath: parsed.filePath || null,
          command: parsed.command || null,
          parameters: parsed.input || {},
          isFileOp: true,
          fileOpType: parsed.fileOpType || null,
          isCollapsed: true
        },
        timestamp: String(parsed.timestamp)
      };

    case 'thinking':
      return {
        type: 'structured_output',
        session_id: relaySessionId,
        output_type: 'thinking',
        content: parsed.content,
        metadata: {},
        timestamp: String(parsed.timestamp)
      };

    case 'system':
      return {
        type: 'structured_output',
        session_id: relaySessionId,
        output_type: 'system',
        content: parsed.content,
        metadata: {
          toolId: parsed.toolId || null,
          isToolResult: Boolean(parsed.isToolResult),
          isCollapsed: true
        },
        timestamp: String(parsed.timestamp)
      };

    default:
      return {
        type: 'new_message',
        session_id: relaySessionId,
        message: {
          id: parsed.id,
          content: parsed.content,
          isFromUser: parsed.isFromUser,
          timestamp: parsed.timestamp
        }
      };
  }
}

function parseTranscriptHistory(filePath, maxMessages = 120) {
  const entries = readJsonLines(filePath);
  const parsed = [];
  for (const entry of entries) {
    parsed.push(...parseTranscriptEntry(entry));
  }
  return parsed.slice(-maxMessages);
}

class TranscriptWatcher {
  constructor({ filePath, relaySessionId, onMessage, onParsedMessage, historyLimit = 120 } = {}) {
    this.filePath = filePath;
    this.relaySessionId = relaySessionId;
    this.onMessage = onMessage;
    this.onParsedMessage = onParsedMessage;
    this.historyLimit = historyLimit;
    this.lastSize = 0;
    this.watcher = null;
    this.debounce = null;
    this.seenIds = new Set();
  }

  loadHistory() {
    const parsed = parseTranscriptHistory(this.filePath, this.historyLimit);
    for (const item of parsed) this.seenIds.add(item.id);
    if (this.filePath && fs.existsSync(this.filePath)) {
      try { this.lastSize = fs.statSync(this.filePath).size; } catch { this.lastSize = 0; }
    }
    return parsed;
  }

  start() {
    if (!this.filePath || !fs.existsSync(this.filePath)) return false;
    if (this.watcher) this.stop();
    this.lastSize = fs.statSync(this.filePath).size;
    this.watcher = fs.watch(this.filePath, (eventType) => {
      if (eventType !== 'change') return;
      clearTimeout(this.debounce);
      this.debounce = setTimeout(() => this.readIncrement(), 100);
    });
    return true;
  }

  stop() {
    if (this.watcher) {
      this.watcher.close();
      this.watcher = null;
    }
    if (this.debounce) {
      clearTimeout(this.debounce);
      this.debounce = null;
    }
  }

  readIncrement() {
    try {
      const currentSize = fs.statSync(this.filePath).size;
      if (currentSize <= this.lastSize) return;

      const fd = fs.openSync(this.filePath, 'r');
      const buffer = Buffer.alloc(currentSize - this.lastSize);
      fs.readSync(fd, buffer, 0, buffer.length, this.lastSize);
      fs.closeSync(fd);
      this.lastSize = currentSize;

      const lines = buffer.toString('utf-8').split('\n').filter((line) => line.trim());
      for (const line of lines) {
        let entry = null;
        try { entry = JSON.parse(line); } catch { continue; }
        const parsedItems = parseTranscriptEntry(entry);
        for (const parsed of parsedItems) {
          if (this.seenIds.has(parsed.id)) continue;
          this.seenIds.add(parsed.id);
          if (this.onParsedMessage) this.onParsedMessage(parsed);
          if (this.onMessage) this.onMessage(toBridgeMessage(this.relaySessionId, parsed), parsed);
        }
      }
    } catch (err) {
      if (this.onMessage) {
        this.onMessage({
          type: 'output',
          data: `Transcript watcher error: ${err.message}\n`,
          stream: 'STDERR'
        });
      }
    }
  }
}

module.exports = {
  TranscriptWatcher,
  parseTranscriptEntry,
  parseTranscriptHistory,
  toBridgeMessage
};
