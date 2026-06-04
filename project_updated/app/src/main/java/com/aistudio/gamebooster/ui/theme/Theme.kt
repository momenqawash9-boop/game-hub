package com.aistudio.gamebooster.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = AccentColor,
    secondary = AccentColor,
    tertiary = AccentColor,
    background = PrimaryBackground,
    surface = CardBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary
  )

private val LightColorScheme = DarkColorScheme // Force gaming dark theme everywhere for premium gamer aesthetic

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark mode for booster feel
  dynamicColor: Boolean = false, // Disable dynamic colors to keep neon glow branding
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
