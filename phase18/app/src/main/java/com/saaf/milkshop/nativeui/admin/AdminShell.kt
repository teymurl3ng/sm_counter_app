package com.saaf.milkshop.nativeui.admin

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.WorkOutline
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.hapticTap
import com.saaf.milkshop.nativeui.common.AdaptiveNavigationScaffold

@Composable
internal fun AdminScaffold(
    snackbarHostState: SnackbarHostState,
    section: AdminSection,
    onNavigate: (AdminSection) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    AdaptiveNavigationScaffold(
        snackbarHostState = snackbarHostState,
        modifier = modifier.testTag(NativeUiTags.AdminShellRoot),
        compactNavigation = {
            AdminNavigation(selected = section, onNavigate = onNavigate)
        },
        expandedNavigation = {
            AdminNavigationRail(selected = section, onNavigate = onNavigate)
        },
        content = content,
    )
}

@Composable
internal fun AdminNavigationRail(
    selected: AdminSection,
    onNavigate: (AdminSection) -> Unit,
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        AdminRailItem(selected == AdminSection.Work, NativeUiTags.AdminNavWork, { onNavigate(AdminSection.Work) }, Icons.Outlined.WorkOutline, "Work")
        AdminRailItem(selected == AdminSection.People, NativeUiTags.AdminNavPeople, { onNavigate(AdminSection.People) }, Icons.Outlined.People, "People")
        AdminRailItem(selected == AdminSection.Records, NativeUiTags.AdminNavRecords, { onNavigate(AdminSection.Records) }, Icons.Outlined.History, "Records")
        AdminRailItem(selected == AdminSection.More, NativeUiTags.AdminNavMore, { onNavigate(AdminSection.More) }, Icons.Outlined.MoreHoriz, "More")
    }
}

@Composable
private fun AdminRailItem(
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
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
    )
}

@Composable
internal fun AdminNavigation(
    selected: AdminSection,
    onNavigate: (AdminSection) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
        AdminNavItem(
            selected = selected == AdminSection.Work,
            tag = NativeUiTags.AdminNavWork,
            onClick = { onNavigate(AdminSection.Work) },
            icon = Icons.Outlined.WorkOutline,
            label = "Work",
        )
        AdminNavItem(
            selected = selected == AdminSection.People,
            tag = NativeUiTags.AdminNavPeople,
            onClick = { onNavigate(AdminSection.People) },
            icon = Icons.Outlined.People,
            label = "People",
        )
        AdminNavItem(
            selected = selected == AdminSection.Records,
            tag = NativeUiTags.AdminNavRecords,
            onClick = { onNavigate(AdminSection.Records) },
            icon = Icons.Outlined.History,
            label = "Records",
        )
        AdminNavItem(
            selected = selected == AdminSection.More,
            tag = NativeUiTags.AdminNavMore,
            onClick = { onNavigate(AdminSection.More) },
            icon = Icons.Outlined.MoreHoriz,
            label = "More",
        )
    }
}

@Composable
private fun RowScope.AdminNavItem(
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
