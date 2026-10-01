package com.autumn.nyaclash.ui.home

import android.app.Activity
import android.net.VpnService
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.autumn.nyaclash.BuildConfig
import com.autumn.nyaclash.core.NativeBridge
import com.autumn.nyaclash.service.NyaVpnService
import com.autumn.nyaclash.service.ProfileStore
import com.autumn.nyaclash.service.TunnelState
import com.autumn.nyaclash.ui.theme.NyaClashTheme
import kotlinx.coroutines.launch

@Composable
private fun rememberCoreStatus(): String {
    val context = LocalContext.current
    return remember {
        if (!NativeBridge.available) {
            "not bundled (built in CI)"
        } else {
            runCatching {
                NativeBridge.ensureInit(
                    homeDir = context.filesDir.absolutePath,
                    versionName = BuildConfig.VERSION_NAME,
                    gitVersion = "",
                    sdkVersion = Build.VERSION.SDK_INT,
                )
                "mihomo ${NativeBridge.nativeVersion()}"
            }.getOrElse { error ->
                "error: ${error.message ?: error.javaClass.simpleName}"
            }
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val coreStatus = rememberCoreStatus()

    var subscriptionUrl by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var hasProfile by remember { mutableStateOf(ProfileStore.activeConfig(context).exists()) }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            NyaVpnService.start(context)
        } else {
            message = "VPN permission denied"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "nyaclash", style = MaterialTheme.typography.displaySmall)
        Text(
            text = "Mihomo core · Material 3 Expressive",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = if (TunnelState.running) "Connected" else "Not connected",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Status: ${TunnelState.status}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Core: $coreStatus",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (hasProfile) "Profile: imported" else "Profile: none",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        OutlinedTextField(
            value = subscriptionUrl,
            onValueChange = { subscriptionUrl = it },
            label = { Text("Subscription URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                scope.launch {
                    message = "Importing…"
                    val result = ProfileStore.importSubscription(context, subscriptionUrl.trim())
                    message = result.fold(
                        onSuccess = {
                            hasProfile = true
                            "Imported"
                        },
                        onFailure = { "Import failed: ${it.message}" },
                    )
                }
            },
            enabled = subscriptionUrl.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Import subscription")
        }

        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (TunnelState.running) {
                    NyaVpnService.stop(context)
                } else {
                    val prepare = VpnService.prepare(context)
                    if (prepare != null) {
                        vpnPermissionLauncher.launch(prepare)
                    } else {
                        NyaVpnService.start(context)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (TunnelState.running) "Disconnect" else "Connect")
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
