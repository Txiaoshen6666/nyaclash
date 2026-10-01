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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.autumn.nyaclash.ui.dashboard.DashboardScreen
import com.autumn.nyaclash.ui.settings.SettingsScreen
import com.autumn.nyaclash.ui.subscriptions.SubscriptionsScreen

private enum class Tab(val label: String) {
    Dashboard("仪表板"),
    Subscriptions("订阅"),
    Settings("设置"),
}

@Composable
fun NyaClashApp() {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val tabs = Tab.entries

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    Tab.Dashboard -> Icons.Filled.Home
                                    Tab.Subscriptions -> Icons.AutoMirrored.Filled.List
                                    Tab.Settings -> Icons.Filled.Settings
                                },
                                contentDescription = tab.label,
                            )
                        },
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
        ) { index ->
            when (tabs[index]) {
                Tab.Dashboard -> DashboardScreen()
                Tab.Subscriptions -> SubscriptionsScreen()
                Tab.Settings -> SettingsScreen()
            }
        }
    }
}
