package com.autumn.nyaclash.ui.nodes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.autumn.nyaclash.data.ClashApi
import com.autumn.nyaclash.data.NodeLayout
import com.autumn.nyaclash.data.NodeSort
import com.autumn.nyaclash.data.ProxyGroup
import com.autumn.nyaclash.data.SettingsStore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NodesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var groups by remember { mutableStateOf<List<ProxyGroup>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var sortMenu by remember { mutableStateOf(false) }

    val latency = remember { mutableStateMapOf<String, Int>() }
    val reachable = remember { mutableStateMapOf<String, Boolean>() }

    suspend fun refresh() {
        loading = true
        runCatching { groups = ClashApi.groups() }
            .onFailure { message = "读取节点失败: ${it.message}" }
        loading = false
    }

    fun test(names: List<String>) {
        if (names.isEmpty() || testing) return
        scope.launch {
            testing = true
            coroutineScope {
                names.map { name ->
                    async {
                        latency[name] = ClashApi.delay(name)
                        reachable[name] = ClashApi.connectivity(name)
                    }
                }.awaitAll()
            }
            testing = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    val allNodes = remember(groups) { groups.flatMap { it.all }.distinct() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("节点", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = if (testing) "测速中…" else "${allNodes.size} 个节点",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = false,
                onClick = { scope.launch { refresh() } },
                enabled = !loading,
                label = { Text("刷新") },
            )
            FilterChip(
                selected = false,
                onClick = { test(allNodes) },
                enabled = !testing && allNodes.isNotEmpty(),
                label = { Text("测速") },
            )
            FilterChip(
                selected = false,
                onClick = { test(allNodes) },
                enabled = !testing && allNodes.isNotEmpty(),
                label = { Text("连通性") },
            )

            Box {
                FilterChip(
                    selected = sortMenu,
                    onClick = { sortMenu = true },
                    label = { Text("排序: ${SettingsStore.nodeSort.label()}") },
                )
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    NodeSort.entries.forEach { sort ->
                        DropdownMenuItem(
                            text = { Text(sort.label()) },
                            onClick = {
                                SettingsStore.setNodeSort(context, sort)
                                sortMenu = false
                            },
                        )
                    }
                }
            }

            FilterChip(
                selected = SettingsStore.nodeLayout == NodeLayout.Grid,
                onClick = {
                    SettingsStore.setNodeLayout(
                        context,
                        if (SettingsStore.nodeLayout == NodeLayout.Grid) NodeLayout.List else NodeLayout.Grid,
                    )
                },
                label = { Text(if (SettingsStore.nodeLayout == NodeLayout.Grid) "网格" else "列表") },
            )
        }

        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (groups.isEmpty() && !loading) {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    text = "没有代理组。请确认已连接且订阅里含 proxy-groups。",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(groups, key = { it.name }) { group ->
                GroupSection(
                    group = group,
                    layout = SettingsStore.nodeLayout,
                    sort = SettingsStore.nodeSort,
                    latency = latency,
                    reachable = reachable,
                    testing = testing,
                    onSelect = { name ->
                        scope.launch {
                            runCatching { ClashApi.select(group.name, name) }
                                .onFailure { message = "切换失败: ${it.message}" }
                            runCatching { groups = ClashApi.groups() }
                        }
                    },
                    onTestGroup = { test(group.all) },
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private fun NodeSort.label(): String = when (this) {
    NodeSort.Default -> "默认"
    NodeSort.Name -> "名称"
    NodeSort.Latency -> "延迟"
}

private fun latencyLabel(ms: Int?): String = when {
    ms == null -> ""
    ms < 0 -> "超时"
    else -> "$ms ms"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupSection(
    group: ProxyGroup,
    layout: NodeLayout,
    sort: NodeSort,
    latency: Map<String, Int>,
    reachable: Map<String, Boolean>,
    testing: Boolean,
    onSelect: (String) -> Unit,
    onTestGroup: () -> Unit,
) {
    val nodes = when (sort) {
        NodeSort.Default -> group.all
        NodeSort.Name -> group.all.sorted()
        NodeSort.Latency -> group.all.sortedBy { latency[it] ?: Int.MAX_VALUE }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "当前: ${group.now.ifBlank { "—" }}  ·  ${group.type}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = onTestGroup, enabled = !testing) { Text("测速") }
            }

            when (layout) {
                NodeLayout.List -> Column {
                    for (name in nodes) {
                        NodeRow(
                            name = name,
                            selected = name == group.now,
                            latency = latency[name],
                            reachable = reachable[name],
                            onClick = { onSelect(name) },
                        )
                    }
                }

                NodeLayout.Grid -> FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (name in nodes) {
                        val ms = latency[name]
                        val suffix = when {
                            reachable[name] == false -> " ✗"
                            ms != null && ms >= 0 -> " $ms"
                            ms != null && ms < 0 -> " 超时"
                            else -> ""
                        }
                        FilterChip(
                            selected = name == group.now,
                            onClick = { onSelect(name) },
                            label = {
                                Text(
                                    text = name + suffix,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeRow(
    name: String,
    selected: Boolean,
    latency: Int?,
    reachable: Boolean?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable { onClick() },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (reachable) {
                true -> Text("✓", color = MaterialTheme.colorScheme.primary)
                false -> Text("✗", color = MaterialTheme.colorScheme.error)
                null -> {}
            }
            val label = latencyLabel(latency)
            if (label.isNotEmpty()) {
                Text(
                    text = "  $label",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
