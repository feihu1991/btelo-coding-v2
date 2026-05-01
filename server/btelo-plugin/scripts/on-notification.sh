#!/bin/bash
# Hook: Notification — Claude is idle and waiting for input

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/btelo-notify.sh"

INPUT=$(cat)

SESSION_ID=$(echo "$INPUT" | jq -r '.session_id // empty' 2>/dev/null)
MSG=$(echo "$INPUT" | jq -r '.message // "Input needed"' 2>/dev/null)
[ -z "$MSG" ] && MSG="Input needed"

PAYLOAD=$(jq -nc \
    --arg session_id "$SESSION_ID" \
    --arg message "$MSG" \
    '{session_id: $session_id, message: $message}')

btelo-notify "waiting_input" "$PAYLOAD"
