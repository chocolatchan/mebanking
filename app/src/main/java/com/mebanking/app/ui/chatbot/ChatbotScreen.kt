package com.mebanking.app.ui.chatbot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.domain.model.ActionChip
import com.mebanking.app.ui.theme.MEbankingTheme

@Composable
fun ChatbotScreen(
    onBack: () -> Unit,
    onNavigateToTarget: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatbotViewModel = viewModel(
        factory = (LocalContext.current.applicationContext as MEbankingApplication).container.viewModelFactory,
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChatbotContent(
        uiState = uiState,
        onBack = onBack,
        onSend = viewModel::send,
        onNavigateToTarget = onNavigateToTarget,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotContent(
    uiState: ChatbotUiState,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onNavigateToTarget: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Trợ lý ảo") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("‹ Quay lại") }
                },
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SUGGESTIONS) { suggestion ->
                        AssistChip(
                            onClick = {
                                input = ""
                                onSend(suggestion)
                            },
                            label = { Text(suggestion) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Nhập câu hỏi…") },
                        maxLines = 3,
                        enabled = !uiState.isSending,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (input.isNotBlank()) {
                                onSend(input)
                                input = ""
                            }
                        },
                        enabled = input.isNotBlank() && !uiState.isSending,
                    ) {
                        Text("Gửi")
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                ChatBubble(
                    message = message,
                    onNavigateToTarget = onNavigateToTarget,
                )
            }
            if (uiState.isSending) {
                item {
                    Text(
                        text = "Đang trả lời…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (uiState.errorMessage != null) {
                item {
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    onNavigateToTarget: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = if (message.fromUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.fromUser) 16.dp else 4.dp,
                bottomEnd = if (message.fromUser) 4.dp else 16.dp,
            ),
            modifier = Modifier.fillMaxWidth(0.88f),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = if (message.fromUser) "Bạn" else "Trợ lý M.Ebanking",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(text = message.text, style = MaterialTheme.typography.bodyLarge)
                if (message.chips.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(message.chips) { chip ->
                            AssistChip(
                                onClick = { onNavigateToTarget(chip.targetScreen) },
                                label = { Text(chip.label) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private val SUGGESTIONS = listOf(
    "Kiểm tra số dư",
    "Chuyển tiền",
    "Thanh toán hóa đơn",
    "Tìm ATM gần nhất",
)

@Preview(showBackground = true)
@Composable
private fun ChatbotPreview() {
    MEbankingTheme {
        ChatbotContent(
            uiState = ChatbotUiState(
                messages = listOf(
                    ChatMessage(0, false, "Xin chào! Tôi là trợ lý ảo M.Ebanking."),
                    ChatMessage(1, true, "Chuyển tiền"),
                    ChatMessage(
                        2,
                        false,
                        "Bạn muốn chuyển tiền đến tài khoản nội bộ hay liên ngân hàng?",
                        listOf(ActionChip("Chuyển tiền", "transfer")),
                    ),
                ),
            ),
            onBack = {},
            onSend = {},
            onNavigateToTarget = {},
        )
    }
}
