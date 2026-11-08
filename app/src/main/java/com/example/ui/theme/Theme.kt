package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val TlBrainNeonColorScheme = darkColorScheme(
    primary = ElectricPurple,
    onPrimary = PureWhite,
    primaryContainer = DeepPurpleContainer,
    onPrimaryContainer = LuminousLavender,
    secondary = NeonLavenderGlow,
    onSecondary = DarkVioletInk,
    secondaryContainer = VioletContainer,
    onSecondaryContainer = SoftLavenderText,
    tertiary = NeonEmerald,
    onTertiary = DeepEmeraldInk,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = MintText,
    error = NeonCrimson,
    onError = PureWhite,
    errorContainer = CrimsonContainer,
    onErrorContainer = SoftRoseText,
    background = ObsidianBlack,
    onBackground = PureWhite,
    surface = CyberSurface,
    onSurface = PureWhite,
    surfaceVariant = CyberSurfaceBright,
    onSurfaceVariant = MutedVioletLabel,
    outline = GlowingPurpleBorder,
    outlineVariant = SubtleVioletDivider
)

val TlBrainShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun TlBrainTestTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TlBrainNeonColorScheme,
        typography = Typography,
        shapes = TlBrainShapes,
        content = content
    )
}
