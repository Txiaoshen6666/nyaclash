package com.autumn.nyaclash.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatBytes(value: Long): String {
    if (value < 1024) return "$value B"
    val units = listOf("KB", "MB", "GB", "TB")
    var v = value.toDouble() / 1024
    var i = 0
    while (v >= 1024 && i < units.size - 1) {
        v /= 1024
        i++
    }
    return String.format(Locale.US, "%.2f %s", v, units[i])
}

fun formatSpeed(value: Long): String = "${formatBytes(value)}/s"

fun formatDate(millis: Long): String {
    if (millis <= 0) return "—"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
}

/** Formats a subscription expiry (unix seconds) as `yyyy-MM-dd`, or 剩余天数. */
fun formatExpire(expireSeconds: Long): String {
    if (expireSeconds <= 0) return "未知"
    val remaining = expireSeconds * 1000 - System.currentTimeMillis()
    if (remaining <= 0) return "已过期"
    val days = remaining / (24 * 60 * 60 * 1000)
    return "$days 天后"
}
