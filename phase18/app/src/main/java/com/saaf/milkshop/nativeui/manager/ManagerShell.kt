package com.saaf.milkshop.nativeui.manager

import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.nativeui.NativeUiTags

@Composable
internal fun ManagerScaffold(
    snackbarHostState: SnackbarHostState,
    section: ManagerSection,
    onNavigate: (ManagerSection) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier.testTag(NativeUiTags.ManagerShellRoot),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ManagerNavigation(selected = section, onNavigate = onNavigate)
        },
        content = content,
    )
}

@Composable
internal fun ManagerNavigation(
    selected: ManagerSection,
    onNavigate: (ManagerSection) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
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
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
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
