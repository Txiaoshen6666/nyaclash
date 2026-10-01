package com.autumn.nyaclash.ui.dashboard

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.autumn.nyaclash.data.ClashApi
import com.autumn.nyaclash.data.ProxyGroup
import com.autumn.nyaclash.data.Traffic
import com.autumn.nyaclash.service.NyaVpnService
import com.autumn.nyaclash.service.ProfileStore
import com.autumn.nyaclash.service.TunnelState
import com.autumn.nyaclash.ui.formatSpeed
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var message by remember { mutableStateOf<String?>(null) }
    var traffic by remember { mutableStateOf(Traffic(0, 0)) }
    var mode by remember { mutableStateOf("rule") }
    var groups by remember { mutableStateOf<List<ProxyGroup>>(emptyList()) }
    var loadingGroups by remember { mutableStateOf(false) }
    val latency = remember { mutableStateMapOf<String, Int>() }
    var expanded by remember { mutableStateOf<String?>(null) }

    val vpnLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            NyaVpnService.start(context)
        } else {
            message = "已拒绝 VPN 授权"
        }
    }

    LaunchedEffect(TunnelState.running) {
        if (TunnelState.running) {
            ClashApi.traffic().collect { traffic = it }
        } else {
            traffic = Traffic(0, 0)
        }
    }

    LaunchedEffect(TunnelState.running) {
        if (!TunnelState.running) {
            groups = emptyList()
            return@LaunchedEffect
        }
        runCatching { mode = ClashApi.mode() }
        loadingGroups = true
        runCatching { groups = ClashApi.groups() }
            .onFailure { message = "读取节点失败: ${it.message}" }
        loadingGroups = false
    }

    val activeProfile = ProfileStore.profiles.firstOrNull { it.id == ProfileStore.activeId }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("仪表板", style = MaterialTheme.typography.headlineMedium)
        }

        item {
            ConnectionCard(
                status = TunnelState.status,
                running = TunnelState.running,
                profileName = activeProfile?.name ?: "未导入订阅",
                onToggle = {
                    if (TunnelState.running) {
                        NyaVpnService.stop(context)
                    } else {
                        val prepare = VpnService.prepare(context)
                        if (prepare != null) vpnLauncher.launch(prepare) else NyaVpnService.start(context)
                    }
                },
            )
        }

        message?.let { text ->
            item {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        item {
            TrafficCard(up = traffic.up, down = traffic.down)
        }

        item {
            Column {
                SectionTitle("代理模式")
                Spacer(Modifier.height(8.dp))
                val modes = listOf("rule" to "规则", "global" to "全局", "direct" to "直连")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    modes.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = mode == value,
                            onClick = {
                                mode = value
                                if (TunnelState.running) {
                                    scope.launch { runCatching { ClashApi.setMode(value) } }
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                            label = { Text(label) },
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle("代理组")
                TextButton(
                    enabled = TunnelState.running && !loadingGroups,
                    onClick = {
                        scope.launch {
                            loadingGroups = true
                            runCatching { groups = ClashApi.groups() }
                                .onFailure { message = "读取节点失败: ${it.message}" }
                            loadingGroups = false
                        }
                    },
                ) { Text(if (loadingGroups) "刷新中…" else "刷新") }
            }
        }

        if (!TunnelState.running) {
            item {
                HintCard("连接后这里会显示代理组和节点，可切换节点、测延迟。")
            }
        } else if (groups.isEmpty() && !loadingGroups) {
            item { HintCard("没有代理组（订阅里可能没有 proxy-groups）。") }
        } else {
            items(groups, key = { it.name }) { group ->
                GroupCard(
                    group = group,
                    expanded = expanded == group.name,
                    latency = latency,
                    onToggleExpand = {
                        expanded = if (expanded == group.name) null else group.name
                    },
                    onSelect = { name ->
                        scope.launch {
                            runCatching { ClashApi.select(group.name, name) }
                                .onFailure { message = "切换失败: ${it.message}" }
                            runCatching { groups = ClashApi.groups() }
                        }
                    },
                    onTest = {
                        scope.launch {
                            coroutineScope {
                                group.all.map { name ->
                                    async { latency[name] = ClashApi.delay(name) }
                                }.awaitAll()
                            }
                        }
                    },
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun HintCard(text: String) {
    Card(Modifier.fillMaxWidth()) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ConnectionCard(
    status: String,
    running: Boolean,
    profileName: String,
    onToggle: () -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (running) {
                    LoadingIndicator(modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.size(12.dp))
                Column {
                    Text(
                        text = if (running) "已连接" else "未连接",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = "订阅: $profileName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (running) "断开" else "连接")
            }
        }
    }
}

@Composable
private fun TrafficCard(up: Long, down: Long) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TrafficColumn(Icons.Filled.KeyboardArrowUp, "上传", up)
            TrafficColumn(Icons.Filled.KeyboardArrowDown, "下载", down)
        }
    }
}

@Composable
private fun TrafficColumn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Long,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatSpeed(value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun GroupCard(
    group: ProxyGroup,
    expanded: Boolean,
    latency: Map<String, Int>,
    onToggleExpand: () -> Unit,
    onSelect: (String) -> Unit,
    onTest: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(group.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "当前: ${group.now.ifBlank { "—" }}  ·  ${group.type}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onTest) { Text("测延迟") }
                    }
                    for (name in group.all) {
                        val selected = name == group.now
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(name) }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                            val ms = latency[name]
                            if (ms != null) {
                                Text(
                                    text = if (ms < 0) "超时" else "$ms ms",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

