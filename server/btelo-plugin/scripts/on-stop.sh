#!/bin/bash
# Hook: Stop — Claude Code finished a task

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/btelo-notify.sh"

INPUT=$(cat)

# Skip if already active (prevent double notification)
STOP_HOOK_ACTIVE=$(echo "$INPUT" | jq -r '.stop_hook_active // false' 2>/dev/null)
if [ "$STOP_HOOK_ACTIVE" = "true" ]; then
    exit 0
fi

SESSION_ID=$(echo "$INPUT" | jq -r '.session_id // empty' 2>/dev/null)
CWD=$(echo "$INPUT" | jq -r '.cwd // empty' 2>/dev/null)
TRANSCRIPT_PATH=$(echo "$INPUT" | jq -r '.transcript_path // empty' 2>/dev/null)

# Small delay to allow transcript to flush
sleep 1

QUERY=""
RESPONSE=""

if [ -n "$TRANSCRIPT_PATH" ] && [ -f "$TRANSCRIPT_PATH" ]; then
    # Extract last user prompt
    QUERY=$(jq -rs '
        [
            .[] | select(.type == "user") |
            if .message.content | type == "string" then .
            elif [.message.content[] | select(.type == "text")] | length > 0 then .
            else empty
            end
        ] | last |
        if .message.content | type == "array"
        then [.message.content[] | select(.type == "text") | .text] | join(" ")
        else .message.content // empty
        end
    ' "$TRANSCRIPT_PATH" 2>/dev/null)

    # Extract last assistant response
    RESPONSE=$(jq -rs '
        [.[] | select(.type == "assistant" and .message.content)] | last |
        [.message.content[] | select(.type == "text") | .text] | join(" ")
    ' "$TRANSCRIPT_PATH" 2>/dev/null)

    # Truncate for display
    if [ -n "$QUERY" ] && [ ${#QUERY} -gt 200 ]; then
        QUERY="${QUERY:0:197}..."
    fi
    if [ -n "$RESPONSE" ] && [ ${#RESPONSE} -gt 500 ]; then
        RESPONSE="${RESPONSE:0:497}..."
    fi
fi

PAYLOAD=$(jq -nc \
    --arg session_id "$SESSION_ID" \
    --arg cwd "$CWD" \
    --arg query "$QUERY" \
    --arg response "$RESPONSE" \
    '{session_id: $session_id, cwd: $cwd, query: $query, response: $response}')

btelo-notify "task_complete" "$PAYLOAD"
