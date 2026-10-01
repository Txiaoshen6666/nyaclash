package com.autumn.nyaclash.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.SecureRandom

enum class DarkMode { System, Light, Dark }

enum class NodeSort { Default, Name, Latency }

enum class NodeLayout { List, Grid }

/** Persisted user settings, exposed as Compose state. */
object SettingsStore {
    private const val PREFS = "nyaclash.settings"
    private const val KEY_DYNAMIC = "dynamic_color"
    private const val KEY_DARK = "dark_mode"
    private const val KEY_STACK = "tun_stack"
    private const val KEY_DNS = "tun_dns"
    private const val KEY_SECRET = "controller_secret"
    private const val KEY_NODE_SORT = "node_sort"
    private const val KEY_NODE_LAYOUT = "node_layout"

    var dynamicColor by mutableStateOf(true)
    var darkMode by mutableStateOf(DarkMode.System)
    var tunStack by mutableStateOf("mixed")
    var dns by mutableStateOf("172.19.0.2")
    var nodeSort by mutableStateOf(NodeSort.Default)
    var nodeLayout by mutableStateOf(NodeLayout.List)

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
        nodeSort = runCatching {
            NodeSort.valueOf(prefs.getString(KEY_NODE_SORT, NodeSort.Default.name)!!)
        }.getOrDefault(NodeSort.Default)
        nodeLayout = runCatching {
            NodeLayout.valueOf(prefs.getString(KEY_NODE_LAYOUT, NodeLayout.List.name)!!)
        }.getOrDefault(NodeLayout.List)
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

    fun setNodeSort(context: Context, value: NodeSort) {
        nodeSort = value
        prefs(context).edit().putString(KEY_NODE_SORT, value.name).apply()
    }

    fun setNodeLayout(context: Context, value: NodeLayout) {
        nodeLayout = value
        prefs(context).edit().putString(KEY_NODE_LAYOUT, value.name).apply()
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
