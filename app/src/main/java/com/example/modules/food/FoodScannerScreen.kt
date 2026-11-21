package com.example.modules.food

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.LocalizedCopy
import com.example.data.model.MealAnalysisResult
import com.example.data.model.UiState
import com.example.ui.theme.BorderBeige
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.DeepInk
import com.example.ui.theme.DeepSage
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.SoftSageBg
import com.example.ui.theme.Terracotta
import com.example.ui.theme.TerracottaSoft
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberBg
import com.example.ui.theme.WarmCream

private val MALAYSIAN_PRESETS = listOf(
    "Nasi Lemak with Sambal & Egg",
    "Roti Canai & Teh Tarik",
    "Char Kway Teow",
    "Steamed Fish & Brown Rice"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodScannerScreen(
    analysisState: UiState<MealAnalysisResult>?,
    copy: LocalizedCopy,
    onAnalyzeMeal: (Bitmap?, String?) -> Unit,
    onLogMeal: (MealAnalysisResult) -> Unit,
    onClearResult: () -> Unit
) {
    val context = LocalContext.current
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bmp = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(context.contentResolver, uri)
                    ) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (_: Exception) {
                null
            }
            if (bmp != null) {
                selectedBitmap = bmp
                onAnalyzeMeal(bmp, null)
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CrispWhite),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderBeige),
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = copy.scanMealTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DeepInk
                    )
                    Text(
                        text = copy.scanMealSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSlate
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(18.dp))
                    ) {
                        if (selectedBitmap != null) {
                            Image(
                                bitmap = selectedBitmap!!.asImageBitmap(),
                                contentDescription = "Selected Meal Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(170.dp)
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_nasi_lemak),
                                contentDescription = "Malaysian Nasi Lemak Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(170.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Terracotta,
                            contentColor = CrispWhite
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pick_meal_photo_button")
                    ) {
                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(copy.uploadPhotoBtn, style = MaterialTheme.typography.labelLarge)
                    }

                    Text(
                        text = copy.presetsLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = DeepInk
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MALAYSIAN_PRESETS.forEachIndexed { index, dish ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = WarmCream,
                                border = BorderStroke(1.dp, BorderBeige),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        selectedBitmap = null
                                        onAnalyzeMeal(null, dish)
                                    }
                                    .testTag("preset_dish_$index")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Terracotta,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = dish,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = DeepInk,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Gemini Vision Result Card
        item {
            when (analysisState) {
                is UiState.Loading -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CrispWhite),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, BorderBeige),
                        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(color = Terracotta, modifier = Modifier.size(28.dp))
                            Text(
                                text = "Gemini Vision is analyzing carbs, glycemic load & 60-min curve...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DeepInk
                            )
                        }
                    }
                }
                is UiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = WarmAmberBg),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Analysis Error",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepInk
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = analysisState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = DeepInk
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    val res = analysisState.data
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CrispWhite),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.5.dp, DeepSage.copy(alpha = 0.4f)),
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
                                Text(
                                    text = res.mealName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepInk,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (res.glycemicLoad == "High") WarmAmberBg else SoftSageBg
                                ) {
                                    Text(
                                        text = "GL: ${res.glycemicLoad}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (res.glycemicLoad == "High") WarmAmber else DeepSage,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MacroMetric("Carbs", "${res.carbsGrams}g", Terracotta)
                                MacroMetric("Protein", "${res.proteinGrams}g", DeepSage)
                                MacroMetric("Fat", "${res.fatGrams}g", MutedSlate)
                                MacroMetric("Fiber", "${res.fiberGrams}g", DeepSage)
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = TerracottaSoft,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Predicted 60-min CGM rise: +${res.predictedRiseMgDl} mg/dL",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Terracotta,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SoftSageBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Malaysian Swap Tip: ${res.malaysianHabitTip}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DeepInk,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }

                            Button(
                                onClick = { onLogMeal(res) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeepSage,
                                    contentColor = CrispWhite
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("log_analyzed_meal_button")
                            ) {
                                Icon(imageVector = Icons.Default.AddCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(copy.logMealBtn, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                null -> Unit
            }
        }
    }
}

@Composable
private fun MacroMetric(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = WarmCream
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MutedSlate
            )
        }
    }
}
