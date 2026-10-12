package com.saaf.milkshop.nativeui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

/** Large chrome surfaces and data emphasis have different jobs from action accents. */
@Immutable
data class NativeVisualColors(
    val header: Color,
    val onHeader: Color,
    val headerSupporting: Color,
    val supporting: Color,
    val metric: Color,
)

internal val LightVisualColors = NativeVisualColors(
    header = SaafGreen,
    onHeader = SaafSurface,
    headerSupporting = SaafSurface.copy(alpha = .82f),
    supporting = SaafMuted,
    metric = Color(0xFF62490E),
)

internal val DarkVisualColors = NativeVisualColors(
    header = Color(0xFF20372D),
    onHeader = Color(0xFFF0F3EF),
    headerSupporting = Color(0xFFC5D2CB),
    supporting = Color(0xFFB3C2B8),
    metric = Color(0xFFE9D6A7),
)

internal val LocalNativeVisualColors = staticCompositionLocalOf { LightVisualColors }

val nativeVisualColors: NativeVisualColors
    @Composable get() = LocalNativeVisualColors.current

/** Short metadata and labels stay subordinate through color and scale, not thin strokes. */
@Composable
fun nativeSupportingTextStyle() = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)

@Composable
fun nativeSectionTextStyle() = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)

@Composable
fun nativeGroupLabelTextStyle() = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)

@Composable
fun nativeNavigationTitleTextStyle() = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)

@Composable
fun nativeValueTextStyle() = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)

@Composable
fun nativeMetricTextStyle() = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
