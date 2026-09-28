package com.saaf.milkshop.nativeui.manager

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.hapticTap
import com.saaf.milkshop.nativeui.common.AdaptiveNavigationScaffold

@Composable
internal fun ManagerScaffold(
    snackbarHostState: SnackbarHostState,
    section: ManagerSection,
    onNavigate: (ManagerSection) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    AdaptiveNavigationScaffold(
        snackbarHostState = snackbarHostState,
        modifier = modifier.testTag(NativeUiTags.ManagerShellRoot),
        compactNavigation = {
            ManagerNavigation(selected = section, onNavigate = onNavigate)
        },
        expandedNavigation = {
            ManagerNavigationRail(selected = section, onNavigate = onNavigate)
        },
        content = content,
    )
}

@Composable
private fun ManagerNavigationRail(
    selected: ManagerSection,
    onNavigate: (ManagerSection) -> Unit,
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        ManagerRailItem(selected == ManagerSection.Work, NativeUiTags.ManagerNavWork, { onNavigate(ManagerSection.Work) }, Icons.Outlined.WorkOutline, "Work")
        ManagerRailItem(selected == ManagerSection.Close, NativeUiTags.ManagerNavClose, { onNavigate(ManagerSection.Close) }, Icons.Outlined.Lock, "Close")
        ManagerRailItem(selected == ManagerSection.Records, NativeUiTags.ManagerNavRecords, { onNavigate(ManagerSection.Records) }, Icons.Outlined.History, "Records")
        ManagerRailItem(selected == ManagerSection.More, NativeUiTags.ManagerNavMore, { onNavigate(ManagerSection.More) }, Icons.Outlined.MoreHoriz, "More")
    }
}

@Composable
private fun ManagerRailItem(
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
) {
    val view = LocalView.current
    val onClickHaptic = { view.hapticTap(); onClick() }
    NavigationRailItem(
        selected = selected,
        onClick = onClickHaptic,
        modifier = Modifier.testTag(tag).heightIn(min = 56.dp),
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
    )
}

@Composable
internal fun ManagerNavigation(
    selected: ManagerSection,
    onNavigate: (ManagerSection) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
        ManagerNavItem(
            selected = selected == ManagerSection.Work,
            tag = NativeUiTags.ManagerNavWork,
            onClick = { onNavigate(ManagerSection.Work) },
            icon = Icons.Outlined.WorkOutline,
            label = "Work",
        )
        ManagerNavItem(
            selected = selected == ManagerSection.Close,
            tag = NativeUiTags.ManagerNavClose,
            onClick = { onNavigate(ManagerSection.Close) },
            icon = Icons.Outlined.Lock,
            label = "Close",
        )
        ManagerNavItem(
            selected = selected == ManagerSection.Records,
            tag = NativeUiTags.ManagerNavRecords,
            onClick = { onNavigate(ManagerSection.Records) },
            icon = Icons.Outlined.History,
            label = "Records",
        )
        ManagerNavItem(
            selected = selected == ManagerSection.More,
            tag = NativeUiTags.ManagerNavMore,
            onClick = { onNavigate(ManagerSection.More) },
            icon = Icons.Outlined.MoreHoriz,
            label = "More",
        )
    }
}

@Composable
private fun RowScope.ManagerNavItem(
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
) {
    val view = LocalView.current
    val onClickHaptic = { view.hapticTap(); onClick() }
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
