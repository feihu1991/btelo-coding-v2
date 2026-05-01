#!/bin/bash
# BTELO Notify - Send Hook events to BTELO server via HTTP
# Usage: btelo-notify.sh <event_name> <json_payload>

EVENT="$1"
PAYLOAD="$2"
BTELO_URL="${BTELO_SERVER_URL:-http://localhost:8080}/api/hooks/callback"

if [ -z "$EVENT" ] || [ -z "$PAYLOAD" ]; then
    echo "Usage: btelo-notify.sh <event> <json_payload>" >&2
    exit 1
fi

# Wrap payload with event type
BODY=$(echo "$PAYLOAD" | jq -c --arg event "$EVENT" '. + {event: $event}' 2>/dev/null)

if [ -z "$BODY" ]; then
    echo "Error: Invalid JSON payload" >&2
    exit 1
fi

# Send with retry (3 attempts)
MAX_RETRIES=3
RETRY_DELAY=1

for i in $(seq 1 $MAX_RETRIES); do
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BTELO_URL" \
        -H "Content-Type: application/json" \
        -d "$BODY" 2>/dev/null)
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    
    if [ "$HTTP_CODE" = "200" ]; then
        exit 0
    fi
    
    if [ "$i" -lt "$MAX_RETRIES" ]; then
        sleep $RETRY_DELAY
    fi
done

echo "Warning: Failed to notify BTELO server after $MAX_RETRIES attempts" >&2
exit 1
