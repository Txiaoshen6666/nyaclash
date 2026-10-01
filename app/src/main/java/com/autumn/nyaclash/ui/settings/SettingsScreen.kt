package com.autumn.nyaclash.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.autumn.nyaclash.BuildConfig
import com.autumn.nyaclash.core.NativeBridge
import com.autumn.nyaclash.data.DarkMode
import com.autumn.nyaclash.data.SettingsStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var dns by remember { mutableStateOf(SettingsStore.dns) }

    val coreVersion = remember {
        if (NativeBridge.available) {
            runCatching { NativeBridge.nativeVersion() }.getOrDefault("?")
        } else {
            "未打包"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium)

        SectionCard("外观") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("动态取色", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Material You（Android 12+）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = SettingsStore.dynamicColor,
                    onCheckedChange = { SettingsStore.setDynamicColor(context, it) },
                )
            }

            Spacer(Modifier.height(12.dp))

            Text("深色模式", style = MaterialTheme.typography.titleSmall)
            val darkModes = listOf(
                DarkMode.System to "跟随系统",
                DarkMode.Light to "浅色",
                DarkMode.Dark to "深色",
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                darkModes.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = SettingsStore.darkMode == value,
                        onClick = { SettingsStore.setDarkMode(context, value) },
                        shape = SegmentedButtonDefaults.itemShape(index, darkModes.size),
                        label = { Text(label) },
                    )
                }
            }
        }

        SectionCard("网络") {
            Text("TUN 栈", style = MaterialTheme.typography.titleSmall)
            val stacks = listOf("system", "gvisor", "mixed")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                stacks.forEachIndexed { index, value ->
                    SegmentedButton(
                        selected = SettingsStore.tunStack == value,
                        onClick = { SettingsStore.setTunStack(context, value) },
                        shape = SegmentedButtonDefaults.itemShape(index, stacks.size),
                        label = { Text(value) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = dns,
                onValueChange = { dns = it },
                label = { Text("DNS 服务器") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = {
                    SettingsStore.setDns(context, dns.trim().ifBlank { "172.19.0.2" })
                    dns = SettingsStore.dns
                }) { Text("保存") }
            }

            Text(
                text = "TUN 栈与 DNS 在下次连接时生效。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard("关于") {
            AboutRow("应用版本", BuildConfig.VERSION_NAME)
            AboutRow("内核", "mihomo $coreVersion")
            AboutRow("许可证", "GPL-3.0")
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
