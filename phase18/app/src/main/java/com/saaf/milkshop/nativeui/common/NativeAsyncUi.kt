package com.saaf.milkshop.nativeui.common

import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Short continuity cue for destinations inside a menu; never use for primary
 * navigation. Scales with the system animator duration scale and snaps when
 * the user removes animations.
 */
@Composable
fun <T> NativeSecondaryTransition(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val durationMillis = rememberSecondaryTransitionDurationMillis()
    Crossfade(
        targetState = targetState,
        modifier = modifier,
        animationSpec = tween(durationMillis = durationMillis),
        label = "secondary_destination",
        content = content,
    )
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
