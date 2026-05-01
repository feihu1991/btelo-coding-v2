#!/bin/bash
# Hook: SessionStart — Claude Code session started or resumed

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/btelo-notify.sh"

INPUT=$(cat)

SESSION_ID=$(echo "$INPUT" | jq -r '.session_id // empty' 2>/dev/null)
CWD=$(echo "$INPUT" | jq -r '.cwd // empty' 2>/dev/null)
MODE=$(echo "$INPUT" | jq -r '.mode // "startup"' 2>/dev/null)

PAYLOAD=$(jq -nc \
    --arg session_id "$SESSION_ID" \
    --arg cwd "$CWD" \
    --arg mode "$MODE" \
    '{session_id: $session_id, cwd: $cwd, mode: $mode}')

btelo-notify "session_start" "$PAYLOAD"
