/**
 * BTELO Coding - Stream JSON Output Parser
 * 
 * Parses Claude Code's --output-format stream-json output into structured messages.
 * No ANSI parsing needed — Claude Code handles the formatting for us.
 * 
 * Stream JSON event types from Claude Code:
 * - {type: "assistant", message: {content: [{type: "text", text: "..."}]}}
 * - {type: "assistant", message: {content: [{type: "tool_use", ...}]}}
 * - {type: "assistant", message: {content: [{type: "tool_result", ...}]}}
 * - {type: "result", result: "..."}
 * - {type: "text", text: "..."}
 * - {type: "stderr", text: "..."}
 * - {type: "error", text: "..."}
 */

const OutputType = {
  CLAUDE_RESPONSE: 'claude_response',
  TOOL_CALL: 'tool_call',
  TOOL_RESULT: 'tool_result',
  FILE_OP: 'file_op',
  THINKING: 'thinking',
  ERROR: 'error',
  SYSTEM: 'system',
  RAW: 'raw'
};

/**
 * Parse a single stream-json line into structured output
 * @param {string} line - A single line from Claude Code stream-json output
 * @returns {object|null} - Parsed output or null if not parseable
 */
function parseStreamJsonLine(line) {
  if (!line || !line.trim()) return null;

  try {
    const event = JSON.parse(line);
    return formatStreamEvent(event);
  } catch {
    // Not JSON — treat as raw text
    return {
      type: OutputType.RAW,
      content: line,
      metadata: {},
      timestamp: new Date().toISOString()
    };
  }
}

/**
 * Format a parsed stream event into structured output
 */
function formatStreamEvent(event) {
  switch (event.type) {
    case 'assistant': {
      if (!event.message || !event.message.content) return null;
      const content = event.message.content;
      
      if (!Array.isArray(content)) return null;
      
      const results = [];
      
      for (const block of content) {
        switch (block.type) {
          case 'text':
            results.push({
              type: OutputType.CLAUDE_RESPONSE,
              content: block.text || '',
              metadata: {},
              timestamp: new Date().toISOString()
            });
            break;
            
          case 'tool_use':
            results.push({
              type: OutputType.TOOL_CALL,
              content: block.name || 'unknown_tool',
              metadata: {
                toolId: block.id,
                toolName: block.name,
                toolType: block.type,
                parameters: block.input || {}
              },
              timestamp: new Date().toISOString()
            });
            break;
            
          case 'thinking':
            results.push({
              type: OutputType.THINKING,
              content: block.thinking || '',
              metadata: {},
              timestamp: new Date().toISOString()
            });
            break;
        }
      }
      
      // Return first result if only one, otherwise return array
      if (results.length === 0) return null;
      if (results.length === 1) return results[0];
      return results;
    }
    
    case 'result':
      return {
        type: OutputType.CLAUDE_RESPONSE,
        content: event.result || '',
        metadata: { isFinal: true },
        timestamp: new Date().toISOString()
      };
      
    case 'text':
      return {
        type: OutputType.CLAUDE_RESPONSE,
        content: event.text || '',
        metadata: {},
        timestamp: new Date().toISOString()
      };
      
    case 'stderr':
      return {
        type: OutputType.ERROR,
        content: event.text || '',
        metadata: { stream: 'stderr' },
        timestamp: new Date().toISOString()
      };
      
    case 'error':
      return {
        type: OutputType.ERROR,
        content: event.text || JSON.stringify(event),
        metadata: { stream: 'error' },
        timestamp: new Date().toISOString()
      };
      
    default:
      return null;
  }
}

/**
 * Parse a buffer of stream-json output, handling partial lines
 * @param {string} buffer - Accumulated buffer
 * @returns {{messages: array, remaining: string}} - Parsed messages and remaining buffer
 */
function parseStreamBuffer(buffer) {
  const lines = buffer.split('\n');
  const remaining = lines.pop(); // Last line might be incomplete
  
  const messages = [];
  for (const line of lines) {
    if (!line.trim()) continue;
    const parsed = parseStreamJsonLine(line);
    if (parsed) {
      if (Array.isArray(parsed)) {
        messages.push(...parsed);
      } else {
        messages.push(parsed);
      }
    }
  }
  
  return { messages, remaining };
}

module.exports = {
  OutputType,
  parseStreamJsonLine,
  parseStreamBuffer,
  formatStreamEvent
};
