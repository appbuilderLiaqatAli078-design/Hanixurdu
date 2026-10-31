package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val HaniXVvipColorScheme = darkColorScheme(
    primary = VipChampagneGold,
    onPrimary = OledBlack,
    primaryContainer = VipGoldDark,
    onPrimaryContainer = VipGoldLight,
    secondary = CinemaCrimson,
    onSecondary = TextPrimaryIvory,
    secondaryContainer = CinemaCrimsonDark,
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = VipGoldLight,
    onTertiary = OledBlack,
    background = OledBlack,
    onBackground = TextPrimaryIvory,
    surface = CharcoalSurface,
    onSurface = TextPrimaryIvory,
    surfaceVariant = GlassCardSurface,
    onSurfaceVariant = TextSecondarySilver,
    surfaceContainerLowest = OledBlack,
    surfaceContainerLow = CharcoalBackground,
    surfaceContainer = CharcoalSurface,
    surfaceContainerHigh = GlassCardSurface,
    surfaceContainerHighest = GlassCardElevated,
    outline = GlassBorderSubtle,
    outlineVariant = Color(0xFF1F2433),
    error = ErrorCoral,
    onError = OledBlack
)

val HaniXShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // HaniX Urdu strictly uses its bespoke VVIP OLED Dark Cinema theme
    MaterialTheme(
        colorScheme = HaniXVvipColorScheme,
        typography = Typography,
        shapes = HaniXShapes,
        content = content
    )
}
