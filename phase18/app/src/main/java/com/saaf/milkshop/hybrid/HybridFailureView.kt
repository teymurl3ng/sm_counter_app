package com.saaf.milkshop.hybrid

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.saaf.milkshop.R

internal fun createHybridFailureView(
    context: Context,
    title: String,
    message: String,
    recoveryAction: (() -> Unit)? = null,
): View {
    val horizontalPadding = context.dp(28)
    val verticalPadding = context.dp(36)
    val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        addView(TextView(context).apply {
            id = R.id.hybrid_failure_title
            text = title
            textSize = 24f
            setTextColor(Color.rgb(15, 23, 42))
            setTypeface(typeface, Typeface.BOLD)
        }, matchWrap())
        addView(TextView(context).apply {
            id = R.id.hybrid_failure_message
            text = message
            textSize = 16f
            setTextColor(Color.rgb(71, 85, 105))
            setLineSpacing(0f, 1.25f)
        }, matchWrap(topMargin = context.dp(14)))
        if (recoveryAction != null) addView(Button(context).apply {
            id = R.id.hybrid_recovery_action
            text = "Reload Saaf Milk securely"
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(4, 120, 87))
            setOnClickListener { recoveryAction() }
        }, matchWrap(topMargin = context.dp(24)))
    }
    return ScrollView(context).apply {
        id = R.id.hybrid_failure_root
        isFillViewport = true
        setBackgroundColor(Color.rgb(247, 246, 240))
        addView(content, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
    }
}

private fun matchWrap(topMargin: Int = 0) = LinearLayout.LayoutParams(
    LinearLayout.LayoutParams.MATCH_PARENT,
    LinearLayout.LayoutParams.WRAP_CONTENT,
).apply { this.topMargin = topMargin }

private fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
