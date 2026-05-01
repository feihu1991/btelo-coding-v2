#!/bin/bash
# Hook: PostToolUse — Tool execution completed

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/btelo-notify.sh"

INPUT=$(cat)

SESSION_ID=$(echo "$INPUT" | jq -r '.session_id // empty' 2>/dev/null)
TOOL_NAME=$(echo "$INPUT" | jq -r '.tool_name // "unknown"' 2>/dev/null)
RESULT=$(echo "$INPUT" | jq -r '.result // ""' 2>/dev/null)

# Truncate result
if [ ${#RESULT} -gt 300 ]; then
    RESULT="${RESULT:0:297}..."
fi

PAYLOAD=$(jq -nc \
    --arg session_id "$SESSION_ID" \
    --arg tool_name "$TOOL_NAME" \
    --arg result_summary "$RESULT" \
    '{session_id: $session_id, tool_name: $tool_name, result_summary: $result_summary}')

btelo-notify "tool_completed" "$PAYLOAD"
