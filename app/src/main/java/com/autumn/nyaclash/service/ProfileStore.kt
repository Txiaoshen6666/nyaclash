package com.autumn.nyaclash.service

import android.content.Context
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class Profile(
    val id: String,
    val name: String,
    val url: String,
    val updatedAt: Long,
    val upload: Long,
    val download: Long,
    val total: Long,
    val expire: Long,
)

/** Multiple mihomo profiles stored under the app's private files dir. */
object ProfileStore {
    private const val PREFS = "nyaclash.profiles"
    private const val KEY_INDEX = "index"
    private const val KEY_ACTIVE = "active_id"

    var profiles by mutableStateOf<List<Profile>>(emptyList())
    var activeId by mutableStateOf<String?>(null)

    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        val prefs = prefs(context)
        profiles = runCatching { parseIndex(prefs.getString(KEY_INDEX, null)) }
            .getOrDefault(emptyList())
        activeId = prefs.getString(KEY_ACTIVE, null)?.takeIf { id -> profiles.any { it.id == id } }
            ?: profiles.firstOrNull()?.id
        loaded = true
    }

    private fun dir(context: Context) = File(context.filesDir, "profiles").apply { mkdirs() }

    fun configFile(context: Context, id: String) = File(dir(context), "$id.yaml")

    fun activeConfigFile(context: Context): File? {
        ensureLoaded(context)
        return activeId?.let { configFile(context, it).takeIf(File::exists) }
    }

    private fun persist(context: Context) {
        prefs(context).edit()
            .putString(KEY_INDEX, indexJson())
            .putString(KEY_ACTIVE, activeId)
            .apply()
    }

    suspend fun add(context: Context, url: String): Result<Profile> = withContext(Dispatchers.IO) {
        runCatching {
            ensureLoaded(context)
            val fetched = fetch(url)
            val id = UUID.randomUUID().toString()
            configFile(context, id).writeText(normalize(fetched.body))
            val profile = Profile(
                id = id,
                name = fetched.name ?: "订阅 ${profiles.size + 1}",
                url = url,
                updatedAt = System.currentTimeMillis(),
                upload = fetched.upload,
                download = fetched.download,
                total = fetched.total,
                expire = fetched.expire,
            )
            profiles = profiles + profile
            activeId = profile.id
            persist(context)
            profile
        }
    }

    suspend fun update(context: Context, id: String): Result<Profile> = withContext(Dispatchers.IO) {
        runCatching {
            ensureLoaded(context)
            val existing = profiles.firstOrNull { it.id == id } ?: error("订阅不存在")
            val fetched = fetch(existing.url)
            configFile(context, id).writeText(normalize(fetched.body))
            val updated = existing.copy(
                updatedAt = System.currentTimeMillis(),
                upload = fetched.upload,
                download = fetched.download,
                total = fetched.total,
                expire = fetched.expire,
            )
            profiles = profiles.map { if (it.id == id) updated else it }
            persist(context)
            updated
        }
    }

    fun setActive(context: Context, id: String) {
        activeId = id
        persist(context)
    }

    fun remove(context: Context, id: String) {
        configFile(context, id).delete()
        profiles = profiles.filterNot { it.id == id }
        if (activeId == id) activeId = profiles.firstOrNull()?.id
        persist(context)
    }

    fun rename(context: Context, id: String, name: String) {
        profiles = profiles.map { if (it.id == id) it.copy(name = name) else it }
        persist(context)
    }

    // --- helpers ---

    private data class Fetched(
        val body: String,
        val name: String?,
        val upload: Long,
        val download: Long,
        val total: Long,
        val expire: Long,
    )

    private fun fetch(url: String): Fetched {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "nyaclash/0.1")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) error("HTTP $code")
            val body = connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            val info = parseUserInfo(connection.getHeaderField("subscription-userinfo"))
            val name = connection.getHeaderField("content-disposition")
                ?.substringAfter("filename=", "")
                ?.trim('"', ' ')
                ?.takeIf { it.isNotEmpty() }
                ?: connection.getHeaderField("profile-title")
            return Fetched(body, name, info[0], info[1], info[2], info[3])
        } finally {
            connection.disconnect()
        }
    }

    /** `upload=1; download=2; total=3; expire=4` */
    private fun parseUserInfo(header: String?): LongArray {
        val result = longArrayOf(0, 0, 0, 0)
        if (header.isNullOrBlank()) return result
        for (part in header.split(';')) {
            val kv = part.split('=', limit = 2)
            if (kv.size != 2) continue
            val value = kv[1].trim().toLongOrNull() ?: continue
            when (kv[0].trim()) {
                "upload" -> result[0] = value
                "download" -> result[1] = value
                "total" -> result[2] = value
                "expire" -> result[3] = value
            }
        }
        return result
    }

    private fun normalize(content: String): String {
        if (content.contains("proxies:")) return content
        return runCatching {
            val decoded = String(Base64.decode(content.trim(), Base64.DEFAULT), Charsets.UTF_8)
            if (decoded.contains("proxies:")) decoded else content
        }.getOrDefault(content)
    }

    private fun indexJson(): String {
        val array = JSONArray()
        for (profile in profiles) {
            array.put(
                JSONObject()
                    .put("id", profile.id)
                    .put("name", profile.name)
                    .put("url", profile.url)
                    .put("updatedAt", profile.updatedAt)
                    .put("upload", profile.upload)
                    .put("download", profile.download)
                    .put("total", profile.total)
                    .put("expire", profile.expire),
            )
        }
        return array.toString()
    }

    private fun parseIndex(raw: String?): List<Profile> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(
                    Profile(
                        id = o.getString("id"),
                        name = o.optString("name"),
                        url = o.optString("url"),
                        updatedAt = o.optLong("updatedAt"),
                        upload = o.optLong("upload"),
                        download = o.optLong("download"),
                        total = o.optLong("total"),
                        expire = o.optLong("expire"),
                    ),
                )
            }
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
