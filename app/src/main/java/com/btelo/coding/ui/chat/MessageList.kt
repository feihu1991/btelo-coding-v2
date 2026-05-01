package com.btelo.coding.ui.chat

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.btelo.coding.domain.model.Message

@Composable
fun MessageList(
    messages: List<Message>,
    streamingContent: String = "",
    isStreaming: Boolean = false,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Scroll to bottom on initial load only
    val wasEmpty = remember { messages.isEmpty() }
    LaunchedEffect(messages.isNotEmpty()) {
        if (messages.isNotEmpty() && wasEmpty) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    // Auto-scroll when new messages arrive and user is near bottom
    val shouldAutoScroll by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            totalItems == 0 || lastVisibleItem >= totalItems - 2
        }
    }

    LaunchedEffect(messages.size, streamingContent) {
        if (shouldAutoScroll && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        items(messages, key = { it.id }) { message ->
            MessageBubble(message = message)
        }

        if (isStreaming && streamingContent.isNotBlank()) {
            item(key = "streaming") {
                AiStreamingBubble(partialContent = streamingContent)
            }
        }
    }
}
