#!/bin/bash
# Hook: UserPromptSubmit — User sent a message

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/btelo-notify.sh"

INPUT=$(cat)

SESSION_ID=$(echo "$INPUT" | jq -r '.session_id // empty' 2>/dev/null)
CWD=$(echo "$INPUT" | jq -r '.cwd // empty' 2>/dev/null)

PAYLOAD=$(jq -nc \
    --arg session_id "$SESSION_ID" \
    --arg cwd "$CWD" \
    '{session_id: $session_id, cwd: $cwd}')

btelo-notify "prompt_submitted" "$PAYLOAD"
