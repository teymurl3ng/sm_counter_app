package com.saaf.milkshop.nativeui.welcome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.R
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.theme.SaafGreen
import com.saaf.milkshop.nativeui.theme.SaafInk
import com.saaf.milkshop.nativeui.theme.SaafIvory
import com.saaf.milkshop.nativeui.theme.SaafSurface
import com.saaf.milkshop.nativeui.theme.SaafYellow

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SaafGreen,
        contentColor = SaafSurface,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.65f))
            SaafBrandMark()
            Spacer(Modifier.height(22.dp))
            Text(
                text = stringResource(R.string.native_app_name),
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.native_welcome_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = SaafSurface.copy(alpha = 0.86f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            Surface(
                color = SaafSurface,
                contentColor = SaafInk,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    WelcomeAssurance(
                        icon = { Icon(Icons.Outlined.CloudOff, contentDescription = null) },
                        text = stringResource(R.string.native_offline_assurance),
                    )
                    Surface(color = SaafGreen.copy(alpha = 0.16f), modifier = Modifier.fillMaxWidth().height(1.dp)) {}
                    WelcomeAssurance(
                        icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                        text = stringResource(R.string.native_private_access_assurance),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag(NativeUiTags.WelcomeContinue),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaafYellow,
                    contentColor = SaafInk,
                ),
            ) {
                Text(
                    text = stringResource(R.string.native_continue),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.native_continue_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = SaafSurface.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SaafBrandMark() {
    val description = stringResource(R.string.native_brand_mark)
    Surface(
        color = SaafIvory,
        contentColor = SaafGreen,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.size(92.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp)
                .semantics { contentDescription = description },
        ) {
            val stroke = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            drawRoundRect(
                color = SaafGreen,
                topLeft = Offset(size.width * 0.23f, size.height * 0.28f),
                size = Size(size.width * 0.54f, size.height * 0.61f),
                cornerRadius = CornerRadius(7.dp.toPx()),
                style = stroke,
            )
            drawLine(
                color = SaafGreen,
                start = Offset(size.width * 0.35f, size.height * 0.12f),
                end = Offset(size.width * 0.65f, size.height * 0.12f),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = SaafGreen,
                start = Offset(size.width * 0.35f, size.height * 0.12f),
                end = Offset(size.width * 0.35f, size.height * 0.28f),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = SaafGreen,
                start = Offset(size.width * 0.65f, size.height * 0.12f),
                end = Offset(size.width * 0.65f, size.height * 0.28f),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = SaafYellow,
                start = Offset(size.width * 0.34f, size.height * 0.58f),
                end = Offset(size.width * 0.66f, size.height * 0.58f),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
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
            color = SaafGreen.copy(alpha = 0.09f),
            contentColor = SaafGreen,
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
