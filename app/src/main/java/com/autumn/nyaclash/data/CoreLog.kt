package com.autumn.nyaclash.data

import android.content.Context
import com.autumn.nyaclash.core.NativeBridge
import java.io.File

/**
 * The core log lives at `filesDir/logs/core.log` and is written by the native
 * layer (both mihomo's own log stream and our app messages go through it).
 */
object CoreLog {

    fun file(context: Context): File = File(context.filesDir, "logs/core.log")

    fun info(message: String) = write("INFO", message)

    fun error(message: String) = write("ERROR", message)

    private fun write(level: String, message: String) {
        if (!NativeBridge.available) return
        runCatching { NativeBridge.nativeAppLog(level, message) }
    }

    /** Returns the tail of the log, newest content last, capped at [maxLines]. */
    fun read(context: Context, maxLines: Int = 2000): List<String> {
        val file = file(context)
        if (!file.exists()) return emptyList()
        return runCatching { file.readLines() }
            .getOrDefault(emptyList())
            .takeLast(maxLines)
    }

    fun clear(context: Context) {
        runCatching { file(context).writeText("") }
    }
}
