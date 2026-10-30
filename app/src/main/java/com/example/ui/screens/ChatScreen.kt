package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatMessageEntity
import com.example.data.model.EngineType
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PrismPurple
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TerminalPrompt
import com.example.ui.theme.TerminalRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ChatUiState
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var showEngineMenu by remember { mutableStateOf(false) }
    var showSessionsDialog by remember { mutableStateOf(false) }

    // Scroll to bottom when new messages or tokens arrive
    LaunchedEffect(uiState.messages.size, uiState.streamingContent.length) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // Top App Bar with Engine Switcher & Status
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showEngineMenu = true }
                ) {
                    // Connection Status Dot
                    val dotColor = when (uiState.connectionState) {
                        ConnectionState.CONNECTED -> TerminalGreen
                        ConnectionState.CHECKING -> CyberCyan
                        ConnectionState.DISCONNECTED -> TerminalRed
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.activeEngine.displayTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Switch Engine",
                                tint = CyberCyan,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(start = 4.dp)
                            )
                        }
                        Text(
                            text = when (uiState.connectionState) {
                                ConnectionState.CONNECTED -> "Online • ${uiState.connectionLatencyMs}ms"
                                ConnectionState.CHECKING -> "Checking loopback..."
                                ConnectionState.DISCONNECTED -> "Offline • tap to ping"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = if (uiState.connectionState == ConnectionState.CONNECTED) TerminalGreen else TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }

                    // Engine Quick Switch Dropdown
                    DropdownMenu(
                        expanded = showEngineMenu,
                        onDismissRequest = { showEngineMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("llama.cpp (:8080 GGUF)") },
                            onClick = {
                                viewModel.switchEngine(EngineType.LLAMA_CPP)
                                showEngineMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Memory, contentDescription = null, tint = CyberCyan)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Prism ML (:8081 Ternary)") },
                            onClick = {
                                viewModel.switchEngine(EngineType.PRISM_ML)
                                showEngineMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = PrismPurple)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Configure Custom Port...") },
                            onClick = {
                                showEngineMenu = false
                                onNavigateToSettings()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = TextSecondary)
                            }
                        )
                    }
                }
            },
            actions = {
                IconButton(
                    onClick = { viewModel.checkConnection() },
                    modifier = Modifier.testTag("refresh_connection_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Test Connection",
                        tint = TextSecondary
                    )
                }
                IconButton(
                    onClick = { viewModel.createNewSession() },
                    modifier = Modifier.testTag("new_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat",
                        tint = CyberCyan
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = TextSecondary
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Conversations (${uiState.sessions.size})") },
                            onClick = {
                                showMenu = false
                                showSessionsDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear Current Chat") },
                            onClick = {
                                viewModel.clearCurrentMessages()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Conversation") },
                            onClick = {
                                viewModel.deleteCurrentSession()
                                showMenu = false
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianSurface)
        )

        // Error Banner if disconnected or inference failed
        AnimatedVisibility(visible = uiState.errorMessage != null && !uiState.isStreaming) {
            Surface(
                color = TerminalRed.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalRed.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.checkConnection() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Messages list or Empty State
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (uiState.messages.isEmpty() && !uiState.isStreaming) {
                EmptyChatWelcome(
                    engineTitle = uiState.activeEngine.displayTitle,
                    engineFormat = uiState.activeEngine.modelFormatNote,
                    onPromptSelect = { prompt ->
                        viewModel.sendMessage(prompt)
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        ChatMessageBubble(
                            message = message,
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("AI Message", message.content)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                viewModel.deleteMessage(message.id)
                            }
                        )
                    }

                    // Live Streaming Bubble
                    if (uiState.isStreaming) {
                        item(key = "streaming_bubble") {
                            StreamingBubble(
                                content = uiState.streamingContent,
                                tokens = uiState.streamingTokens,
                                elapsedMs = uiState.streamingElapsedMs,
                                engineName = uiState.activeEngine.displayTitle
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Floating Stop Button when Streaming
            if (uiState.isStreaming) {
                Button(
                    onClick = { viewModel.stopStreaming() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerminalRed,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .testTag("stop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Stop Generation (${uiState.streamingTokens} tokens)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Bottom Input Bar
        ChatInputBar(
            inputText = uiState.inputText,
            onTextChanged = { viewModel.onInputTextChanged(it) },
            onSend = { viewModel.sendMessage() },
            isStreaming = uiState.isStreaming,
            onStop = { viewModel.stopStreaming() }
        )
    }

    // Sessions Dialog
    if (showSessionsDialog) {
        ConversationsDialog(
            sessions = uiState.sessions,
            currentSessionId = uiState.currentSessionId,
            onSelect = {
                viewModel.selectSession(it)
                showSessionsDialog = false
            },
            onDismiss = { showSessionsDialog = false }
        )
    }
}

@Composable
fun EmptyChatWelcome(
    engineTitle: String,
    engineFormat: String,
    onPromptSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(CyberCyan.copy(alpha = 0.1f))
                .border(1.dp, CyberCyan.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Local Termux AI",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
            color = ObsidianCard,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NeonEmerald)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$engineTitle • 127.0.0.1",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = engineFormat,
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Try a starter prompt:",
            style = MaterialTheme.typography.labelMedium.copy(color = TextMuted)
        )

        Spacer(modifier = Modifier.height(12.dp))

        val promptSuggestions = listOf(
            "Hello! What model are you running locally?",
            "Write a Python script to benchmark local inference speed",
            "Explain how llama.cpp runs on Android ARM architecture",
            "What are the benefits of 1.58-bit ternary quantized weights?"
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            promptSuggestions.forEach { prompt ->
                Surface(
                    color = ObsidianCard,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPromptSelect(prompt) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ">",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessageEntity,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Assistant avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CyberCyan.copy(alpha = 0.15f))
                    .border(1.dp, CyberCyan.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ">_",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Surface(
                color = if (isUser) CyberCyan.copy(alpha = 0.15f) else ObsidianCard,
                shape = RoundedCornerShape(
                    topStart = 14.dp,
                    topEnd = 14.dp,
                    bottomStart = if (isUser) 14.dp else 2.dp,
                    bottomEnd = if (isUser) 2.dp else 14.dp
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isUser) CyberCyan.copy(alpha = 0.4f) else ObsidianCardBorder
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.content.ifBlank { "(empty message)" },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (message.isError) TerminalRed else TextPrimary,
                            lineHeight = 22.sp
                        )
                    )

                    // Assistant metrics footer
                    if (!isUser && message.durationMs > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val seconds = message.durationMs / 1000.0
                        val tokensPerSec = if (seconds > 0 && message.tokensEstimated > 0) {
                            String.format("%.1f", message.tokensEstimated / seconds)
                        } else {
                            "-"
                        }
                        Text(
                            text = "${message.tokensEstimated} tokens • ${String.format("%.1f", seconds)}s • $tokensPerSec t/s • ${message.engineUsed}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // Quick actions under bubble
            Row(
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy message",
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete message",
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StreamingBubble(
    content: String,
    tokens: Int,
    elapsedMs: Long,
    engineName: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(CyberCyan.copy(alpha = 0.2f))
                .border(1.dp, CyberCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = ">_",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.widthIn(max = 320.dp)) {
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 2.dp, bottomEnd = 14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (content.isEmpty()) "Generating tokens from Termux..." else content,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (content.isEmpty()) TextMuted else TextPrimary,
                            lineHeight = 22.sp
                        )
                    )

                    // Blinking cursor
                    Box(
                        modifier = Modifier
                            .alpha(cursorAlpha)
                            .padding(top = 4.dp)
                            .size(width = 8.dp, height = 14.dp)
                            .background(CyberCyan)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    val sec = elapsedMs / 1000.0
                    val speed = if (sec > 0) String.format("%.1f", tokens / sec) else "0.0"
                    Text(
                        text = "⚡ Generating: $tokens tokens • ${String.format("%.1f", sec)}s ($speed t/s)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    inputText: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    isStreaming: Boolean,
    onStop: () -> Unit
) {
    Surface(
        color = ObsidianSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onTextChanged,
                placeholder = {
                    Text(
                        text = "Message local Termux engine...",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                    )
                },
                maxLines = 4,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input"),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = ObsidianCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (isStreaming) {
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(TerminalRed)
                        .testTag("stop_button_input")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Generation",
                        tint = TextPrimary
                    )
                }
            } else {
                IconButton(
                    onClick = onSend,
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) CyberCyan else ObsidianCard)
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Message",
                        tint = if (inputText.isNotBlank()) Color(0xFF0B0F19) else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationsDialog(
    sessions: List<com.example.data.local.ChatSessionEntity>,
    currentSessionId: Long,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Saved Conversations") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sessions) { session ->
                    Surface(
                        color = if (session.id == currentSessionId) CyberCyan.copy(alpha = 0.15f) else ObsidianCard,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (session.id == currentSessionId) CyberCyan else ObsidianCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(session.id) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = session.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = session.engineName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberCyan
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
