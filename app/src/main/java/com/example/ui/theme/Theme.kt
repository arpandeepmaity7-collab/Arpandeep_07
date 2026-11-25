package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = InkPrimary,
    onPrimary = PaperCard,
    primaryContainer = PaperElevated,
    onPrimaryContainer = InkPrimary,
    secondary = Terracotta,
    onSecondary = PaperCard,
    secondaryContainer = TerracottaSoft,
    onSecondaryContainer = Terracotta,
    tertiary = Sage,
    onTertiary = PaperCard,
    tertiaryContainer = SageSoft,
    onTertiaryContainer = Sage,
    background = PaperBackground,
    onBackground = InkPrimary,
    surface = PaperCard,
    onSurface = InkPrimary,
    surfaceVariant = PaperSurface,
    onSurfaceVariant = InkSecondary,
    outline = InkMuted,
    outlineVariant = InkHairline,
    error = Terracotta,
    onError = PaperCard,
    errorContainer = TerracottaSoft,
    onErrorContainer = Terracotta
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkInkPrimary,
    onPrimary = DarkBackground,
    primaryContainer = DarkElevated,
    onPrimaryContainer = DarkInkPrimary,
    secondary = TerracottaDark,
    onSecondary = DarkBackground,
    secondaryContainer = TerracottaDarkSoft,
    onSecondaryContainer = TerracottaDark,
    tertiary = SageDark,
    onTertiary = DarkBackground,
    tertiaryContainer = SageDarkSoft,
    onTertiaryContainer = SageDark,
    background = DarkBackground,
    onBackground = DarkInkPrimary,
    surface = DarkCard,
    onSurface = DarkInkPrimary,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkInkSecondary,
    outline = DarkInkMuted,
    outlineVariant = DarkHairline,
    error = TerracottaDark,
    onError = DarkBackground,
    errorContainer = TerracottaDarkSoft,
    onErrorContainer = TerracottaDark
)

val FolioShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun FolioTasksTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = FolioShapes,
        content = content
    )
}
