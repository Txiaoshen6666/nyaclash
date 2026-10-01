package com.autumn.nyaclash.service

import android.content.Context
import com.autumn.nyaclash.core.NativeBridge
import com.autumn.nyaclash.data.SettingsStore
import java.io.File

/**
 * Builds the runtime config used when connecting: the active profile with
 * mihomo's external controller injected so the UI can talk to the REST API.
 */
object RuntimeConfig {
    const val HOST = "127.0.0.1"
    const val PORT = 9090
    const val CONTROLLER = "$HOST:$PORT"

    fun path(context: Context): File = File(context.filesDir, "profiles/runtime.yaml")

    fun prepare(context: Context): Result<File> = runCatching {
        val profile = ProfileStore.activeConfigFile(context) ?: error("没有可用的订阅")
        val out = path(context)
        out.parentFile?.mkdirs()

        val message = NativeBridge.nativePrepareConfig(
            profilePath = profile.absolutePath,
            outPath = out.absolutePath,
            controller = CONTROLLER,
            secret = SettingsStore.controllerSecret(context),
        )
        if (message != null) error(message)

        out
    }
}
