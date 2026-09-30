package com.saaf.milkshop.nativeui.common

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

private const val SECONDARY_TRANSITION_BASE_MILLIS = 180
private const val SECONDARY_TRANSITION_MAX_MILLIS = 1000

/** Shared native UX baseline: cold loads replace content; warm refreshes do not. */
@Composable
fun NativeColdLoadingPane(
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun NativeLoadFailurePane(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryTestTag: String? = null,
) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            modifier = retryTestTag?.let { Modifier.testTag(it) } ?: Modifier,
        ) { Text("Try again") }
    }
}

/**
 * Warm-refresh indicator drawn as a faded overlay at the top of a screen —
 * never composed in flow, so its appearance/disappearance cannot shift the
 * layout by even its 4dp height (the old in-column bar made whole screens
 * jitter on every re-validation). Place inside a Box, aligned TopCenter.
 */
@Composable
fun NativeRefreshOverlay(
    refreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = refreshing,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(150)),
        modifier = modifier,
    ) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

/**
 * Warm-refresh indicator drawn ON TOP of the modified layout: an indeterminate
 * 4dp sweep at the top edge with a fast fade. Never composed in flow, so it
 * cannot shift content by even 4dp (the old in-column bar made whole screens
 * jitter on every re-validation). Attach to a screen's root column, before
 * verticalScroll so the sweep stays pinned while content scrolls.
 */
@Composable
fun Modifier.nativeRefreshOverlay(refreshing: Boolean): Modifier {
    val alpha by animateFloatAsState(
        targetValue = if (refreshing) 1f else 0f,
        animationSpec = tween(150),
        label = "refresh_alpha",
    )
    val sweep = if (refreshing) {
        rememberInfiniteTransition(label = "refresh_sweep").animateFloat(
            initialValue = -0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing)),
            label = "refresh_shift",
        ).value
    } else {
        -0.4f
    }
    val bar = MaterialTheme.colorScheme.primary
    return drawWithContent {
        drawContent()
        if (alpha > 0.01f) {
            val h = 4.dp.toPx()
            val w = size.width
            drawRect(color = bar.copy(alpha = 0.14f * alpha), size = Size(w, h))
            val seg = w * 0.32f
            val x = sweep * w
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(bar.copy(alpha = 0f), bar, bar.copy(alpha = 0f)),
                    startX = x,
                    endX = x + seg,
                ),
                alpha = alpha,
                topLeft = Offset(x, 0f),
                size = Size(seg, h),
            )
        }
    }
}

/**
 * The app's one transition language: Material 3 fade-through. Outgoing content
 * fades out fast with no movement; incoming content fades in while settling
 * from 92% to full scale — no directional slide (screen switches are between
 * peers, and M3 reserves slides for hierarchy). Panes never overlap at partial
 * alpha (no brightness dip / flash) and the container never morphs size.
 * [contentKey] groups states that should share a pane — e.g. Idle/Loading of
 * the same screen — so only real content changes animate, and each pane keeps
 * rendering the state it was composed with (no mid-fade re-branching). Respects
 * the system animator-duration-scale.
 */
@Composable
fun <T> NativeSectionTransition(
    targetState: T,
    modifier: Modifier = Modifier,
    contentKey: (T) -> Any,
    content: @Composable (T) -> Unit,
) {
    // DIAGNOSTIC: switching is fully instant. If a "double" still appears
    // with zero animation, it is a data repaint, not motion.
    content(targetState)
}
@Composable
private fun rememberSecondaryTransitionDurationMillis(): Int {
    val context = LocalContext.current
    return remember {
        val scale = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        when {
            scale <= 0f -> 1
            else -> (SECONDARY_TRANSITION_BASE_MILLIS * scale).toInt().coerceIn(1, SECONDARY_TRANSITION_MAX_MILLIS)
        }
    }
}
