package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ZegaNeonCyan,
    secondary = ZegaNeonViolet,
    tertiary = ZegaPurpleAccent,
    background = CosmicBackground,
    surface = CosmicCard,
    onPrimary = CosmicBackground,
    onSecondary = ZegaWhite,
    onTertiary = ZegaWhite,
    onBackground = ZegaWhite,
    onSurface = ZegaLightGrayText
)

private val LightColorScheme = DarkColorScheme // Keep consistent dark theme for the cosmic assistant look

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for the premium cosmic assistant atmosphere
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve our tailored branding
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
