package com.saaf.milkshop.nativeui.common

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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp


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
            onClick = com.saaf.milkshop.nativeui.common.hapticAction(onRetry),
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
 * Shared pane crossfade. Keep the outgoing pane while the incoming pane appears;
 * never shrink full-screen chrome or expose the window with an instant exit.
 * Disable size interpolation so status bars and IME bounds stay stationary.
 * [contentKey] groups states that should share a pane — e.g. Idle/Loading of
 * the same screen — so only real content changes animate, and each pane keeps
 * rendering the state it was composed with (no mid-fade re-branching). Respects
 * the system animator-duration-scale.
 */
@Composable
fun <T> NativeSectionTransition(
    targetState: T,
    modifier: Modifier = Modifier,
    contentKey: (T) -> Any?,
    content: @Composable (T) -> Unit,
) {
    val targetKey = contentKey(targetState)
    val parentActive = LocalPaneActive.current
    NativeFailureFeedback(targetState, parentActive)
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(targetKey) {
        focus.clearFocus()
        keyboard?.hide()
    }
    AnimatedContent(
        targetState = targetState,
        transitionSpec = {
            (fadeIn(tween(180)) togetherWith fadeOut(tween(180))).using(null)
        },
        contentKey = contentKey,
        modifier = modifier,
        label = "section_transition",
    ) { state ->
        val active = parentActive && contentKey(state) == targetKey
        val paneModifier = if (active) Modifier.zIndex(1f) else Modifier.zIndex(0f)
            .clearAndSetSemantics { }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            }
        CompositionLocalProvider(LocalPaneActive provides active) {
            Box(paneModifier) { content(state) }
        }
    }
}
