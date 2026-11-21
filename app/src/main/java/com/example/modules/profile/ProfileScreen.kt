package com.example.modules.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.config.FeatureFlags
import com.example.core.LocalizedCopy
import com.example.data.model.DiagnosisTrack
import com.example.data.model.OrganScreening
import com.example.data.model.PatientProfile
import com.example.data.model.UiState
import com.example.ui.theme.BorderBeige
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.DeepInk
import com.example.ui.theme.DeepSage
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.SoftSageBg
import com.example.ui.theme.Terracotta
import com.example.ui.theme.TerracottaSoft
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberBg
import com.example.ui.theme.WarmCream

@Composable
fun ProfileScreen(
    profile: PatientProfile,
    screeningState: UiState<OrganScreening>,
    stakeholderMode: Boolean,
    featureFlags: FeatureFlags,
    copy: LocalizedCopy,
    onSwitchTrack: (DiagnosisTrack) -> Unit,
    onSaveScreening: (
        footTingling: Boolean,
        hrvStressHigh: Boolean,
        postSpikeBrainFog: Boolean,
        systolicBp: Int,
        diastolicBp: Int,
        hydrationGlasses: Int
    ) -> Unit
) {
    val latestScreening = (screeningState as? UiState.Success)?.data ?: OrganScreening()

    var footTingling by remember(latestScreening.id) { mutableStateOf(latestScreening.footTingling) }
    var hrvStressHigh by remember(latestScreening.id) { mutableStateOf(latestScreening.hrvStressHigh) }
    var brainFog by remember(latestScreening.id) { mutableStateOf(latestScreening.postSpikeBrainFog) }
    var systolicBp by remember(latestScreening.id) { mutableIntStateOf(latestScreening.systolicBp.toInt()) }
    var hydration by remember(latestScreening.id) { mutableIntStateOf(latestScreening.hydrationGlasses.toInt()) }

    val isT2D = DiagnosisTrack.fromWire(profile.diagnosisTrack) == DiagnosisTrack.EARLY_T2D_REVERSAL

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 1. Patient & CGM Device Card
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
                                text = "${profile.patientName}, ${profile.age}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = DeepInk
                            )
                            Text(
                                text = if (isT2D) "Early Type 2 Diabetes — Reversal Track" else "Type 1 / Insulin-Assisted Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DeepSage,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SoftSageBg
                        ) {
                            Text(
                                text = "${profile.targetLowMgDl}–${profile.targetHighMgDl} mg/dL",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                color = DeepSage,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = WarmCream,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Sensors, contentDescription = null, tint = DeepSage, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("CGM Connected", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = DeepInk)
                                    Text("${profile.sensorBattery}% · ${profile.sensorDaysLeft} days left", style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = WarmCream,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Medication, contentDescription = null, tint = Terracotta, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Regimen", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = DeepInk)
                                    Text(if (isT2D) "Metformin + 1u PRN" else "Basal + Bolus", style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onSwitchTrack(
                                if (isT2D) DiagnosisTrack.T1_INSULIN_ASSISTED else DiagnosisTrack.EARLY_T2D_REVERSAL
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftSageBg,
                            contentColor = DeepSage
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("switch_track_button")
                    ) {
                        Text(
                            text = if (isT2D) "Preview Type 1 / Insulin-Assisted Mode" else "Return to Early T2D Reversal Track",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // 2. Early Neuro-Metabolic & Kidney Health Screening Card
        if (featureFlags.neuroKidneyScreening) {
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
                        Text(
                            text = copy.organScreeningTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepInk
                        )

                        // Neuroscience & Brain-Metabolic Check
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = WarmCream,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Psychology, contentDescription = null, tint = DeepSage, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(copy.neuroTitle, style = MaterialTheme.typography.labelLarge, color = DeepInk)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = SoftSageBg
                                    ) {
                                        Text(
                                            text = "${profile.neuroScore}/100",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepSage,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                NeuroToggleRow("Foot tingling or numbness", footTingling) { footTingling = it }
                                NeuroToggleRow("Elevated HRV autonomic stress", hrvStressHigh) { hrvStressHigh = it }
                                NeuroToggleRow("Post-spike brain fog", brainFog) { brainFog = it }
                            }
                        }

                        // Kidney Health (Renal) Check
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = WarmCream,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = DeepSage, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(copy.kidneyTitle, style = MaterialTheme.typography.labelLarge, color = DeepInk)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (systolicBp >= 135) WarmAmberBg else SoftSageBg
                                    ) {
                                        Text(
                                            text = "eGFR ${profile.renalEgfr} · uACR ${profile.renalUacr}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (systolicBp >= 135) WarmAmber else DeepSage,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Systolic BP: $systolicBp / 78 mmHg",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DeepInk
                                )
                                Slider(
                                    value = systolicBp.toFloat(),
                                    onValueChange = { systolicBp = it.toInt() },
                                    valueRange = 100f..165f,
                                    colors = SliderDefaults.colors(thumbColor = DeepSage, activeTrackColor = DeepSage)
                                )

                                Text(
                                    text = "Daily Hydration: $hydration glasses",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DeepInk
                                )
                                Slider(
                                    value = hydration.toFloat(),
                                    onValueChange = { hydration = it.toInt() },
                                    valueRange = 2f..12f,
                                    colors = SliderDefaults.colors(thumbColor = DeepSage, activeTrackColor = DeepSage)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onSaveScreening(
                                    footTingling,
                                    hrvStressHigh,
                                    brainFog,
                                    systolicBp,
                                    78,
                                    hydration
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepSage),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("save_organ_screening_button")
                        ) {
                            Text("Save Organ Protection Check-In", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // 3. Ecosystem Ideology & Win-Win B40 Impact Card (HOTE + Value Co-Creation)
        if (featureFlags.ecosystemBusinessModel) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (stakeholderMode) TerracottaSoft else CrispWhite
                    ),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, if (stakeholderMode) Terracotta else BorderBeige),
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Public, contentDescription = null, tint = Terracotta, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = copy.ecosystemTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepInk
                            )
                        }

                        Text(
                            text = "Every 5 B2B KPJ Hospital & Corporate Insurer DTx subscriptions automatically cross-subsidize 1 free AURELIX AI license + CGM monitoring kit for an underprivileged B40 patient in Malaysia.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepInk
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HotePillarChip("H · Human", "10-sec SHAP", Modifier.weight(1f))
                            HotePillarChip("O · Org", "KPJ Triage", Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HotePillarChip("T · Tech", "60m Twin + AI", Modifier.weight(1f))
                            HotePillarChip("E · Env", "5:1 B40 Subsidy", Modifier.weight(1f))
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SoftSageBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = DeepSage, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pilot Impact: 1,250 KPJ Members → 250 B40 Kits Funded",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DeepSage,
                                    fontWeight = FontWeight.Bold
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
private fun NeuroToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = DeepInk,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CrispWhite,
                checkedTrackColor = Terracotta,
                uncheckedThumbColor = DeepSage,
                uncheckedTrackColor = SoftSageBg
            )
        )
    }
}

@Composable
private fun HotePillarChip(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = WarmCream,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = DeepSage)
            Text(text = subtitle, style = MaterialTheme.typography.labelMedium, color = MutedSlate)
        }
    }
}
