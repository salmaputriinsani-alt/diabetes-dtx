package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AurelixLightColorScheme = lightColorScheme(
    primary = DeepSage,
    onPrimary = CrispWhite,
    primaryContainer = SoftSageBg,
    onPrimaryContainer = SageDark,
    secondary = Terracotta,
    onSecondary = CrispWhite,
    secondaryContainer = TerracottaSoft,
    onSecondaryContainer = TerracottaDark,
    tertiary = WarmAmber,
    onTertiary = CrispWhite,
    tertiaryContainer = WarmAmberBg,
    onTertiaryContainer = ColorTokens.AmberInk,
    background = WarmCream,
    onBackground = DeepInk,
    surface = CrispWhite,
    onSurface = DeepInk,
    surfaceVariant = SoftStone,
    onSurfaceVariant = MutedSlate,
    outline = BorderBeige,
    error = DangerCoral,
    onError = CrispWhite,
    errorContainer = DangerCoralBg,
    onErrorContainer = DangerCoral
)

private object ColorTokens {
    val AmberInk = androidx.compose.ui.graphics.Color(0xFF92400E)
}

val AurelixShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun AurelixTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AurelixLightColorScheme,
        typography = AurelixTypography,
        shapes = AurelixShapes,
        content = content
    )
}
