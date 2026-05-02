package com.btelo.coding.ui.chat

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.btelo.coding.domain.model.Message

@Composable
fun MessageList(
    messages: List<Message>,
    streamingContent: String = "",
    isStreaming: Boolean = false,
    thinkingSession: ThinkingSession? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll when new messages arrive (always scroll to bottom)
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Auto-scroll when streaming content updates or thinking session changes
    LaunchedEffect(streamingContent, thinkingSession?.currentMessage, thinkingSession?.isActive) {
        if (messages.isNotEmpty() || thinkingSession?.isActive == true) {
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 260.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            MessageBubble(message = message)
        }

        // Thinking box (shows all thinking messages in one collapsible box)
        if (thinkingSession != null && thinkingSession.isActive) {
            item(key = "thinking_box") {
                ThinkingBox(session = thinkingSession)
            }
        }

        // Regular streaming content (text response)
        if (isStreaming && streamingContent.isNotBlank() && !streamingContent.startsWith("…")) {
            item(key = "streaming") {
                AiStreamingBubble(partialContent = streamingContent)
            }
        }
    }
}
