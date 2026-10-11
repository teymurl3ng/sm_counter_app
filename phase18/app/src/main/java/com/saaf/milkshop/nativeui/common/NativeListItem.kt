package com.saaf.milkshop.nativeui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

/** Shared geometry; heights are minima so names and accessibility text can grow. */
enum class NativeListStyle(val minimumHeight: Int, val cornerRadius: Int) {
    Navigation(72, 12), Entity(72, 12), Selection(56, 12), Record(72, 12), Section(0, 16),
}

/** Surface outside click handling. Parents own the exterior gaps. */
@Composable
fun Modifier.nativeListItem(
    style: NativeListStyle = NativeListStyle.Record,
    selected: Boolean = false,
    enabled: Boolean = true,
): Modifier {
    val shape = RoundedCornerShape(style.cornerRadius.dp)
    val scheme = MaterialTheme.colorScheme
    val fill = if (selected) scheme.primary.copy(alpha = .08f).compositeOver(scheme.surface) else scheme.surface
    return fillMaxWidth().heightIn(min = style.minimumHeight.dp).clip(shape)
        .background(fill)
        .border(1.dp, if (selected) scheme.primary else scheme.outlineVariant, shape)
        .alpha(if (enabled) 1f else .6f)
}

/** Inside click handling, so the entire padded surface remains tappable. */
fun Modifier.nativeListItemContent(style: NativeListStyle = NativeListStyle.Record): Modifier =
    padding(horizontal = 16.dp, vertical = if (style == NativeListStyle.Section) 16.dp else 12.dp)

/** Default navigation feedback for shared submenu controls. */
@Composable
fun hapticAction(action: () -> Unit): () -> Unit {
    val context = LocalContext.current
    return { context.hapticTap(); action() }
}


@Composable
fun hapticCommitAction(action: () -> Unit): () -> Unit {
    val context = LocalContext.current
    return { context.hapticCommit(); action() }
}

@Composable
fun hapticDestructiveAction(action: () -> Unit): () -> Unit {
    val context = LocalContext.current
    return { context.hapticHeavy(); action() }
}

@Composable
fun hapticSelectionAction(action: () -> Unit): () -> Unit {
    val context = LocalContext.current
    return { context.hapticSelect(); action() }
}
