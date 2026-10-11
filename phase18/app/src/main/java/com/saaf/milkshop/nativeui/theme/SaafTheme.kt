package com.saaf.milkshop.nativeui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.saaf.milkshop.R

val SaafIvory = Color(0xFFECE7DA)
val SaafSurface = Color(0xFFFFFDF7)
val SaafGreen = Color(0xFF173F32)
val SaafYellow = Color(0xFFF2C85B)
val SaafInk = Color(0xFF17201B)
val SaafMuted = Color(0xFF515F55)
val SaafLine = Color(0xFF878171)
val SaafError = Color(0xFFB3261E)

val SaafFontFamily = FontFamily(
    Font(R.font.manrope_regular, weight = FontWeight.Normal),
    Font(R.font.manrope_medium, weight = FontWeight.Medium),
    Font(R.font.manrope_semibold, weight = FontWeight.SemiBold),
    Font(R.font.manrope_bold, weight = FontWeight.Bold),
    Font(R.font.manrope_extrabold, weight = FontWeight.ExtraBold),
)

private val LightColors = lightColorScheme(
    primary = SaafGreen,
    onPrimary = SaafSurface,
    primaryContainer = SaafGreen,
    onPrimaryContainer = SaafSurface,
    secondary = SaafYellow,
    onSecondary = SaafInk,
    secondaryContainer = Color(0xFFFFE49A),
    onSecondaryContainer = SaafInk,
    background = SaafIvory,
    onBackground = SaafInk,
    surface = SaafSurface,
    onSurface = SaafInk,
    surfaceVariant = Color(0xFFEBE7DC),
    onSurfaceVariant = SaafMuted,
    outline = SaafLine,
    outlineVariant = Color(0xFFC9C3B3),
    error = SaafError,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8D8C4),
    onPrimary = Color(0xFF092118),
    primaryContainer = Color(0xFF285A49),
    onPrimaryContainer = Color(0xFFE3F5ED),
    secondary = SaafYellow,
    onSecondary = SaafInk,
    secondaryContainer = Color(0xFF5A4815),
    onSecondaryContainer = Color(0xFFFFE7A7),
    background = Color(0xFF101713),
    onBackground = Color(0xFFF0F3EF),
    surface = Color(0xFF18211C),
    onSurface = Color(0xFFF0F3EF),
    surfaceVariant = Color(0xFF253029),
    onSurfaceVariant = Color(0xFFC5CEC8),
    outline = Color(0xFF7D8981),
    outlineVariant = Color(0xFF435248),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val SaafTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = SaafFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 38.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = SaafFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = SaafFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = SaafFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = SaafFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = SaafFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = SaafFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = SaafFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    // Remaining slots keep Material sizes but must render in Manrope —
    // leaving them default leaks Roboto next to the brand font on the same
    // screen (headlineSmall/titleSmall/bodySmall are used ~50× across UI).
    headlineLarge = TextStyle(
        fontFamily = SaafFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = SaafFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = SaafFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = SaafFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = SaafFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)

@Composable
fun SaafTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SaafTypography,
        content = content,
    )
}

/**
 * Shared edge-to-edge safe spacing for native screens.
 *
 * Green identity headers already pad their own title band under the status bar —
 * do not also wrap those screens in [nativeSafeTop]. Use these for full-screen
 * pickers / sheets without the green header.
 */
fun Modifier.nativeSafeTop(): Modifier = statusBarsPadding()

fun Modifier.nativeSafeBottom(): Modifier = navigationBarsPadding()

fun Modifier.nativeSafeDrawing(): Modifier = safeDrawingPadding()

val NativeScaffoldContentInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing
