package com.whirlyfan.breezy.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whirlyfan.breezy.api.Message

@Composable
fun MessagesList(
    messages: List<Message>,
    currentUserId: String,
    modifier: Modifier = Modifier,
    onUnsendMessage: (String) -> Unit = {},
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        reverseLayout = false,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(messages) { message ->
            MessageItem(
                message = message,
                isFromCurrentUser = message.userId == currentUserId,
                onUnsendMessage = onUnsendMessage,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}