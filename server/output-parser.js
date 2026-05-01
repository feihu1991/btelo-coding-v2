/**
 * BTELO Coding - Stream JSON Output Parser
 *
 * Parses Claude Code's --output-format stream-json output into structured messages
 * that the Android app can render as thinking/tool/file/system/error cards.
 */

const OutputType = {
  CLAUDE_RESPONSE: 'claude_response',
  TOOL_CALL: 'tool_call',
  FILE_OP: 'file_op',
  THINKING: 'thinking',
  ERROR: 'error',
  SYSTEM: 'system',
  RAW: 'raw'
};

const FILE_TOOLS = new Set(['Read', 'Write', 'Edit', 'MultiEdit', 'NotebookEdit']);
const DISCOVERY_TOOLS = new Set(['Glob', 'Grep', 'LS']);

function nowTimestamp() {
  // Android currently accepts timestamp as a string and attempts to parse Long.
  return String(Date.now());
}

function normalizeToolMetadata(block) {
  const input = block.input || {};
  const toolName = block.name || 'unknown_tool';
  const filePath = input.file_path || input.path || input.notebook_path || null;
  const command = input.command || null;

  return {
    toolId: block.id || null,
    toolName,
    toolType: block.type || 'tool_use',
    filePath,
    command,
    parameters: input,
    isFileOp: FILE_TOOLS.has(toolName),
    fileOpType: FILE_TOOLS.has(toolName) ? toolName.toLowerCase() : null,
    isToolResult: false,
    isCollapsed: true,
    originalLength: 0,
    parserVersion: '1.1.0'
  };
}

/**
 * Format a parsed stream event into one structured output object or an array.
 * @param {object} event Claude Code stream-json event
 * @returns {object|object[]|null}
 */
function formatStreamEvent(event) {
  switch (event.type) {
    case 'assistant': {
      const content = event.message && event.message.content;
      if (!Array.isArray(content)) return null;

      const results = [];

      for (const block of content) {
        switch (block.type) {
          case 'text':
            if (block.text) {
              results.push({
                type: OutputType.CLAUDE_RESPONSE,
                content: block.text,
                metadata: {},
                timestamp: nowTimestamp()
              });
            }
            break;

          case 'tool_use': {
            const metadata = normalizeToolMetadata(block);
            const isFileLike = metadata.isFileOp;
            const isDiscovery = DISCOVERY_TOOLS.has(metadata.toolName);
            results.push({
              type: isFileLike ? OutputType.FILE_OP : OutputType.TOOL_CALL,
              content: metadata.command || metadata.filePath || block.name || 'tool',
              metadata: {
                ...metadata,
                isFileOp: isFileLike || isDiscovery,
                fileOpType: isFileLike ? metadata.fileOpType : (isDiscovery ? metadata.toolName.toLowerCase() : null)
              },
              timestamp: nowTimestamp()
            });
            break;
          }

          case 'tool_result': {
            const text = typeof block.content === 'string'
              ? block.content
              : JSON.stringify(block.content || '');
            results.push({
              type: OutputType.SYSTEM,
              content: text,
              metadata: {
                toolId: block.tool_use_id || null,
                isToolResult: true,
                originalLength: text.length,
                isCollapsed: true,
                parserVersion: '1.1.0'
              },
              timestamp: nowTimestamp()
            });
            break;
          }

          case 'thinking':
            if (block.thinking) {
              results.push({
                type: OutputType.THINKING,
                content: block.thinking,
                metadata: {},
                timestamp: nowTimestamp()
              });
            }
            break;

          default:
            break;
        }
      }

      if (results.length === 0) return null;
      return results.length === 1 ? results[0] : results;
    }

    case 'result':
      return event.result ? {
        type: OutputType.CLAUDE_RESPONSE,
        content: event.result,
        metadata: { isFinal: true },
        timestamp: nowTimestamp()
      } : null;

    case 'text':
      return {
        type: OutputType.CLAUDE_RESPONSE,
        content: event.text || '',
        metadata: {},
        timestamp: nowTimestamp()
      };

    case 'stderr':
      return {
        type: OutputType.ERROR,
        content: event.text || '',
        metadata: { stream: 'stderr' },
        timestamp: nowTimestamp()
      };

    case 'error':
      return {
        type: OutputType.ERROR,
        content: event.text || event.message || JSON.stringify(event),
        metadata: { stream: 'error', code: event.code || null },
        timestamp: nowTimestamp()
      };

    case 'system':
      return {
        type: OutputType.SYSTEM,
        content: event.text || event.message || JSON.stringify(event),
        metadata: {},
        timestamp: nowTimestamp()
      };

    default:
      return null;
  }
}

/**
 * Parse a single stream-json line into structured output.
 * @param {string} line A single line from Claude Code stream-json output
 * @returns {object|object[]|null}
 */
function parseStreamJsonLine(line) {
  if (!line || !line.trim()) return null;

  try {
    const event = JSON.parse(line);
    return formatStreamEvent(event);
  } catch {
    return {
      type: OutputType.RAW,
      content: line,
      metadata: {},
      timestamp: nowTimestamp()
    };
  }
}

/**
 * Parse a buffer of stream-json output, handling partial lines.
 * @param {string} buffer Accumulated buffer
 * @returns {{messages: object[], remaining: string}}
 */
function parseStreamBuffer(buffer) {
  const lines = buffer.split('\n');
  const remaining = lines.pop();

  const messages = [];
  for (const line of lines) {
    if (!line.trim()) continue;
    const parsed = parseStreamJsonLine(line);
    if (Array.isArray(parsed)) {
      messages.push(...parsed);
    } else if (parsed) {
      messages.push(parsed);
    }
  }

  return { messages, remaining };
}

class OutputParser {
  constructor({ onMessage, debug = false } = {}) {
    if (typeof onMessage !== 'function') {
      throw new Error('OutputParser requires an onMessage callback');
    }
    this.onMessage = onMessage;
    this.debug = debug;
    this.buffer = '';
  }

  process(chunk) {
    if (!chunk) return;
    this.buffer += chunk.toString();

    const { messages, remaining } = parseStreamBuffer(this.buffer);
    this.buffer = remaining;

    for (const message of messages) {
      this.emitParsedMessage(message);
    }
  }

  flush() {
    const remaining = this.buffer.trim();
    this.buffer = '';
    if (!remaining) return;

    const parsed = parseStreamJsonLine(remaining);
    const messages = Array.isArray(parsed) ? parsed : (parsed ? [parsed] : []);
    for (const message of messages) {
      this.emitParsedMessage(message);
    }
  }

  emitStructuredOutput(outputType, content, metadata = {}) {
    this.onMessage({
      type: 'structured_output',
      output_type: outputType,
      content: content || '',
      metadata,
      timestamp: nowTimestamp()
    });
  }

  emitParsedMessage(message) {
    if (!message) return;

    if (message.type === OutputType.RAW) {
      if (this.debug) {
        this.onMessage({
          type: 'output',
          data: message.content,
          stream: 'STDOUT'
        });
      }
      return;
    }

    this.onMessage({
      type: 'structured_output',
      output_type: message.type,
      content: message.content || '',
      metadata: message.metadata || {},
      timestamp: message.timestamp || nowTimestamp()
    });
  }
}

// Default export is the class expected by bridge.js.
module.exports = OutputParser;

// Named exports keep tests and ad-hoc scripts compatible.
module.exports.OutputParser = OutputParser;
module.exports.OutputType = OutputType;
module.exports.parseStreamJsonLine = parseStreamJsonLine;
module.exports.parseStreamBuffer = parseStreamBuffer;
module.exports.formatStreamEvent = formatStreamEvent;

if (require.main === module) {
  const parser = new OutputParser({
    debug: true,
    onMessage: (msg) => console.log(JSON.stringify(msg, null, 2))
  });

  process.stdin.setEncoding('utf-8');
  process.stdin.on('data', (chunk) => parser.process(chunk));
  process.stdin.on('end', () => parser.flush());
}
