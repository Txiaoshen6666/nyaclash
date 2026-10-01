package com.autumn.nyaclash.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Fallback palettes used when dynamic color (Material You) is unavailable.
private val NyaPrimaryLight = Color(0xFF3F5AE0)
private val NyaSecondaryLight = Color(0xFF5B5D72)
private val NyaTertiaryLight = Color(0xFF77536D)

private val NyaPrimaryDark = Color(0xFFB8C3FF)
private val NyaSecondaryDark = Color(0xFFC4C5DD)
private val NyaTertiaryDark = Color(0xFFE6B9D9)

val LightColors = lightColorScheme(
    primary = NyaPrimaryLight,
    secondary = NyaSecondaryLight,
    tertiary = NyaTertiaryLight,
)

val DarkColors = darkColorScheme(
    primary = NyaPrimaryDark,
    secondary = NyaSecondaryDark,
    tertiary = NyaTertiaryDark,
)
