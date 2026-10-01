package com.autumn.nyaclash.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.SecureRandom

enum class DarkMode { System, Light, Dark }

/** Persisted user settings, exposed as Compose state. */
object SettingsStore {
    private const val PREFS = "nyaclash.settings"
    private const val KEY_DYNAMIC = "dynamic_color"
    private const val KEY_DARK = "dark_mode"
    private const val KEY_STACK = "tun_stack"
    private const val KEY_DNS = "tun_dns"
    private const val KEY_SECRET = "controller_secret"

    var dynamicColor by mutableStateOf(true)
    var darkMode by mutableStateOf(DarkMode.System)
    var tunStack by mutableStateOf("mixed")
    var dns by mutableStateOf("172.19.0.2")

    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        val prefs = prefs(context)
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC, true)
        darkMode = runCatching {
            DarkMode.valueOf(prefs.getString(KEY_DARK, DarkMode.System.name)!!)
        }.getOrDefault(DarkMode.System)
        tunStack = prefs.getString(KEY_STACK, "mixed") ?: "mixed"
        dns = prefs.getString(KEY_DNS, "172.19.0.2") ?: "172.19.0.2"
        loaded = true
    }

    fun setDynamicColor(context: Context, value: Boolean) {
        dynamicColor = value
        prefs(context).edit().putBoolean(KEY_DYNAMIC, value).apply()
    }

    fun setDarkMode(context: Context, value: DarkMode) {
        darkMode = value
        prefs(context).edit().putString(KEY_DARK, value.name).apply()
    }

    fun setTunStack(context: Context, value: String) {
        tunStack = value
        prefs(context).edit().putString(KEY_STACK, value).apply()
    }

    fun setDns(context: Context, value: String) {
        dns = value
        prefs(context).edit().putString(KEY_DNS, value).apply()
    }

    /** Stable random secret for mihomo's external controller. */
    fun controllerSecret(context: Context): String {
        val prefs = prefs(context)
        prefs.getString(KEY_SECRET, null)?.let { return it }
        val bytes = ByteArray(24).also { SecureRandom().nextBytes(it) }
        val secret = bytes.joinToString("") { "%02x".format(it) }
        prefs.edit().putString(KEY_SECRET, secret).apply()
        return secret
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
