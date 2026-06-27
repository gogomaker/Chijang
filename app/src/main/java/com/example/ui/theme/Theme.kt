package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme =
  lightColorScheme(
    primary = TacticalGreen,
    onPrimary = OnTacticalGreen,
    primaryContainer = TacticalGreenContainer,
    onPrimaryContainer = OnTacticalGreenContainer,
    secondary = MutedSage,
    onSecondary = OnMutedSage,
    secondaryContainer = MutedSageContainer,
    onSecondaryContainer = OnMutedSageContainer,
    tertiary = SlateGrey,
    onTertiary = OnSlateGrey,
    tertiaryContainer = SlateGreyContainer,
    onTertiaryContainer = OnSlateGreyContainer,
    background = OffWhiteGreen,
    onBackground = OnBackgroundDark,
    surface = PureWhite,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantGreen,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = TacticalOutline
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Enforce light mode
  dynamicColor: Boolean = false, // Disable dynamic color to preserve custom primary colors
  content: @Composable () -> Unit,
) {
  // Enforce LightColorScheme only as the app is optimized for Light Mode to maximize readability
  val colorScheme = LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
