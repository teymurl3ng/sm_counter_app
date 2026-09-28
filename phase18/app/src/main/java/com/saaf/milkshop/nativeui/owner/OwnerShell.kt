package com.saaf.milkshop.nativeui.owner

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.hapticTap
import com.saaf.milkshop.nativeui.common.AdaptiveNavigationScaffold

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
private fun OwnerNavigationRail(
    selected: OwnerSection,
    onNavigate: (OwnerSection) -> Unit,
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        OwnerRailItem(selected == OwnerSection.Overview, NativeUiTags.OwnerNavOverview, { onNavigate(OwnerSection.Overview) }, Icons.Outlined.GridView, "Overview")
        OwnerRailItem(selected == OwnerSection.Work, NativeUiTags.OwnerNavWork, { onNavigate(OwnerSection.Work) }, Icons.Outlined.WorkOutline, "Work")
        OwnerRailItem(selected == OwnerSection.Records, NativeUiTags.OwnerNavRecords, { onNavigate(OwnerSection.Records) }, Icons.Outlined.History, "Records")
        OwnerRailItem(selected == OwnerSection.More, NativeUiTags.OwnerNavMore, { onNavigate(OwnerSection.More) }, Icons.Outlined.MoreHoriz, "More")
    }
}

@Composable
private fun OwnerRailItem(
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
) {
    val context = LocalContext.current
    val onClickHaptic = { context.hapticTap(); onClick() }
    NavigationRailItem(
        selected = selected,
        onClick = onClickHaptic,
        modifier = Modifier.testTag(tag).heightIn(min = 56.dp),
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
    )
}

@Composable
internal fun OwnerNavigation(
    selected: OwnerSection,
    onNavigate: (OwnerSection) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
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
) {
    val context = LocalContext.current
    val onClickHaptic = { context.hapticTap(); onClick() }
    NavigationBarItem(
        selected = selected,
        onClick = onClickHaptic,
        modifier = Modifier
            .testTag(tag)
            .heightIn(min = 56.dp),
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}
