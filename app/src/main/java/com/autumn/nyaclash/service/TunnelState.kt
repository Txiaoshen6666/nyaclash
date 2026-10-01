package com.autumn.nyaclash.service

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Process-wide tunnel state observed by the UI. */
object TunnelState {
    var running by mutableStateOf(false)
    var status by mutableStateOf("Disconnected")
}
