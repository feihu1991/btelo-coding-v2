#!/bin/bash
# Hook: PermissionRequest — Claude wants to run a tool

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/btelo-notify.sh"

INPUT=$(cat)

SESSION_ID=$(echo "$INPUT" | jq -r '.session_id // empty' 2>/dev/null)
TOOL_NAME=$(echo "$INPUT" | jq -r '.tool_name // "unknown"' 2>/dev/null)
TOOL_INPUT=$(echo "$INPUT" | jq -r '.tool_input // ""' 2>/dev/null)

# Truncate tool input preview
if [ ${#TOOL_INPUT} -gt 500 ]; then
    TOOL_INPUT="${TOOL_INPUT:0:497}..."
fi

PAYLOAD=$(jq -nc \
    --arg session_id "$SESSION_ID" \
    --arg tool_name "$TOOL_NAME" \
    --arg tool_input "$TOOL_INPUT" \
    '{session_id: $session_id, tool_name: $tool_name, tool_input: $tool_input}')

btelo-notify "permission_request" "$PAYLOAD"

# Write a file to signal that permission was requested
# The server will write the response to /tmp/btelo-permission-{session_id}
PERM_FILE="/tmp/btelo-permission-${SESSION_ID}"
echo "pending" > "$PERM_FILE"

# Poll for response (timeout 120 seconds)
TIMEOUT=120
ELAPSED=0
while [ $ELAPSED -lt $TIMEOUT ]; do
    if [ -f "$PERM_FILE" ]; then
        DECISION=$(cat "$PERM_FILE" 2>/dev/null)
        if [ "$DECISION" = "allow" ] || [ "$DECISION" = "deny" ]; then
            rm -f "$PERM_FILE"
            if [ "$DECISION" = "allow" ]; then
                exit 0
            else
                exit 1
            fi
        fi
    fi
    sleep 1
    ELAPSED=$((ELAPSED + 1))
done

# Timeout = deny
rm -f "$PERM_FILE"
exit 1
