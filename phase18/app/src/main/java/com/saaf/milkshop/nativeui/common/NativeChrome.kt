package com.saaf.milkshop.nativeui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.saaf.milkshop.nativeui.theme.nativeVisualColors

/** Slots depend only on the global text setting, never on the destination's wording. */
@Composable
fun NativeChromeBand(
    title: String,
    modifier: Modifier = Modifier,
    navigation: (@Composable () -> Unit)? = null,
    reserveNavigationSpace: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    reserveActionSpace: Boolean = true,
    actionWidth: Dp = 96.dp,
    context: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val titleStyle = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 28.sp)
    // Reserve actual font metrics from fixed samples, never the destination's content.
    // This includes font padding/rounding that adding raw sp line heights misses.
    fun lineHeight(style: TextStyle) = with(density) {
        measurer.measure("Ag", style = style, maxLines = 1, softWrap = false).size.height.toDp()
    }
    val metadataStyle = nativeChromeContextStyle(false)
    val identityStyle = nativeChromeContextStyle(true)
    val titleHeight = (lineHeight(titleStyle) + 16.dp).coerceAtLeast(56.dp)
    val contextHeight = (lineHeight(metadataStyle) * 2 + lineHeight(identityStyle) + 16.dp).coerceAtLeast(80.dp)
    Surface(modifier.fillMaxWidth(), color = nativeVisualColors.header,
        contentColor = nativeVisualColors.onHeader) {
        Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().height(titleHeight), verticalAlignment = Alignment.CenterVertically) {
                if (reserveNavigationSpace || navigation != null) {
                    Box(Modifier.width(48.dp), contentAlignment = Alignment.CenterStart) { navigation?.invoke() }
                }
                Text(title, modifier = Modifier.weight(1f), style = titleStyle,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.fillMaxWidth().height(contextHeight), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp), content = context)
                // Reserving the action slot also keeps context text still when Back/day status is absent.
                if (reserveActionSpace || trailing != null) {
                    Box(Modifier.width(actionWidth), contentAlignment = Alignment.CenterEnd) { trailing?.invoke() }
                }
            }
        }
    }
}

@Composable
fun NativeChromeContext(text: String, prominent: Boolean = false) {
    Text(text, style = nativeChromeContextStyle(prominent),
        color = if (prominent) nativeVisualColors.onHeader else nativeVisualColors.headerSupporting,
        maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun nativeChromeContextStyle(prominent: Boolean) = MaterialTheme.typography.bodySmall.copy(
    fontSize = if (prominent) 14.sp else 12.sp, lineHeight = if (prominent) 20.sp else 16.sp,
    fontWeight = if (prominent) FontWeight.SemiBold else FontWeight.Normal,
)

/** One bottom-bar size and safe-area owner for every signed-in role. */
@Composable
fun NativeBottomNavigation(content: @Composable RowScope.() -> Unit) {
    val height = with(LocalDensity.current) { 16.sp.toDp() * 2 + 40.dp }.coerceAtLeast(80.dp)
    NavigationBar(
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(height),
        windowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp,
        content = content,
    )
}

@Composable
fun NativeNavigationLabel(label: String) {
    val slot = with(LocalDensity.current) { 16.sp.toDp() * 2 }
    Box(Modifier.height(slot), contentAlignment = Alignment.Center) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 2,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}
