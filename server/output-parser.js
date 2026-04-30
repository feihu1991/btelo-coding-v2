#!/usr/bin/env node

/**
 * BTELO Coding Output Parser
 * 
 * Parses Claude Code's stream-json output into structured messages.
 * 
 * Claude Code stream-json format reference:
 * - { "type": "assistant", "message": { "content": [...] } }
 * - { "type": "tool_call", "id": "...", "name": "...", "input": {...} }
 * - { "type": "tool_result", "tool_use_id": "...", "content": "..." }
 * - { "type": "result", "content": "..." }
 * - { "type": "text", "text": "..." }
 * - { "type": "subtype", "subtype": "thinking", "thinking": "..." }
 * - { "type": "error", "error": "...", "subtype": "..." }
 * 
 * Output types:
 * - claude_response: Regular text output from Claude
 * - tool_call: Tool usage request (Bash, Read, Edit, etc.)
 * - file_op: File operation details
 * - thinking: Thinking/thought process
 * - error: Error message
 * - system: System message
 */

'use strict';

// ============================================================
// Output Types Enum
// ============================================================
const OutputType = {
  CLAUDE_RESPONSE: 'claude_response',
  TOOL_CALL: 'tool_call',
  FILE_OP: 'file_op',
  THINKING: 'thinking',
  ERROR: 'error',
  SYSTEM: 'system'
};

// ============================================================
// Tool name mappings
// ============================================================
const ToolNames = {
  'bash': 'Bash',
  'read': 'Read',
  'edit': 'Edit',
  'write': 'Write',
  'grepr': 'Grep',
  'glob': 'Glob',
  'lsp': 'LSP',
  'webfetch': 'WebFetch',
  'mcp__taskagent__exec': 'TaskAgent',
  'mcp__filesystem__read_file': 'FileRead',
  'mcp__filesystem__write_file': 'FileWrite',
  'mcp__filesystem__list_directory': 'ListDir',
  'mcp__cli__execute_command': 'CLI'
};

// ============================================================
// File operation patterns
// ============================================================
const FileOpPatterns = [
  /^(Read|read)\s+['"]?([^\s'"]+)['"]?/,
  /^(Edit|edit)\s+['"]?([^\s'"]+)['"]?/,
  /^(Write|write)\s+['"]?([^\s'"]+)['"]?/,
  /^(Create|create)\s+['"]?([^\s'"]+)['"]?/,
  /^(Delete|delete)\s+['"]?([^\s'"]+)['"]?/,
  /^(Move|move)\s+['"]?([^\s'"]+)['"]?/,
  /^(Copy|copy)\s+['"]?([^\s'"]+)['"]?/,
  /^(Rename|rename)\s+['"]?([^\s'"]+)['"]?/,
  /^(Edit|edit)\s+['"]?([^\s'"]+)['"]?.*\(line\s+\d+\)/i
];

/**
 * Detect if content indicates a file operation
 */
function detectFileOp(content, toolName) {
  // Common file operations
  const readPatterns = ['Reading', 'read', 'Opening', 'open'];
  const writePatterns = ['Writing', 'write', 'Creating', 'create', 'Saving', 'save'];
  const editPatterns = ['Editing', 'edit', 'Modifying', 'modify', 'Updating', 'update'];
  const deletePatterns = ['Deleting', 'delete', 'Removing', 'remove'];
  
  const lowerContent = content.toLowerCase();
  const lowerTool = toolName.toLowerCase();
  
  // Tool-based detection
  if (['read', 'write', 'edit', 'glob', 'lsp'].includes(lowerTool)) {
    return {
      detected: true,
      operation: lowerTool === 'read' ? 'read' : 
                 lowerTool === 'write' ? 'write' :
                 lowerTool === 'edit' ? 'edit' : 'other',
      filePath: extractFilePath(content)
    };
  }
  
  // Pattern-based detection
  for (const pattern of readPatterns) {
    if (lowerContent.includes(pattern.toLowerCase())) {
      return { detected: true, operation: 'read', filePath: extractFilePath(content) };
    }
  }
  for (const pattern of writePatterns) {
    if (lowerContent.includes(pattern.toLowerCase())) {
      return { detected: true, operation: 'write', filePath: extractFilePath(content) };
    }
  }
  for (const pattern of editPatterns) {
    if (lowerContent.includes(pattern.toLowerCase())) {
      return { detected: true, operation: 'edit', filePath: extractFilePath(content) };
    }
  }
  for (const pattern of deletePatterns) {
    if (lowerContent.includes(pattern.toLowerCase())) {
      return { detected: true, operation: 'delete', filePath: extractFilePath(content) };
    }
  }
  
  return { detected: false };
}

/**
 * Extract file path from content
 */
function extractFilePath(content) {
  // Match common path patterns
  const patterns = [
    /['"]([^'"]+\.[a-zA-Z0-9]+)['"]/g,           // Quoted paths
    /\/([\w\-\.\/]+\.[a-zA-Z0-9]+)/g,            // Absolute paths
    /([\w\-\.]+\.[a-zA-Z0-9]+)/g                  // Simple filenames
  ];
  
  for (const pattern of patterns) {
    const matches = content.match(pattern);
    if (matches && matches.length > 0) {
      // Return the most likely file path (usually the longest match)
      return matches.sort((a, b) => b.length - a.length)[0];
    }
  }
  
  return null;
}

/**
 * Parse thinking content from event
 */
function parseThinking(event) {
  // Claude Code uses "subtype": "thinking" with "thinking" field
  if (event.subtype === 'thinking' && event.thinking) {
    return {
      content: event.thinking,
      isCollapsed: true
    };
  }
  
  // Also check for common thinking indicators in text
  if (event.type === 'text' && event.text) {
    const thinkingPatterns = [
      /^(Let me|I need to|First|Let me think|Hmm|Well,)/i,
      /thinking/i
    ];
    
    for (const pattern of thinkingPatterns) {
      if (pattern.test(event.text)) {
        return {
          content: event.text,
          isCollapsed: true
        };
      }
    }
  }
  
  return null;
}

/**
 * Format tool call metadata
 */
function formatToolMetadata(event) {
  const toolName = event.name || event.tool || 'unknown';
  const displayName = ToolNames[toolName] || toolName;
  
  let parameters = {};
  if (event.input) {
    parameters = event.input;
  } else if (event.parameters) {
    parameters = event.parameters;
  }
  
  // Extract command for Bash tool
  let command = null;
  if (toolName === 'bash' && parameters.command) {
    command = parameters.command;
  } else if (toolName === 'bash' && parameters.cmd) {
    command = parameters.cmd;
  }
  
  // Extract file path for file operations
  let filePath = null;
  if (parameters.file || parameters.path || parameters.target) {
    filePath = parameters.file || parameters.path || parameters.target;
  }
  
  return {
    toolId: event.id || event.tool_use_id || generateId(),
    toolName: displayName,
    toolType: toolName,
    parameters,
    command,
    filePath,
    rawInput: event.input || event.parameters || {}
  };
}

/**
 * Generate a simple unique ID
 */
function generateId() {
  return 'struct-' + Date.now().toString(36) + '-' + Math.random().toString(36).substr(2, 5);
}

/**
 * Create structured output message
 */
function createStructuredOutput(outputType, content, metadata = {}) {
  return {
    type: 'structured_output',
    output_type: outputType,
    content: content,
    metadata: {
      ...metadata,
      parserVersion: '1.0.0'
    },
    timestamp: new Date().toISOString()
  };
}

// ============================================================
// Main Parser Class
// ============================================================
class OutputParser {
  constructor(options = {}) {
    this.buffer = '';
    this.pendingThinking = null;
    this.pendingToolCall = null;
    this.messageBuffer = [];
    this.onMessage = options.onMessage || (() => {});
    this.debug = options.debug || false;
  }
  
  /**
   * Process raw input data (from PTY or CLI stdout)
   */
  process(data) {
    this.buffer += data.toString();
    const lines = this.buffer.split('\n');
    this.buffer = lines.pop() || '';
    
    for (const line of lines) {
      if (line.trim()) {
        this.processLine(line.trim());
      }
    }
  }
  
  /**
   * Process a single line of output
   */
  processLine(line) {
    // Try to parse as JSON
    try {
      const event = JSON.parse(line);
      this.processEvent(event);
    } catch {
      // Not JSON - treat as raw text output
      if (this.debug) {
        console.log('[PARSER] Raw text:', line.substring(0, 100));
      }
      
      // Check if it's an error
      if (line.toLowerCase().includes('error') || line.startsWith('!')) {
        this.emitStructuredOutput(OutputType.ERROR, line);
      } else if (line.startsWith('[System]') || line.startsWith('[system]')) {
        this.emitStructuredOutput(OutputType.SYSTEM, line.replace(/^\[System\]\s*/i, ''));
      } else {
        // Treat as regular response
        this.emitStructuredOutput(OutputType.CLAUDE_RESPONSE, line);
      }
    }
  }
  
  /**
   * Process a parsed Claude Code event
   */
  processEvent(event) {
    if (this.debug) {
      console.log('[PARSER] Event type:', event.type, JSON.stringify(event).substring(0, 100));
    }
    
    switch (event.type) {
      case 'assistant':
        this.handleAssistantEvent(event);
        break;
        
      case 'tool_call':
        this.handleToolCallEvent(event);
        break;
        
      case 'tool_use':
        // Alias for tool_call
        this.handleToolCallEvent(event);
        break;
        
      case 'tool_result':
        this.handleToolResultEvent(event);
        break;
        
      case 'result':
        // Final result after tool execution
        if (event.content) {
          this.emitStructuredOutput(OutputType.CLAUDE_RESPONSE, event.content);
        }
        break;
        
      case 'text':
        if (event.text) {
          // Check for thinking
          const thinking = parseThinking(event);
          if (thinking) {
            this.emitStructuredOutput(OutputType.THINKING, thinking.content, {
              isCollapsed: thinking.isCollapsed
            });
          } else {
            this.emitStructuredOutput(OutputType.CLAUDE_RESPONSE, event.text);
          }
        }
        break;
        
      case 'subtype':
        if (event.subtype === 'thinking' && event.thinking) {
          this.emitStructuredOutput(OutputType.THINKING, event.thinking, {
            isCollapsed: true
          });
        } else if (event.subtype === 'system' && event.content) {
          this.emitStructuredOutput(OutputType.SYSTEM, event.content);
        }
        break;
        
      case 'error':
        this.handleErrorEvent(event);
        break;
        
      case 'stderr':
        if (event.text) {
          this.emitStructuredOutput(OutputType.ERROR, event.text);
        }
        break;
        
      case 'user':
        // User message - ignore in output parsing
        break;
        
      case 'message':
        // Container message type
        if (event.message && event.message.content) {
          const content = event.message.content;
          if (Array.isArray(content)) {
            for (const block of content) {
              if (block.type === 'text' && block.text) {
                this.emitStructuredOutput(OutputType.CLAUDE_RESPONSE, block.text);
              } else if (block.type === 'tool_use') {
                this.handleToolUseBlock(block);
              }
            }
          }
        }
        break;
        
      default:
        if (this.debug) {
          console.log('[PARSER] Unknown event type:', event.type);
        }
    }
  }
  
  /**
   * Handle assistant message event
   */
  handleAssistantEvent(event) {
    if (!event.message || !event.message.content) return;
    
    const content = event.message.content;
    if (!Array.isArray(content)) return;
    
    for (const block of content) {
      if (block.type === 'text' && block.text) {
        // Check for thinking in text
        const thinking = parseThinking({ type: 'text', text: block.text });
        if (thinking) {
          this.emitStructuredOutput(OutputType.THINKING, thinking.content, {
            isCollapsed: thinking.isCollapsed
          });
        } else {
          this.emitStructuredOutput(OutputType.CLAUDE_RESPONSE, block.text);
        }
      } else if (block.type === 'tool_use') {
        this.handleToolUseBlock(block);
      }
    }
  }
  
  /**
   * Handle tool_use block from assistant message
   */
  handleToolUseBlock(block) {
    const toolName = block.name || 'unknown';
    const displayName = ToolNames[toolName] || toolName;
    
    // Extract relevant parameters based on tool type
    let summary = '';
    let filePath = null;
    let command = null;
    let parameters = block.input || {};
    
    if (toolName === 'bash') {
      command = parameters.command || parameters.cmd || '';
      summary = `Running: ${command.substring(0, 60)}${command.length > 60 ? '...' : ''}`;
      filePath = extractFilePath(command);
    } else if (toolName === 'read') {
      filePath = parameters.file_path || parameters.path || parameters.file;
      summary = `Reading: ${filePath || 'file'}`;
    } else if (toolName === 'write') {
      filePath = parameters.file_path || parameters.path || parameters.file;
      summary = `Writing: ${filePath || 'file'}`;
    } else if (toolName === 'edit') {
      filePath = parameters.file_path || parameters.path || parameters.file;
      const editType = parameters.edit_type || parameters.type || 'modification';
      summary = `Editing (${editType}): ${filePath || 'file'}`;
    } else if (toolName === 'glob') {
      const pattern = parameters.pattern || parameters.glob || '*';
      summary = `Finding: ${pattern}`;
    } else if (toolName === 'grepr') {
      const query = parameters.query || parameters.search || parameters.text || '';
      const file = parameters.file || parameters.path || 'all files';
      summary = `Searching "${query}" in ${file}`;
    } else {
      summary = `Using: ${displayName}`;
    }
    
    // Detect file operations
    const fileOp = detectFileOp(summary, toolName);
    
    this.emitStructuredOutput(OutputType.TOOL_CALL, summary, {
      toolId: block.id || generateId(),
      toolName: displayName,
      toolType: toolName,
      filePath: filePath,
      command: command,
      parameters: parameters,
      isFileOp: fileOp.detected,
      fileOpType: fileOp.operation
    });
  }
  
  /**
   * Handle tool_call event (from stream-json)
   */
  handleToolCallEvent(event) {
    const metadata = formatToolMetadata(event);
    const toolName = metadata.toolName;
    
    // Detect file operations
    let summary = '';
    if (metadata.command) {
      summary = `Running: ${metadata.command.substring(0, 60)}${metadata.command.length > 60 ? '...' : ''}`;
    } else if (metadata.filePath) {
      summary = `Using ${toolName}: ${metadata.filePath}`;
    } else {
      summary = `Using ${toolName}`;
    }
    
    const fileOp = detectFileOp(summary, toolName);
    
    this.emitStructuredOutput(OutputType.TOOL_CALL, summary, {
      ...metadata,
      isFileOp: fileOp.detected,
      fileOpType: fileOp.operation
    });
  }
  
  /**
   * Handle tool_result event
   */
  handleToolResultEvent(event) {
    const toolUseId = event.tool_use_id || event.toolCallId;
    const content = event.content || '';
    
    // Truncate long outputs
    let displayContent = content;
    if (typeof content === 'string' && content.length > 500) {
      displayContent = content.substring(0, 500) + '\n... (truncated)';
    } else if (typeof content === 'object') {
      displayContent = JSON.stringify(content, null, 2);
      if (displayContent.length > 500) {
        displayContent = displayContent.substring(0, 500) + '\n... (truncated)';
      }
    }
    
    this.emitStructuredOutput(OutputType.CLAUDE_RESPONSE, displayContent, {
      toolUseId: toolUseId,
      isToolResult: true,
      originalLength: typeof content === 'string' ? content.length : 
                      typeof content === 'object' ? JSON.stringify(content).length : 0
    });
  }
  
  /**
   * Handle error event
   */
  handleErrorEvent(event) {
    const errorMsg = event.error || event.message || JSON.stringify(event);
    const subtype = event.subtype || '';
    
    let metadata = { subtype };
    if (event.code) metadata.code = event.code;
    if (event.details) metadata.details = event.details;
    
    this.emitStructuredOutput(OutputType.ERROR, errorMsg, metadata);
  }
  
  /**
   * Emit structured output message
   */
  emitStructuredOutput(outputType, content, metadata = {}) {
    const message = createStructuredOutput(outputType, content, metadata);
    this.onMessage(message);
    return message;
  }
  
  /**
   * Flush any remaining buffered content
   */
  flush() {
    if (this.buffer.trim()) {
      this.processLine(this.buffer.trim());
      this.buffer = '';
    }
  }
  
  /**
   * Reset parser state
   */
  reset() {
    this.buffer = '';
    this.pendingThinking = null;
    this.pendingToolCall = null;
    this.messageBuffer = [];
  }
}

// ============================================================
// CLI Interface (for testing)
// ============================================================
if (require.main === module) {
  const parser = new OutputParser({ debug: true });
  
  console.log('BTELO Output Parser - Interactive Mode');
  console.log('Paste Claude Code stream-json output or raw text...');
  console.log('Press Ctrl+D to exit\n');
  
  process.stdin.setEncoding('utf8');
  
  process.stdin.on('data', (chunk) => {
    parser.process(chunk);
  });
  
  parser.onMessage = (msg) => {
    console.log('\n[STRUCTURED]', JSON.stringify(msg, null, 2));
  };
  
  process.stdin.on('end', () => {
    parser.flush();
    process.exit(0);
  });
}

// ============================================================
// Exports
// ============================================================
module.exports = {
  OutputParser,
  OutputType,
  ToolNames,
  createStructuredOutput,
  detectFileOp,
  extractFilePath,
  parseThinking,
  formatToolMetadata
};
