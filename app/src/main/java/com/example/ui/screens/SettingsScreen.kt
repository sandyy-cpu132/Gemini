package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EngineType
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.PrismAmber
import com.example.ui.theme.PrismPurple
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TerminalRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberCyan.copy(alpha = 0.15f))
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = CyberCyan
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Inference Engine Profiles",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Switch port & target engine running in Termux",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            }
        }

        // Notification Banner
        if (uiState.bannerMessage != null) {
            item {
                Surface(
                    color = NeonEmerald.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NeonEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.bannerMessage ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary)
                            )
                        }
                        IconButton(onClick = { viewModel.clearBanner() }, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 1. Presets Header
        item {
            Text(
                text = "ENGINE PRESETS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    letterSpacing = 1.sp
                )
            )
        }

        // Preset 1: llama.cpp
        item {
            PresetOptionCard(
                title = "Standard llama.cpp",
                portBadge = "Port 8080",
                formatBadge = "GGUF Format",
                description = "Standard llama-server for GGUF weights (Qwen, Llama 3, DeepSeek, Mistral).",
                accentColor = CyberCyan,
                icon = Icons.Default.Memory,
                isSelected = uiState.selectedEngineType == EngineType.LLAMA_CPP,
                onSelect = { viewModel.selectPreset(EngineType.LLAMA_CPP) }
            )
        }

        // Preset 2: Prism ML
        item {
            PresetOptionCard(
                title = "Prism ML Engine",
                portBadge = "Port 8081",
                formatBadge = "Ternary / BitNet 1.58b",
                description = "Specialized engine for ultra-low-bit ternary weights for maximum mobile speed.",
                accentColor = PrismPurple,
                icon = Icons.Default.Bolt,
                isSelected = uiState.selectedEngineType == EngineType.PRISM_ML,
                onSelect = { viewModel.selectPreset(EngineType.PRISM_ML) }
            )
        }

        // Preset 3: Custom Port / Endpoint
        item {
            PresetOptionCard(
                title = "Custom Engine Configuration",
                portBadge = "Custom Port",
                formatBadge = "OpenAI REST",
                description = "Configure custom loopback port or host IP for any local engine.",
                accentColor = PrismAmber,
                icon = Icons.Default.Settings,
                isSelected = uiState.selectedEngineType == EngineType.CUSTOM,
                onSelect = { viewModel.selectPreset(EngineType.CUSTOM) }
            )
        }

        // 2. Live Connection Ping / Diagnostics
        item {
            Text(
                text = "LOOPBACK HEALTH CHECK",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    letterSpacing = 1.sp
                )
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianCardBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Target: http://${uiState.hostInput}:${uiState.portInput}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Verifies /v1/models & loopback binding",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }

                        Button(
                            onClick = { viewModel.testCurrentConnection() },
                            enabled = !uiState.isTestingConnection,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan.copy(alpha = 0.2f),
                                contentColor = CyberCyan
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("ping_connection_button")
                        ) {
                            if (uiState.isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = CyberCyan
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ping", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Test result status
                    if (uiState.testResult != null) {
                        val result = uiState.testResult!!
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = if (result.isSuccess) TerminalGreen.copy(alpha = 0.1f) else TerminalRed.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (result.isSuccess) TerminalGreen.copy(alpha = 0.35f) else TerminalRed.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (result.isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (result.isSuccess) TerminalGreen else TerminalRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (result.isSuccess) "ONLINE • ${result.latencyMs}ms" else "OFFLINE",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (result.isSuccess) TerminalGreen else TerminalRed
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = result.message,
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary)
                                )
                                if (result.detectedModels.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Models: ${result.detectedModels.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = CyberCyan
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Network & Model Parameters
        item {
            Text(
                text = "NETWORK & MODEL SETTINGS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    letterSpacing = 1.sp
                )
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianCardBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Host and Port Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.hostInput,
                            onValueChange = { viewModel.onHostChanged(it) },
                            label = { Text("Host IP") },
                            placeholder = { Text("127.0.0.1") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("host_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = ObsidianCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedLabelColor = CyberCyan,
                                unfocusedLabelColor = TextSecondary
                            )
                        )

                        OutlinedTextField(
                            value = uiState.portInput,
                            onValueChange = { viewModel.onPortChanged(it) },
                            label = { Text("Port") },
                            placeholder = { Text("8080") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("port_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = ObsidianCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedLabelColor = CyberCyan,
                                unfocusedLabelColor = TextSecondary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Model Name Identifier
                    OutlinedTextField(
                        value = uiState.modelInput,
                        onValueChange = { viewModel.onModelChanged(it) },
                        label = { Text("Model Identifier") },
                        placeholder = { Text("default or model filename") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("model_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = ObsidianCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = CyberCyan,
                            unfocusedLabelColor = TextSecondary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // System Prompt
                    OutlinedTextField(
                        value = uiState.systemPromptInput,
                        onValueChange = { viewModel.onSystemPromptChanged(it) },
                        label = { Text("System Prompt") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("system_prompt_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = ObsidianCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = CyberCyan,
                            unfocusedLabelColor = TextSecondary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Temperature Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Temperature",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                        )
                        Text(
                            text = String.format("%.2f", uiState.temperatureInput),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Slider(
                        value = uiState.temperatureInput,
                        onValueChange = { viewModel.onTemperatureChanged(it) },
                        valueRange = 0.0f..1.5f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyan,
                            activeTrackColor = CyberCyan,
                            inactiveTrackColor = ObsidianCardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Max Tokens Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Max Tokens",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                        )
                        Text(
                            text = "${uiState.maxTokensInput}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Slider(
                        value = uiState.maxTokensInput.toFloat(),
                        onValueChange = { viewModel.onMaxTokensChanged(it.toInt()) },
                        valueRange = 256f..4096f,
                        steps = 15,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyan,
                            activeTrackColor = CyberCyan,
                            inactiveTrackColor = ObsidianCardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stream SSE Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Stream Completions (SSE)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Display tokens in real-time as generated",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                        Switch(
                            checked = uiState.streamEnabled,
                            onCheckedChange = { viewModel.onStreamToggled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberCyan,
                                checkedTrackColor = CyberCyan.copy(alpha = 0.4f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = ObsidianCardBorder
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Save Button
                    Button(
                        onClick = { viewModel.saveCustomSettings() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = Color(0xFF0B0F19)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_settings_button")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save & Apply Engine Configuration",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PresetOptionCard(
    title: String,
    portBadge: String,
    formatBadge: String,
    description: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accentColor.copy(alpha = 0.08f) else ObsidianCard
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accentColor else ObsidianCardBorder,
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(accentColor)
                            .size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color(0xFF0B0F19),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = accentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = portBadge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = formatBadge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
        }
    }
}
