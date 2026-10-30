package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.PrismAmber
import com.example.ui.theme.PrismPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TermuxGuideScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

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
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = CyberCyan
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Termux Setup Guide",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Run local LLMs on Android loopback (127.0.0.1)",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            }
        }

        // Section 1: Prevent Sleep (Wake Lock)
        item {
            GuideSectionCard(
                title = "1. Prevent Background Sleep",
                subtitle = "Keep Termux CPU active while chatting",
                accentColor = PrismAmber,
                icon = Icons.Default.Warning,
                command = "termux-wake-lock",
                description = "Android battery management puts background processes to sleep. Run this first inside Termux to keep your model server alive when switching apps.",
                context = context
            )
        }

        // Section 2: Standard llama.cpp
        item {
            GuideSectionCard(
                title = "2. Standard llama.cpp (Port 8080)",
                subtitle = "For standard GGUF models (Q4_K, Q8, etc.)",
                accentColor = CyberCyan,
                icon = Icons.Default.Memory,
                command = "./llama-server -m ~/models/qwen2.5-1.5b-instruct.gguf --host 127.0.0.1 --port 8080 -c 2048",
                description = "Runs the official llama.cpp HTTP server on loopback port 8080. It exposes the OpenAI-compatible /v1/chat/completions endpoint with SSE streaming.",
                context = context
            )
        }

        // Section 3: Prism ML Engine
        item {
            GuideSectionCard(
                title = "3. Prism ML Engine (Port 8081)",
                subtitle = "For specialized ultra-low-bit / ternary models",
                accentColor = PrismPurple,
                icon = Icons.Default.Bolt,
                command = "prism-server --model ~/models/bitnet_1.58.prism --host 127.0.0.1 --port 8081",
                description = "Prism ML handles 1.58-bit ternary quantized weights for ultra-low memory and power usage on ARM mobile chips.",
                context = context
            )
        }

        // Section 4: Loopback Verification
        item {
            GuideSectionCard(
                title = "4. Quick Loopback Test (curl)",
                subtitle = "Verify local server is responding",
                accentColor = NeonEmerald,
                icon = Icons.Default.PlayArrow,
                command = "curl -i http://127.0.0.1:8080/v1/models",
                description = "You can run this inside Termux to verify that the port is open and binding to 127.0.0.1 properly before chatting in the app.",
                context = context
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianCardBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💡 Loopback Architecture Tips",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Both the app and Termux run on the same device, so 127.0.0.1 (localhost) is shared.\n• Always bind with --host 127.0.0.1 (binding to 0.0.0.0 also works).\n• You can switch the target engine anytime in 'Engine Profiles' without rebuilding.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, lineHeight = 20.sp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun GuideSectionCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    command: String,
    description: String,
    context: Context
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Monospace Command Box
            Surface(
                color = Color(0xFF070B13),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$ $command",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = accentColor,
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Termux Command", command)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Command copied!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Command",
                            tint = TextMuted
                        )
                    }
                }
            }
        }
    }
}
