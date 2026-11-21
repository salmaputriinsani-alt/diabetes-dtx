package com.example.config

/**
 * Modular feature toggles for AURELIX AI.
 * Any module can be independently enabled or disabled for demos or clinical pilots.
 */
data class FeatureFlags(
    val foodScanner: Boolean = true,
    val cgmForecastEngine: Boolean = true,
    val shapDualSummary: Boolean = true,
    val doctorTeleCare: Boolean = true,
    val neuroKidneyScreening: Boolean = true,
    val aiChatbot: Boolean = true,
    val ecosystemBusinessModel: Boolean = true
)

val DEFAULT_FEATURE_FLAGS = FeatureFlags()
