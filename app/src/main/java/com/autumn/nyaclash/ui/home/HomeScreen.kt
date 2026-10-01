package com.autumn.nyaclash.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.autumn.nyaclash.core.NativeBridge
import com.autumn.nyaclash.ui.theme.NyaClashTheme

@Composable
private fun rememberCoreStatus(): String {
    val context = LocalContext.current
    return remember {
        if (!NativeBridge.available) {
            "not bundled (built in CI)"
        } else {
            runCatching {
                NativeBridge.nativeInit(context.filesDir.absolutePath)
                "mihomo ${NativeBridge.nativeVersion()}"
            }.getOrElse { error ->
                "error: ${error.message ?: error.javaClass.simpleName}"
            }
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val coreStatus = rememberCoreStatus()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "nyaclash",
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            text = "Mihomo core · Material 3 Expressive",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Not connected",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Core: $coreStatus",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Connect")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    NyaClashTheme(dynamicColor = false) {
        HomeScreen()
    }
}
