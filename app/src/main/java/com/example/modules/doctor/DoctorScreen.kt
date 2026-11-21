package com.example.modules.doctor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.config.FeatureFlags
import com.example.core.LocalizedCopy
import com.example.data.model.CgmSnapshotSummary
import com.example.data.model.DoctorMessage
import com.example.data.model.PatientProfile
import com.example.data.model.SenderRole
import com.example.data.model.ShapDriver
import com.example.data.model.UiState
import com.example.ui.theme.BorderBeige
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.DeepInk
import com.example.ui.theme.DeepSage
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.SoftSageBg
import com.example.ui.theme.SoftStone
import com.example.ui.theme.Terracotta
import com.example.ui.theme.WarmCream
import java.util.Locale

@Composable
fun DoctorScreen(
    profile: PatientProfile,
    cgm: CgmSnapshotSummary,
    messagesState: UiState<List<DoctorMessage>>,
    featureFlags: FeatureFlags,
    copy: LocalizedCopy,
    onSendMessage: (text: String, includeGraph: Boolean, includeEstimate: Boolean) -> Unit,
    onConfirmTriagePlan: (Double) -> Unit
) {
    var showTriageView by remember { mutableStateOf(false) }
    var draftMessage by remember { mutableStateOf("") }
    var attachGraph by remember { mutableStateOf(false) }
    var attachEstimate by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Top Toggle: Patient Chat View vs KPJ Clinician 10-Second Triage View
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SoftStone,
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (!showTriageView) CrispWhite else SoftStone,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showTriageView = false }
                        .testTag("doctor_patient_chat_tab")
                ) {
                    Text(
                        text = copy.patientChatTab,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (!showTriageView) FontWeight.Bold else FontWeight.Medium,
                        color = if (!showTriageView) DeepSage else MutedSlate,
                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 12.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (showTriageView) CrispWhite else SoftStone,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showTriageView = true }
                        .testTag("doctor_triage_tab")
                ) {
                    Text(
                        text = copy.doctorTriageTab,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (showTriageView) FontWeight.Bold else FontWeight.Medium,
                        color = if (showTriageView) DeepSage else MutedSlate,
                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 12.dp)
                    )
                }
            }
        }

        if (showTriageView) {
            KpjClinicianTriageView(
                profile = profile,
                cgm = cgm,
                featureFlags = featureFlags,
                onConfirmTriagePlan = onConfirmTriagePlan
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Clinician Header + Embedded Mini CGM & Insulin Summary Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CrispWhite),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, BorderBeige),
                        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_dr_sarah),
                                    contentDescription = "dr. Sarah Wijaya KPJ Endocrinology",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "dr. Sarah Wijaya",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepInk
                                    )
                                    Text(
                                        text = "KPJ Endocrinology · Online",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = DeepSage,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = DeepSage,
                                    modifier = Modifier.size(10.dp)
                                ) {}
                            }

                            // Mini CGM & Insulin summary strip
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = WarmCream,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    SummaryStat("CGM Peak", "188 now")
                                    SummaryStat("Logged", "1x meal")
                                    SummaryStat(
                                        "Estimate",
                                        "${String.format(Locale.US, "%.1f", profile.simInsulinUnits)} unit"
                                    )
                                }
                            }
                        }
                    }
                }

                // Chat Thread
                when (messagesState) {
                    is UiState.Loading -> {
                        item { CircularProgressIndicator(color = DeepSage) }
                    }
                    is UiState.Error -> {
                        item {
                            Text(
                                text = messagesState.message,
                                color = Terracotta,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    is UiState.Success -> {
                        items(messagesState.data, key = { it.id.ifBlank { "${it.senderRole}_${it.timeLabel}_${it.text.hashCode()}" } }) { msg ->
                            DoctorChatBubble(
                                message = msg,
                                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                            )
                        }
                    }
                }

                // Quick Attach Chips + Input Composer
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CrispWhite),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, BorderBeige),
                        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (attachGraph) SoftSageBg else WarmCream,
                                    border = BorderStroke(1.dp, if (attachGraph) DeepSage else BorderBeige),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { attachGraph = !attachGraph }
                                        .testTag("attach_graph_chip")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Assessment,
                                            contentDescription = null,
                                            tint = DeepSage,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = copy.attachGraph,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = DeepInk,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (attachEstimate) SoftSageBg else WarmCream,
                                    border = BorderStroke(1.dp, if (attachEstimate) DeepSage else BorderBeige),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { attachEstimate = !attachEstimate }
                                        .testTag("attach_estimate_chip")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Vaccines,
                                            contentDescription = null,
                                            tint = DeepSage,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = copy.attachEstimate,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = DeepInk,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = draftMessage,
                                    onValueChange = { draftMessage = it },
                                    placeholder = {
                                        Text(
                                            text = copy.sendMsgHint,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DeepSage,
                                        unfocusedBorderColor = BorderBeige
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("doctor_message_input")
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = DeepSage,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            onSendMessage(draftMessage, attachGraph, attachEstimate)
                                            draftMessage = ""
                                            attachGraph = false
                                            attachEstimate = false
                                        },
                                        modifier = Modifier.testTag("doctor_send_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send to doctor",
                                            tint = CrispWhite
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MutedSlate)
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            color = DeepInk
        )
    }
}

@Composable
private fun DoctorChatBubble(
    message: DoctorMessage,
    modifier: Modifier = Modifier
) {
    val isDoctor = SenderRole.fromWire(message.senderRole) == SenderRole.DOCTOR
    Row(
        modifier = modifier,
        horizontalArrangement = if (isDoctor) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isDoctor) 4.dp else 18.dp,
                bottomEnd = if (isDoctor) 18.dp else 4.dp
            ),
            color = if (isDoctor) CrispWhite else SoftSageBg,
            border = BorderStroke(1.dp, if (isDoctor) BorderBeige else DeepSage.copy(alpha = 0.25f)),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepSage
                    )
                    Text(
                        text = message.timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate
                    )
                }

                if (!message.attachedGraphSummary.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = WarmCream
                    ) {
                        Text(
                            text = "📊 ${message.attachedGraphSummary}",
                            style = MaterialTheme.typography.labelMedium,
                            color = DeepInk,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                if (message.attachedEstimateUnits != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = WarmCream
                    ) {
                        Text(
                            text = "💉 Insulin Estimate: ${message.attachedEstimateUnits} units",
                            style = MaterialTheme.typography.labelMedium,
                            color = DeepInk,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepInk
                )

                if (!message.confirmedBadge.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SoftSageBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = DeepSage,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "✓ ${message.confirmedBadge}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepSage
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpjClinicianTriageView(
    profile: PatientProfile,
    cgm: CgmSnapshotSummary,
    featureFlags: FeatureFlags,
    onConfirmTriagePlan: (Double) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 10-Second Executive Summary
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CrispWhite),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, BorderBeige),
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "KPJ 10-Second Executive Clinical Brief",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepSage
                    )
                    cgm.clinicianBrief.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepInk
                        )
                    }
                }
            }
        }

        // Quantitative SHAP Attribution Chart
        if (featureFlags.shapDualSummary) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CrispWhite),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, BorderBeige),
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Quantitative SHAP Feature Attribution (%)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepInk
                        )
                        cgm.shapDrivers.forEach { driver ->
                            ClinicianShapRow(driver)
                        }
                    }
                }
            }
        }

        // Organ Protection Status Badges
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CrispWhite),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, BorderBeige),
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Organ Protection Surveillance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepInk
                    )

                    OrganBadgeRow(
                        icon = Icons.Default.Psychology,
                        title = "Neuro-Metabolic",
                        status = "Stability ${profile.neuroScore}/100 · Monitor HRV"
                    )
                    OrganBadgeRow(
                        icon = Icons.Default.WaterDrop,
                        title = "Kidney (eGFR / uACR)",
                        status = "eGFR ${profile.renalEgfr} mL/min · uACR ${profile.renalUacr} mg/g (Protected)"
                    )
                    OrganBadgeRow(
                        icon = Icons.Default.RemoveRedEye,
                        title = "Retinopathy Baseline",
                        status = "Cleared · Annual KPJ Screening Active"
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { onConfirmTriagePlan(1.0) },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepSage),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("confirm_triage_order_button")
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm & Sync 1.0u KPJ Order", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClinicianShapRow(driver: ShapDriver) {
    val isRisk = driver.deltaMgDl >= 0
    val color = if (isRisk) Terracotta else DeepSage
    val sign = if (isRisk) "+" else "-"
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${driver.emoji} ${driver.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = DeepInk
            )
            Text(
                text = "$sign${driver.attributionPercent}%",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SoftStone)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((driver.attributionPercent / 60f).coerceIn(0.08f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun OrganBadgeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    status: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SoftSageBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = DeepSage, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.labelLarge, color = DeepInk, fontWeight = FontWeight.Bold)
                Text(text = status, style = MaterialTheme.typography.bodyMedium, color = MutedSlate)
            }
        }
    }
}
