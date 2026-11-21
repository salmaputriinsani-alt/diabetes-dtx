package com.example.modules.chatbot

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.LocalizedCopy
import com.example.data.model.AppLanguage
import com.example.data.model.BotChatMessage
import com.example.ui.theme.BorderBeige
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.DeepInk
import com.example.ui.theme.DeepSage
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.SoftSageBg
import com.example.ui.theme.Terracotta
import com.example.ui.theme.WarmCream

@Composable
fun FloatingDropletMascotButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = CrispWhite,
        border = BorderStroke(2.dp, DeepSage),
        shadowElevation = 10.dp,
        modifier = modifier
            .size(62.dp)
            .shadow(10.dp, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("floating_droplet_chatbot_button")
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_droplet_mascot),
            contentDescription = "Open AURELIX AI Companion",
            contentScale = ContentScale.Crop,
            modifier = Modifier.padding(3.dp).clip(CircleShape)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotBottomSheet(
    messages: List<BotChatMessage>,
    isThinking: Boolean,
    language: AppLanguage,
    copy: LocalizedCopy,
    onAsk: (String) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var input by remember { mutableStateOf("") }

    val quickChips = when (language) {
        AppLanguage.MS -> listOf(
            "Mengapa gula saya naik?",
            "Adakah Nasi Lemak selamat sekarang?",
            "Ringkaskan minggu saya untuk doktor"
        )
        AppLanguage.ZH -> listOf(
            "为什么我的血糖会飙升？",
            "现在吃椰浆饭安全吗？",
            "为医生总结我的本周情况"
        )
        AppLanguage.EN -> listOf(
            "Why did my sugar spike?",
            "Is Nasi Lemak safe right now?",
            "Summarize my week for my doctor"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WarmCream
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.img_droplet_mascot),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(38.dp).clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = copy.chatbotTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepInk
                        )
                        Text(
                            text = "Gemini 3.8 Flash · Context-Aware (${language.label})",
                            style = MaterialTheme.typography.labelMedium,
                            color = DeepSage
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close chatbot", tint = MutedSlate)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.isFromUser) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (msg.isFromUser) DeepSage else CrispWhite,
                            border = if (msg.isFromUser) null else BorderStroke(1.dp, BorderBeige),
                            modifier = Modifier.widthIn(max = 310.dp)
                        ) {
                            Text(
                                text = if (msg.text.isBlank() && msg.isStreaming) "Thinking..." else msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (msg.isFromUser) CrispWhite else DeepInk,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // Quick-tap chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickChips.forEachIndexed { idx, chip ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SoftSageBg,
                        border = BorderStroke(1.dp, DeepSage.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !isThinking) { onAsk(chip) }
                            .testTag("chatbot_chip_$idx")
                    ) {
                        Text(
                            text = chip,
                            style = MaterialTheme.typography.labelMedium,
                            color = DeepSage,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(copy.chatbotPlaceholder, style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepSage,
                        unfocusedBorderColor = BorderBeige,
                        focusedContainerColor = CrispWhite,
                        unfocusedContainerColor = CrispWhite
                    ),
                    modifier = Modifier.weight(1f).testTag("chatbot_input")
                )
                Surface(
                    shape = CircleShape,
                    color = if (isThinking) Terracotta else DeepSage,
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(
                        enabled = !isThinking && input.isNotBlank(),
                        onClick = {
                            onAsk(input)
                            input = ""
                        },
                        modifier = Modifier.testTag("chatbot_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send to AI Companion",
                            tint = CrispWhite
                        )
                    }
                }
            }
        }
    }
}
