package com.saaf.milkshop.nativeui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Spotify/Airbnb-style press feedback: the element shrinks subtly while
 * pressed and springs back on release, on top of the existing ripple.
 *
 * Usage — thread the SAME source into the component so it observes presses:
 * ```
 * val press = rememberPressSource()
 * Button(onClick, interactionSource = press, modifier = Modifier.pressScale(press)) { ... }
 * ```
 * Scale runs in graphicsLayer (draw phase) so presses never trigger
 * recomposition of the button content.
 */
@Composable
fun rememberPressSource(): MutableInteractionSource = remember { MutableInteractionSource() }

@Composable
fun Modifier.pressScale(
    source: MutableInteractionSource,
    pressedScale: Float = 0.96f,
): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "press_scale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
