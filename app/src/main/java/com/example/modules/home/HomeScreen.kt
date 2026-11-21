package com.example.modules.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.config.FeatureFlags
import com.example.core.LocalizedCopy
import com.example.data.model.CgmSnapshotSummary
import com.example.data.model.LogEntryType
import com.example.data.model.PatientProfile
import com.example.data.model.ShapDriver
import com.example.data.model.TimeWindow
import com.example.ui.theme.BorderBeige
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.DeepInk
import com.example.ui.theme.DeepSage
import com.example.ui.theme.InsulinBlue
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.SoftSageBg
import com.example.ui.theme.SoftStone
import com.example.ui.theme.Terracotta
import com.example.ui.theme.TerracottaSoft
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberBg
import com.example.ui.theme.WarmCream
import java.util.Locale
import kotlin.math.abs

@Composable
fun HomeScreen(
    profile: PatientProfile,
    cgm: CgmSnapshotSummary,
    selectedWindow: TimeWindow,
    featureFlags: FeatureFlags,
    copy: LocalizedCopy,
    onSelectWindow: (TimeWindow) -> Unit,
    onUpdateSimulator: (sleepHours: Double, carbs: Int, walkMins: Int, insulinUnits: Double) -> Unit
) {
    var simulatorExpanded by remember { mutableStateOf(false) }

    // Local slider state synced to Firestore profile
    var sleepVal by remember(profile.sleepHours) { mutableDoubleStateOf(profile.sleepHours) }
    var carbsVal by remember(profile.simMealCarbs) { mutableIntStateOf(profile.simMealCarbs.toInt()) }
    var walkVal by remember(profile.walkMinutes) { mutableIntStateOf(profile.walkMinutes.toInt()) }
    var insulinVal by remember(profile.simInsulinUnits) { mutableDoubleStateOf(profile.simInsulinUnits) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 1. Greeting & Live CGM Hero Card
        item {
            Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                Text(
                    text = "${copy.greetingPrefix}, ${profile.patientName}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DeepInk
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = CrispWhite),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, BorderBeige),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(SoftSageBg.copy(alpha = 0.55f), CrispWhite)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (cgm.inTarget) SoftSageBg else WarmAmberBg
                            ) {
                                Text(
                                    text = if (cgm.inTarget) copy.inTarget else copy.aboveTarget,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (cgm.inTarget) DeepSage else WarmAmber,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = "CGM Sensor",
                                    tint = DeepSage,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sensor ${profile.sensorDaysLeft}d · ${profile.sensorBattery}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MutedSlate
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.BatteryChargingFull,
                                    contentDescription = null,
                                    tint = DeepSage,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "${cgm.currentMgDl}",
                                style = MaterialTheme.typography.displayLarge,
                                color = DeepInk
                            )
                            Column(modifier = Modifier.padding(bottom = 6.dp)) {
                                Text(
                                    text = "mg/dL · ${String.format(Locale.US, "%.1f", cgm.currentMmolL)} mmol/L",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedSlate,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (cgm.trendUp) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingFlat,
                                        contentDescription = null,
                                        tint = if (cgm.trendUp) WarmAmber else DeepSage,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (cgm.trendUp) copy.risingArrow else copy.stableArrow,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (cgm.trendUp) WarmAmber else DeepSage
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. 10-Second Smart Summary + Patient SHAP Card
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = SoftSageBg,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = DeepSage,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = copy.tenSecSummaryTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepInk
                            )
                        }

                        cgm.tenSecondBullets.forEach { bullet ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 7.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(DeepSage)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = bullet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DeepInk
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = WarmCream,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = copy.whySugarMoved,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DeepSage,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = cgm.patientShapSentence,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedSlate
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                cgm.shapDrivers.forEach { driver ->
                                    ShapImpactBarRow(driver = driver)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Interactive Glucose Forecast Graph (3h | 6h | 12h | 24h) + Simulator Controls
        if (featureFlags.cgmForecastEngine) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CrispWhite),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, BorderBeige),
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = copy.forecastTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepInk
                                )
                                Text(
                                    text = "${copy.prediction60m}: ${cgm.predicted60MinMgDl} mg/dL",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (cgm.predicted60MinMgDl > 180) WarmAmber else DeepSage,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SoftStone
                            ) {
                                Row(modifier = Modifier.padding(3.dp)) {
                                    TimeWindow.entries.forEach { w ->
                                        val selected = w == selectedWindow
                                        Surface(
                                            shape = RoundedCornerShape(11.dp),
                                            color = if (selected) CrispWhite else Color.Transparent,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(11.dp))
                                                .clickable { onSelectWindow(w) }
                                                .testTag("window_chip_${w.label}")
                                        ) {
                                            Text(
                                                text = w.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) DeepInk else MutedSlate,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        CgmForecastCanvas(
                            cgm = cgm,
                            targetLow = profile.targetLowMgDl.toFloat(),
                            targetHigh = profile.targetHighMgDl.toFloat()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LegendDot(color = Terracotta, label = "Meal")
                            LegendDot(color = InsulinBlue, label = "Insulin")
                            LegendDot(color = WarmAmber, label = "Alert")
                            LegendDot(color = DeepSage.copy(alpha = 0.45f), label = copy.prediction60m)
                        }

                        // Collapsible Demo Simulator Controls
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = WarmCream,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { simulatorExpanded = !simulatorExpanded }
                                .testTag("simulator_toggle")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = Terracotta,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = copy.simulatorControls,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = DeepInk
                                    )
                                }
                                Icon(
                                    imageVector = if (simulatorExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MutedSlate
                                )
                            }
                        }

                        AnimatedVisibility(visible = simulatorExpanded) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SimulatorSlider(
                                    label = "Sleep Duration",
                                    valueText = "${String.format(Locale.US, "%.1f", sleepVal)} hrs",
                                    value = sleepVal.toFloat(),
                                    range = 3f..9f,
                                    onValueChange = { sleepVal = it.toDouble() },
                                    onValueChangeFinished = {
                                        onUpdateSimulator(sleepVal, carbsVal, walkVal, insulinVal)
                                    }
                                )
                                SimulatorSlider(
                                    label = "Meal Carbs",
                                    valueText = "$carbsVal g",
                                    value = carbsVal.toFloat(),
                                    range = 10f..110f,
                                    onValueChange = { carbsVal = it.toInt() },
                                    onValueChangeFinished = {
                                        onUpdateSimulator(sleepVal, carbsVal, walkVal, insulinVal)
                                    }
                                )
                                SimulatorSlider(
                                    label = "Post-Meal Walk",
                                    valueText = "$walkVal min",
                                    value = walkVal.toFloat(),
                                    range = 0f..45f,
                                    onValueChange = { walkVal = it.toInt() },
                                    onValueChangeFinished = {
                                        onUpdateSimulator(sleepVal, carbsVal, walkVal, insulinVal)
                                    }
                                )
                                SimulatorSlider(
                                    label = "Insulin / Meds",
                                    valueText = "${String.format(Locale.US, "%.1f", insulinVal)} u",
                                    value = insulinVal.toFloat(),
                                    range = 0f..6f,
                                    onValueChange = { insulinVal = ((it * 2).toInt() / 2.0) },
                                    onValueChangeFinished = {
                                        onUpdateSimulator(sleepVal, carbsVal, walkVal, insulinVal)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShapImpactBarRow(driver: ShapDriver) {
    val isPositiveRisk = driver.deltaMgDl >= 0
    val barFraction = (abs(driver.deltaMgDl) / 55f).coerceIn(0.08f, 1f)
    val barColor = if (isPositiveRisk) Terracotta else DeepSage
    val signedText = if (isPositiveRisk) "+${driver.deltaMgDl} mg/dL" else "${driver.deltaMgDl} mg/dL"

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${driver.emoji} ${driver.label}",
                style = MaterialTheme.typography.labelMedium,
                color = DeepInk
            )
            Text(
                text = signedText,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SoftStone)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(barFraction)
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun CgmForecastCanvas(
    cgm: CgmSnapshotSummary,
    targetLow: Float,
    targetHigh: Float
) {
    val points = cgm.points
    if (points.size < 2) return

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(WarmCream.copy(alpha = 0.6f))
        ) {
            val minVal = 55f
            val maxVal = 235f
            val padH = 14.dp.toPx()
            val padV = 16.dp.toPx()
            val w = size.width - padH * 2
            val h = size.height - padV * 2

            fun xAt(index: Int): Float = padH + (index.toFloat() / (points.size - 1)) * w
            fun yAt(v: Float): Float {
                val norm = ((v - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                return padV + h * (1f - norm)
            }

            // Target green band (70-180 mg/dL)
            val yHigh = yAt(targetHigh)
            val yLow = yAt(targetLow)
            drawRect(
                color = SoftSageBg.copy(alpha = 0.85f),
                topLeft = Offset(0f, yHigh),
                size = androidx.compose.ui.geometry.Size(size.width, yLow - yHigh)
            )

            // Reference lines
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            drawLine(
                color = DeepSage.copy(alpha = 0.35f),
                start = Offset(0f, yHigh),
                end = Offset(size.width, yHigh),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = dashEffect
            )
            drawLine(
                color = DeepSage.copy(alpha = 0.35f),
                start = Offset(0f, yLow),
                end = Offset(size.width, yLow),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = dashEffect
            )

            val firstForecastIdx = points.indexOfFirst { it.isForecast }.let { if (it <= 0) points.size else it }
            val anchorIdx = (firstForecastIdx - 1).coerceAtLeast(0)

            // 60-min AI prediction cone polygon
            if (firstForecastIdx < points.size) {
                val conePath = Path().apply {
                    moveTo(xAt(anchorIdx), yAt(points[anchorIdx].mgDl))
                    for (i in firstForecastIdx until points.size) {
                        lineTo(xAt(i), yAt(points[i].upperBound))
                    }
                    for (i in points.size - 1 downTo firstForecastIdx) {
                        lineTo(xAt(i), yAt(points[i].lowerBound))
                    }
                    close()
                }
                drawPath(
                    path = conePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(DeepSage.copy(alpha = 0.28f), Terracotta.copy(alpha = 0.16f)),
                        startX = xAt(anchorIdx),
                        endX = size.width
                    )
                )
            }

            // Historical line
            val historyPath = Path()
            for (i in 0..anchorIdx) {
                val px = xAt(i)
                val py = yAt(points[i].mgDl)
                if (i == 0) historyPath.moveTo(px, py) else historyPath.lineTo(px, py)
            }
            drawPath(
                path = historyPath,
                color = DeepSage,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Forecast dashed line
            if (firstForecastIdx < points.size) {
                val forecastPath = Path().apply {
                    moveTo(xAt(anchorIdx), yAt(points[anchorIdx].mgDl))
                    for (i in firstForecastIdx until points.size) {
                        lineTo(xAt(i), yAt(points[i].mgDl))
                    }
                }
                drawPath(
                    path = forecastPath,
                    color = Terracotta,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                )
            }

            // Event markers
            points.forEachIndexed { i, pt ->
                val cx = xAt(i)
                val cy = yAt(pt.mgDl)
                when (pt.markerType) {
                    LogEntryType.MEAL -> {
                        drawCircle(color = CrispWhite, radius = 6.dp.toPx(), center = Offset(cx, cy))
                        drawCircle(color = Terracotta, radius = 4.5.dp.toPx(), center = Offset(cx, cy))
                    }
                    LogEntryType.INSULIN -> {
                        drawCircle(color = CrispWhite, radius = 6.dp.toPx(), center = Offset(cx, cy))
                        drawCircle(color = InsulinBlue, radius = 4.5.dp.toPx(), center = Offset(cx, cy))
                    }
                    LogEntryType.ALERT, LogEntryType.PREDICTION -> {
                        drawCircle(color = CrispWhite, radius = 6.dp.toPx(), center = Offset(cx, cy))
                        drawCircle(color = WarmAmber, radius = 4.5.dp.toPx(), center = Offset(cx, cy))
                    }
                    null -> if (i == anchorIdx || i == points.lastIndex) {
                        drawCircle(
                            color = if (pt.isForecast) Terracotta else DeepSage,
                            radius = 4.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val labelIndices = listOf(0, points.size / 2, points.lastIndex).distinct()
            labelIndices.forEach { idx ->
                Text(
                    text = points[idx].label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (points[idx].isForecast) Terracotta else MutedSlate
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MutedSlate
        )
    }
}

@Composable
private fun SimulatorSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = DeepInk)
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = DeepSage
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = DeepSage,
                activeTrackColor = DeepSage,
                inactiveTrackColor = BorderBeige
            )
        )
    }
}
