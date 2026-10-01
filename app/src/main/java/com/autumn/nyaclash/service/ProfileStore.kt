package com.autumn.nyaclash.service

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Stores the active mihomo profile under the app's private files dir. */
object ProfileStore {

    private fun dir(context: Context): File =
        File(context.filesDir, "profiles").apply { mkdirs() }

    fun activeConfig(context: Context): File = File(dir(context), "active.yaml")

    suspend fun importSubscription(context: Context, url: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val raw = download(url)
                activeConfig(context).writeText(normalize(raw))
            }
        }

    suspend fun importText(context: Context, text: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { activeConfig(context).writeText(normalize(text)) }
        }

    private fun download(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "nyaclash/0.1")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) error("HTTP $code")
            return connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Some providers return a base64-encoded YAML document. If the payload does
     * not look like mihomo YAML but decodes to something that does, decode it.
     */
    private fun normalize(content: String): String {
        if (content.contains("proxies:")) return content
        return runCatching {
            val decoded = String(
                Base64.decode(content.trim(), Base64.DEFAULT),
                Charsets.UTF_8,
            )
            if (decoded.contains("proxies:")) decoded else content
        }.getOrDefault(content)
    }
}
