package com.saaf.milkshop.nativeui.welcome

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.R
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.hapticTap
import com.saaf.milkshop.nativeui.common.pressScale
import com.saaf.milkshop.nativeui.common.rememberPressSource
import com.saaf.milkshop.nativeui.theme.SaafGreen
import com.saaf.milkshop.nativeui.theme.SaafInk
import com.saaf.milkshop.nativeui.theme.SaafIvory
import com.saaf.milkshop.nativeui.theme.SaafLine
import com.saaf.milkshop.nativeui.theme.SaafMuted
import com.saaf.milkshop.nativeui.theme.SaafSurface
import com.saaf.milkshop.nativeui.theme.SaafYellow

@Composable
fun WelcomeScreen(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground, modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            com.saaf.milkshop.nativeui.common.NativeChromeBand(title = "Welcome", reserveNavigationSpace = true, reserveActionSpace = false) {
                com.saaf.milkshop.nativeui.common.NativeChromeContext(com.saaf.milkshop.nativeui.common.LocalShopName.current)
                com.saaf.milkshop.nativeui.common.NativeChromeContext("Daily work", prominent = true)
                com.saaf.milkshop.nativeui.common.NativeChromeContext("Offline and private")
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                SaafBrandMark()
                Spacer(Modifier.height(18.dp))
                Text(com.saaf.milkshop.nativeui.common.LocalShopName.current,
                    style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.native_welcome_tagline), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                WelcomeLedgerProgress()
                Spacer(Modifier.height(24.dp))
                Surface(color = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()) {
                    Column {
                        WelcomeAssurance(icon = { Icon(Icons.Outlined.CloudOff, contentDescription = null) },
                            text = stringResource(R.string.native_offline_assurance))
                        Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = .16f),
                            modifier = Modifier.fillMaxWidth().height(1.dp)) {}
                        WelcomeAssurance(icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                            text = stringResource(R.string.native_private_access_assurance))
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                val hapticContext = LocalContext.current
                val press = rememberPressSource()
                Button(onClick = { hapticContext.hapticTap(); onContinue() },
                    modifier = Modifier.pressScale(press).fillMaxWidth().height(56.dp).testTag(NativeUiTags.WelcomeContinue),
                    shape = RoundedCornerShape(14.dp), interactionSource = press,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary)) {
                    Text(stringResource(R.string.native_continue), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.native_continue_hint), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

/**
 * Canonical brand mark: the exact launcher-icon foreground (milk vessel,
 * golden purity drop, verification badge on the green tile). One drawing for
 * the app icon and the sign-in screen — no wrapper frames, no drift.
 * Entrance: settles from 92% scale + slight offset, springing into place.
 */
@Composable
private fun SaafBrandMark() {
    val description = "${com.saaf.milkshop.nativeui.common.LocalShopName.current} mark"
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "brand_scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(220),
        label = "brand_alpha",
    )
    Image(
        painter = painterResource(R.drawable.ic_launcher_foreground),
        contentDescription = description,
        modifier = Modifier
            .size(120.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
    )
}

/** Animated ledger-ready sequence from the Counter Ledger welcome design. */
@Composable
private fun WelcomeLedgerProgress() {
    Text(
        text = "Sales, stock and daily cash in one place",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun WelcomeAssurance(
    icon: @Composable () -> Unit,
    text: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
            contentColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(11.dp),
            modifier = Modifier.size(42.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                icon()
            }
        }
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}
