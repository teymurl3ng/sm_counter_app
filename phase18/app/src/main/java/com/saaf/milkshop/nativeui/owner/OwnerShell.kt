package com.saaf.milkshop.nativeui.owner

import androidx.compose.foundation.layout.RowScope
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
internal fun OwnerScaffold(
    snackbarHostState: SnackbarHostState,
    section: OwnerSection,
    onNavigate: (OwnerSection) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier.testTag(NativeUiTags.OwnerShellRoot),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            OwnerNavigation(selected = section, onNavigate = onNavigate)
        },
        content = content,
    )
}

@Composable
internal fun OwnerNavigation(
    selected: OwnerSection,
    onNavigate: (OwnerSection) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
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
