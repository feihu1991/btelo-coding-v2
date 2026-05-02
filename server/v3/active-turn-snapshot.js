const ACTIVE_TEXT_TAIL_LIMIT = 4000;

function normalizePendingInputs(pendingInputs = []) {
  return pendingInputs.map((input) => ({
    client_message_id: input.id,
    content_preview: preview(input.content, 160),
    status: input.status || 'pending',
    created_at: input.createdAt || Date.now(),
    updated_at: input.updatedAt || input.createdAt || Date.now()
  }));
}

function preview(value, limit) {
  const text = String(value || '').replace(/\s+/g, ' ').trim();
  if (text.length <= limit) return text;
  return `${text.slice(0, Math.max(0, limit - 1))}...`;
}

function tail(value, limit = ACTIVE_TEXT_TAIL_LIMIT) {
  const text = String(value || '');
  if (text.length <= limit) return text;
  return text.slice(text.length - limit);
}

function eventTerminality(event) {
  if (!event) return 'advisory';
  if (event.role === 'user') return 'terminal';
  if (event.kind === 'text' || event.kind === 'error') return 'terminal';
  return 'advisory';
}

function activeToolsFrom(events) {
  return events
    .filter((event) => event.role === 'assistant' && (event.kind === 'tool_call' || event.kind === 'file_op'))
    .map((event) => ({
      event_id: event.id,
      tool_id: event.metadata && event.metadata.toolId ? event.metadata.toolId : null,
      tool_name: event.metadata && event.metadata.toolName ? event.metadata.toolName : null,
      kind: event.kind,
      content: event.content || '',
      started_at: event.timestamp || 0
    }));
}

function currentTurnEvents(events) {
  const lastUserIndex = findLastIndex(events, (event) => event.role === 'user');
  if (lastUserIndex < 0) return [];
  return events.slice(lastUserIndex);
}

function findLastIndex(items, predicate) {
  for (let i = items.length - 1; i >= 0; i--) {
    if (predicate(items[i], i)) return i;
  }
  return -1;
}

function buildActiveTurnSnapshot({
  relaySessionId,
  claudeSessionId,
  workspaceRoot,
  cursor = 0,
  events = [],
  pendingInputs = []
} = {}) {
  const pending = normalizePendingInputs(pendingInputs);
  const turnEvents = currentTurnEvents(events);
  const lastEvent = turnEvents.length ? turnEvents[turnEvents.length - 1] : null;
  const lastUser = [...turnEvents].reverse().find((event) => event.role === 'user') || null;
  const assistantEvents = turnEvents.filter((event) => event.role === 'assistant');
  const assistantText = assistantEvents
    .filter((event) => event.kind === 'text')
    .map((event) => event.content || '')
    .join('');
  const activeTools = activeToolsFrom(turnEvents);
  const lastTerminal = [...turnEvents].reverse().find((event) => eventTerminality(event) === 'terminal') || null;

  let status = 'idle';
  let isActive = false;

  if (pending.length > 0) {
    status = 'input_pending';
    isActive = true;
  } else if (lastUser && assistantEvents.length === 0) {
    status = 'waiting_for_assistant';
    isActive = true;
  } else if (lastEvent && lastEvent.role === 'assistant' && eventTerminality(lastEvent) === 'advisory') {
    status = activeTools.length > 0 ? 'tool_activity' : 'assistant_activity';
    isActive = true;
  }

  return {
    type: 'active_turn_snapshot',
    version: 3,
    session_id: relaySessionId,
    claude_session_id: claudeSessionId,
    workspace_root: workspaceRoot || null,
    cursor,
    active_turn: {
      is_active: isActive,
      status,
      pending_inputs: pending,
      last_user_event_id: lastUser ? lastUser.id : null,
      last_assistant_event_id: assistantEvents.length ? assistantEvents[assistantEvents.length - 1].id : null,
      last_terminal_event_id: lastTerminal ? lastTerminal.id : null,
      active_tools: isActive ? activeTools : [],
      text_tail: isActive ? tail(assistantText) : '',
      updated_at: Date.now()
    }
  };
}

module.exports = {
  buildActiveTurnSnapshot
};
