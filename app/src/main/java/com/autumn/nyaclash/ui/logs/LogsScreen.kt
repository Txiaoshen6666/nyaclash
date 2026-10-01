package com.autumn.nyaclash.ui.logs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.autumn.nyaclash.data.CoreLog
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var lines by remember { mutableStateOf(CoreLog.read(context)) }
    var autoRefresh by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    LaunchedEffect(autoRefresh) {
        while (autoRefresh) {
            lines = CoreLog.read(context)
            delay(2000)
        }
    }

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("日志", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "${lines.size} 行",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = autoRefresh,
                onClick = { autoRefresh = !autoRefresh },
                label = { Text("自动刷新") },
            )
            FilterChip(
                selected = false,
                onClick = { lines = CoreLog.read(context) },
                label = { Text("刷新") },
            )
            FilterChip(
                selected = false,
                onClick = {
                    val text = lines.joinToString("\n")
                    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    manager.setPrimaryClip(ClipData.newPlainText("nyaclash log", text))
                    Toast.makeText(context, "日志已复制", Toast.LENGTH_SHORT).show()
                },
                label = { Text("复制") },
            )
            FilterChip(
                selected = false,
                onClick = {
                    CoreLog.clear(context)
                    lines = CoreLog.read(context)
                },
                label = { Text("清空") },
            )
        }

        if (lines.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    text = "暂无日志。连接一次后这里会出现内核日志；失败时也会记录原因。",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
            ) {
                items(lines) { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            line.contains("[ERROR]") -> MaterialTheme.colorScheme.error
                            line.contains("[WARNING]") -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
