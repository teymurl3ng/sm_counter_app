package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.nativeui.common.NativeBottomNavigation
import com.saaf.milkshop.nativeui.common.NativeNavigationLabel
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.hapticTap
import com.saaf.milkshop.nativeui.common.AdaptiveNavigationScaffold

/** Injected shell for Owner panes; mirrors common/HostedScaffold minus the nav-action wrap. */
typealias OwnScaffold = @Composable (
    snackbarHostState: SnackbarHostState,
    modifier: Modifier,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) -> Unit

@Composable
internal fun OwnerScaffold(
    snackbarHostState: SnackbarHostState,
    section: OwnerSection,
    onNavigate: (OwnerSection) -> Unit,
    modifier: Modifier = Modifier,
    // Zeroed by default so the green header bleeds under the status bar like
    // every other shell; individual screens may still pass their own insets.
    contentWindowInsets: androidx.compose.foundation.layout.WindowInsets =
        androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    AdaptiveNavigationScaffold(
        snackbarHostState = snackbarHostState,
        contentWindowInsets = contentWindowInsets,
        modifier = modifier.testTag(NativeUiTags.OwnerShellRoot),
        compactNavigation = {
            OwnerNavigation(selected = section, onNavigate = onNavigate)
        },
        expandedNavigation = {
            OwnerNavigationRail(selected = section, onNavigate = onNavigate)
        },
        content = content,
    )
}

@Composable
internal fun OwnerNavigationRail(
    selected: OwnerSection,
    syncAttention: OwnerSyncAttention? = null,
    onNavigate: (OwnerSection) -> Unit,
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        OwnerRailItem(selected == OwnerSection.Overview, NativeUiTags.OwnerNavOverview, { onNavigate(OwnerSection.Overview) }, Icons.Outlined.GridView, "Overview")
        OwnerRailItem(selected == OwnerSection.Work, NativeUiTags.OwnerNavWork, { onNavigate(OwnerSection.Work) }, Icons.Outlined.WorkOutline, "Work")
        OwnerRailItem(selected == OwnerSection.Records, NativeUiTags.OwnerNavRecords, { onNavigate(OwnerSection.Records) }, Icons.Outlined.History, "Records")
        OwnerRailItem(selected == OwnerSection.More, NativeUiTags.OwnerNavMore, { onNavigate(OwnerSection.More) }, Icons.Outlined.MoreHoriz, "More", syncAttention)
    }
}

@Composable
private fun OwnerRailItem(
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    syncAttention: OwnerSyncAttention? = null,
) {
    val context = LocalContext.current
    val onClickHaptic = { context.hapticTap(); onClick() }
    NavigationRailItem(
        selected = selected,
        onClick = onClickHaptic,
        modifier = Modifier.testTag(tag).heightIn(min = 56.dp).semantics {
            if (syncAttention != null) stateDescription = syncAttention.description
        },
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        icon = { OwnerNavigationIcon(icon, syncAttention) },
        label = { NativeNavigationLabel(label) },
    )
}

@Composable
internal fun OwnerNavigation(
    selected: OwnerSection,
    syncAttention: OwnerSyncAttention? = null,
    onNavigate: (OwnerSection) -> Unit,
) {
    NativeBottomNavigation {
        OwnerNavItem(
            selected = selected == OwnerSection.Overview,
            tag = NativeUiTags.OwnerNavOverview,
            onClick = { onNavigate(OwnerSection.Overview) },
            icon = Icons.Outlined.GridView,
            label = "Overview",
        )
        OwnerNavItem(
            selected = selected == OwnerSection.Work,
            tag = NativeUiTags.OwnerNavWork,
            onClick = { onNavigate(OwnerSection.Work) },
            icon = Icons.Outlined.WorkOutline,
            label = "Work",
        )
        OwnerNavItem(
            selected = selected == OwnerSection.Records,
            tag = NativeUiTags.OwnerNavRecords,
            onClick = { onNavigate(OwnerSection.Records) },
            icon = Icons.Outlined.History,
            label = "Records",
        )
        OwnerNavItem(
            selected = selected == OwnerSection.More,
            tag = NativeUiTags.OwnerNavMore,
            onClick = { onNavigate(OwnerSection.More) },
            icon = Icons.Outlined.MoreHoriz,
            label = "More",
            syncAttention = syncAttention,
        )
    }
}

@Composable
private fun RowScope.OwnerNavItem(
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    syncAttention: OwnerSyncAttention? = null,
) {
    val context = LocalContext.current
    val onClickHaptic = { context.hapticTap(); onClick() }
    NavigationBarItem(
        selected = selected,
        onClick = onClickHaptic,
        modifier = Modifier
            .testTag(tag)
            .heightIn(min = 56.dp)
            .semantics { if (syncAttention != null) stateDescription = syncAttention.description },
        icon = { OwnerNavigationIcon(icon, syncAttention) },
        label = { NativeNavigationLabel(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
private fun OwnerNavigationIcon(icon: ImageVector, attention: OwnerSyncAttention?) {
    BadgedBox(badge = {
        attention?.count?.takeIf { it > 0 }?.let { count ->
            val density = LocalDensity.current
            // The duplicated status glyph stays compact; the exact count is available
            // in navigation semantics and fully scaled text on the Sync menu row.
            CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(1.3f))) {
                Badge(Modifier.testTag(NativeUiTags.OwnerSyncReviewBadge)) {
                    Text(if (count > 99) "99+" else count.toString())
                }
            }
        }
    }) { Icon(icon, contentDescription = null) }
}
