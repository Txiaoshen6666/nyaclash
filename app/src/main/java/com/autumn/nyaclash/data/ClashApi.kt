package com.autumn.nyaclash.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class Traffic(val up: Long, val down: Long)

data class ProxyGroup(
    val name: String,
    val type: String,
    val now: String,
    val all: List<String>,
)

/** Minimal client for mihomo's external controller REST/WebSocket API. */
object ClashApi {
    private const val BASE = "http://127.0.0.1:9090"

    @Volatile
    private var secret: String = ""

    private val json = "application/json; charset=utf-8".toMediaType()

    private val http = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val streaming = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    fun configure(secret: String) {
        this.secret = secret
    }

    private fun request(path: String): Request.Builder =
        Request.Builder().url("$BASE$path").apply {
            if (secret.isNotEmpty()) header("Authorization", "Bearer $secret")
        }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    suspend fun mode(): String = withContext(Dispatchers.IO) {
        http.newCall(request("/configs").get().build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("HTTP ${response.code}")
            JSONObject(body).optString("mode", "rule")
        }
    }

    suspend fun setMode(mode: String) {
        withContext(Dispatchers.IO) {
            val payload = JSONObject().put("mode", mode).toString().toRequestBody(json)
            http.newCall(request("/configs").patch(payload).build()).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
            }
        }
    }

    suspend fun groups(): List<ProxyGroup> = withContext(Dispatchers.IO) {
        http.newCall(request("/proxies").get().build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val proxies = JSONObject(body).getJSONObject("proxies")
            buildList {
                for (name in proxies.keys()) {
                    val node = proxies.getJSONObject(name)
                    val all = node.optJSONArray("all") ?: continue
                    val members = buildList { for (i in 0 until all.length()) add(all.getString(i)) }
                    add(ProxyGroup(name, node.optString("type"), node.optString("now"), members))
                }
            }
        }
    }

    suspend fun select(group: String, name: String) {
        withContext(Dispatchers.IO) {
            val payload = JSONObject().put("name", name).toString().toRequestBody(json)
            http.newCall(request("/proxies/${encode(group)}").put(payload).build())
                .execute().use { response ->
                    if (!response.isSuccessful) error("HTTP ${response.code}")
                }
        }
    }

    /** Returns the latency in ms, or -1 on failure. */
    suspend fun delay(name: String): Int = withContext(Dispatchers.IO) {
        val url = "http://www.gstatic.com/generate_204"
        val path = "/proxies/${encode(name)}/delay?timeout=5000&url=${encode(url)}"
        http.newCall(request(path).get().build()).execute().use { response ->
            if (!response.isSuccessful) return@use -1
            val body = response.body?.string().orEmpty()
            runCatching { JSONObject(body).optInt("delay", -1) }.getOrDefault(-1)
        }
    }

    /** Streams `/traffic` (one JSON object per message). */
    fun traffic(): Flow<Traffic> = callbackFlow {
        val socket = streaming.newWebSocket(
            request("/traffic").build(),
            object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    runCatching {
                        val o = JSONObject(text)
                        trySend(Traffic(o.optLong("up"), o.optLong("down")))
                    }
                }
            },
        )
        awaitClose { socket.cancel() }
    }
}
