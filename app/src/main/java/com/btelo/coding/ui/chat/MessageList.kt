package com.btelo.coding.ui.chat

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

    LaunchedEffect(messages.size, isStreaming, streamingContent) {
        if (messages.isNotEmpty() || isStreaming) {
            listState.animateScrollToItem(
                if (isStreaming) messages.size else messages.size - 1
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        items(messages) { message ->
            MessageBubble(message = message)
        }

        if (isStreaming && streamingContent.isNotBlank()) {
            item {
                AiStreamingBubble(partialContent = streamingContent)
            }
        }
    }
}
