package com.example.modules.log

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.LocalizedCopy
import com.example.data.model.LogEntryType
import com.example.data.model.MetabolicLog
import com.example.data.model.UiState
import com.example.ui.theme.BorderBeige
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.DeepInk
import com.example.ui.theme.DeepSage
import com.example.ui.theme.InsulinBlue
import com.example.ui.theme.InsulinBlueBg
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.SoftSageBg
import com.example.ui.theme.SoftStone
import com.example.ui.theme.Terracotta
import com.example.ui.theme.TerracottaSoft
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberBg
import com.example.ui.theme.WarmAmberBorder
import com.example.ui.theme.WarmCream

private enum class LogFilter { ALL, MEAL, INSULIN, ALERT }

@Composable
fun LogScreen(
    logsState: UiState<List<MetabolicLog>>,
    copy: LocalizedCopy
) {
    var activeFilter by remember { mutableStateOf(LogFilter.ALL) }
    var selectedDetailLog by remember { mutableStateOf<MetabolicLog?>(null) }

    if (selectedDetailLog != null) {
        BackHandler { selectedDetailLog = null }
        LogDetailDialog(
            log = selectedDetailLog!!,
            onDismiss = { selectedDetailLog = null }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            Row(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterPill(
                    label = copy.logFilterAll,
                    selected = activeFilter == LogFilter.ALL,
                    tag = "filter_all",
                    onClick = { activeFilter = LogFilter.ALL }
                )
                FilterPill(
                    label = copy.logFilterMeal,
                    selected = activeFilter == LogFilter.MEAL,
                    tag = "filter_meal",
                    onClick = { activeFilter = LogFilter.MEAL }
                )
                FilterPill(
                    label = copy.logFilterInsulin,
                    selected = activeFilter == LogFilter.INSULIN,
                    tag = "filter_insulin",
                    onClick = { activeFilter = LogFilter.INSULIN }
                )
                FilterPill(
                    label = copy.logFilterAlert,
                    selected = activeFilter == LogFilter.ALERT,
                    tag = "filter_alert",
                    onClick = { activeFilter = LogFilter.ALERT }
                )
            }
        }

        when (logsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = DeepSage)
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = WarmAmberBg),
                        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                    ) {
                        Text(
                            text = logsState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepInk,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                val filtered = logsState.data.filter { log ->
                    val type = LogEntryType.fromWire(log.entryType)
                    when (activeFilter) {
                        LogFilter.ALL -> true
                        LogFilter.MEAL -> type == LogEntryType.MEAL
                        LogFilter.INSULIN -> type == LogEntryType.INSULIN
                        LogFilter.ALERT -> type == LogEntryType.ALERT || type == LogEntryType.PREDICTION
                    }
                }
                val grouped = filtered.groupBy { it.dateGroup }

                if (grouped.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CrispWhite),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, BorderBeige),
                            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                        ) {
                            Text(
                                text = "No entries match this filter yet. Try scanning a meal with the Makan button!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSlate,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                } else {
                    grouped.forEach { (dateGroup, itemsList) ->
                        item(key = "header_$dateGroup") {
                            Text(
                                text = dateGroup,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MutedSlate,
                                modifier = Modifier
                                    .widthIn(max = 600.dp)
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                            )
                        }
                        items(itemsList, key = { it.id.ifBlank { "${it.title}_${it.timeLabel}" } }) { log ->
                            TimelineLogCard(
                                log = log,
                                detailLabel = copy.detailBtn,
                                onOpenDetail = { selectedDetailLog = log },
                                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) DeepSage else CrispWhite,
        border = BorderStroke(1.dp, if (selected) DeepSage else BorderBeige),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) CrispWhite else MutedSlate,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun TimelineLogCard(
    log: MetabolicLog,
    detailLabel: String,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val type = LogEntryType.fromWire(log.entryType)
    val (bgColor, borderColor, iconBg, iconTint, icon) = when (type) {
        LogEntryType.ALERT -> LogVisualStyle(
            WarmAmberBg, WarmAmberBorder, CrispWhite, WarmAmber, Icons.Default.NotificationsActive
        )
        LogEntryType.PREDICTION -> LogVisualStyle(
            WarmAmberBg.copy(alpha = 0.65f), WarmAmberBorder, CrispWhite, WarmAmber, Icons.AutoMirrored.Filled.TrendingUp
        )
        LogEntryType.INSULIN -> LogVisualStyle(
            CrispWhite, BorderBeige, InsulinBlueBg, InsulinBlue, Icons.Default.Vaccines
        )
        LogEntryType.MEAL -> LogVisualStyle(
            CrispWhite, BorderBeige, TerracottaSoft, Terracotta, Icons.Default.Restaurant
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconBg,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = log.title,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = log.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepInk
                    )
                    Text(
                        text = "${log.timeLabel} · ${log.glucoseMgDl} mg/dL",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFamily,
                        color = MutedSlate
                    )
                    if (log.shapImpactSummary.isNotBlank()) {
                        Text(
                            text = log.shapImpactSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepInk.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (type == LogEntryType.ALERT || type == LogEntryType.PREDICTION) CrispWhite else SoftSageBg,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenDetail)
                    .testTag("log_detail_${log.id}")
            ) {
                Text(
                    text = detailLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepSage,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun LogDetailDialog(
    log: MetabolicLog,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CrispWhite,
        title = {
            Text(
                text = log.title,
                style = MaterialTheme.typography.titleLarge,
                color = DeepInk
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${log.dateGroup} · ${log.timeLabel} · Glucose: ${log.glucoseMgDl} mg/dL",
                    style = MaterialTheme.typography.labelLarge,
                    color = DeepSage
                )
                if (log.carbsGrams > 0 || log.proteinGrams > 0) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = WarmCream,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MacroChip("Carbs", "${log.carbsGrams}g")
                            MacroChip("Protein", "${log.proteinGrams}g")
                            MacroChip("Fat", "${log.fatGrams}g")
                            MacroChip("Fiber", "${log.fiberGrams}g")
                        }
                    }
                }
                if (log.insulinUnits > 0.0) {
                    Text(
                        text = "Insulin Dose: ${log.insulinUnits} units",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = InsulinBlue
                    )
                }
                Text(
                    text = "SHAP Attribution: ${log.shapImpactSummary}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepInk
                )
                if (!log.habitTip.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SoftSageBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tip: ${log.habitTip}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepSage,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DeepSage)
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun MacroChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepInk)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MutedSlate)
    }
}

private data class LogVisualStyle(
    val bgColor: androidx.compose.ui.graphics.Color,
    val borderColor: androidx.compose.ui.graphics.Color,
    val iconBg: androidx.compose.ui.graphics.Color,
    val iconTint: androidx.compose.ui.graphics.Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
