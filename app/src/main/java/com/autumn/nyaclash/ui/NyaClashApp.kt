package com.autumn.nyaclash.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.autumn.nyaclash.service.TunnelState
import com.autumn.nyaclash.ui.dashboard.DashboardScreen
import com.autumn.nyaclash.ui.logs.LogsScreen
import com.autumn.nyaclash.ui.nodes.NodesScreen
import com.autumn.nyaclash.ui.settings.SettingsScreen
import com.autumn.nyaclash.ui.subscriptions.SubscriptionsScreen
import com.autumn.nyaclash.ui.theme.NyaNodesIcon

private enum class Tab(val label: String) {
    Dashboard("仪表板"),
    Nodes("节点"),
    Subscriptions("订阅"),
    Logs("日志"),
    Settings("设置"),
}

private fun iconOf(tab: Tab): ImageVector = when (tab) {
    Tab.Dashboard -> Icons.Filled.Home
    Tab.Nodes -> NyaNodesIcon
    Tab.Subscriptions -> Icons.AutoMirrored.Filled.List
    Tab.Logs -> Icons.Filled.Info
    Tab.Settings -> Icons.Filled.Settings
}

@Composable
fun NyaClashApp() {
    // The Nodes tab only exists while the tunnel is up.
    val tabs = remember(TunnelState.running) {
        buildList {
            add(Tab.Dashboard)
            if (TunnelState.running) add(Tab.Nodes)
            add(Tab.Subscriptions)
            add(Tab.Logs)
            add(Tab.Settings)
        }
    }

    var selectedName by rememberSaveable { mutableStateOf(Tab.Dashboard.name) }
    val selected = tabs.firstOrNull { it.name == selectedName } ?: tabs.first()

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selected,
                        onClick = { selectedName = tab.name },
                        icon = { Icon(iconOf(tab), contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(160))
            },
            label = "tab",
            modifier = Modifier.padding(padding),
        ) { tab ->
            when (tab) {
                Tab.Dashboard -> DashboardScreen()
                Tab.Nodes -> NodesScreen()
                Tab.Subscriptions -> SubscriptionsScreen()
                Tab.Logs -> LogsScreen()
                Tab.Settings -> SettingsScreen()
            }
        }
    }
}
