package com.btelo.coding.ui.chat

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
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

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(streamingContent, thinkingSession?.currentMessage, thinkingSession?.isActive, thinkingSession?.isCompleted) {
        if (listState.layoutInfo.totalItemsCount > 0) {
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            MessageBubble(message = message)
        }

        if (thinkingSession != null && (thinkingSession.isActive || thinkingSession.isCompleted)) {
            item(key = "thinking_box") {
                ThinkingBox(session = thinkingSession)
            }
        }

        if (isStreaming && streamingContent.isNotBlank() && streamingContent != "...") {
            item(key = "streaming") {
                AiStreamingBubble(partialContent = streamingContent)
            }
        }
    }
}
