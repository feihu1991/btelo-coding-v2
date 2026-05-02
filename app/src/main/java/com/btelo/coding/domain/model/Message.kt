package com.btelo.coding.domain.model

/**
 * Message model for BTELO Coding v2
 * 
 * Extended with structured output support:
 * - outputType: Type of Claude Code output
 * - metadata: Additional metadata for structured rendering
 * - thinkingContent: Thinking process (collapsible)
 */
data class Message(
    val id: String,
    val sessionId: String,
    val content: String,
    val type: MessageType,
    val timestamp: Long,
    val isFromUser: Boolean,
    val sender: String = "",
    val tools: List<ToolExecution>? = null,
    
    // BTELO Coding v2: Structured Output Support
    val outputType: OutputType? = null,
    val metadata: MessageMetadata? = null,
    val thinkingContent: String? = null
)

/**
 * Message metadata for structured output rendering
 */
data class MessageMetadata(
    val sourceKind: String? = null,
    val terminality: String? = null,
    val fingerprint: String? = null,
    val toolId: String? = null,
    val toolName: String? = null,
    val toolType: String? = null,
    val filePath: String? = null,
    val command: String? = null,
    val parameters: Map<String, Any>? = null,
    val isFileOp: Boolean = false,
    val fileOpType: String? = null,
    val isToolResult: Boolean = false,
    val isCollapsed: Boolean = false,
    val originalLength: Int = 0,
    val errorCode: String? = null,
    val errorDetails: String? = null,
    val toolNames: List<String>? = null
)

/**
 * Output type enum for Claude Code structured output
 */
enum class OutputType {
    /** Regular text response from Claude */
    CLAUDE_RESPONSE,
    
    /** Tool usage call (Bash, Read, Edit, etc.) */
    TOOL_CALL,
    
    /** File operation details */
    FILE_OP,
    
    /** Thinking/thought process */
    THINKING,
    
    /** Error message */
    ERROR,
    
    /** System message */
    SYSTEM
}

enum class MessageType {
    TEXT, COMMAND, OUTPUT, ERROR, TOOL, THINKING
}

data class ToolExecution(
    val type: ToolType,
    val command: String,
    val output: String? = null,
    val status: ToolStatus,
    val filePath: String? = null,
    val parameters: Map<String, Any>? = null,
    val isCollapsed: Boolean = false
)

enum class ToolType {
    BASH, READ, EDIT, WRITE, GREP, GLOB, LSP, WEBFETCH, MCP
}

enum class ToolStatus {
    SUCCESS, ERROR, RUNNING
}
