package com.autumn.nyaclash.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.autumn.nyaclash.BuildConfig
import com.autumn.nyaclash.MainActivity
import com.autumn.nyaclash.R
import com.autumn.nyaclash.core.NativeBridge
import com.autumn.nyaclash.core.TunInterface
import com.autumn.nyaclash.data.ClashApi
import com.autumn.nyaclash.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NyaVpnService : VpnService(), TunInterface {

    companion object {
        const val ACTION_START = "com.autumn.nyaclash.action.START"
        const val ACTION_STOP = "com.autumn.nyaclash.action.STOP"

        private const val CHANNEL_ID = "nyaclash.vpn"
        private const val NOTIFICATION_ID = 1

        private const val TUN_GATEWAY = "172.19.0.1"
        private const val TUN_GATEWAY_PREFIX = 30
        private const val TUN_GATEWAY6 = "fdfe:dcba:9876::1"
        private const val TUN_GATEWAY6_PREFIX = 126
        private const val TUN_DNS = "172.19.0.2"
        private const val TUN_DNS6 = "fdfe:dcba:9876::2"
        private const val TUN_MTU = 9000

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, NyaVpnService::class.java).setAction(ACTION_START),
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, NyaVpnService::class.java).setAction(ACTION_STOP),
            )
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tunnel: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                scope.launch { stopTunnel() }
                stopSelf()
            }

            else -> {
                startForeground(NOTIFICATION_ID, buildNotification("连接中…"))
                TunnelState.status = "连接中…"
                scope.launch { startTunnel() }
            }
        }
        return START_STICKY
    }

    private fun startTunnel() {
        if (tunnel != null) return

        val context = applicationContext
        ProfileStore.ensureLoaded(context)
        SettingsStore.ensureLoaded(context)

        if (ProfileStore.activeConfigFile(context) == null) {
            fail("还没有导入订阅")
            return
        }
        if (!NativeBridge.available) {
            fail("原生内核未打包")
            return
        }

        try {
            NativeBridge.ensureInit(
                homeDir = context.filesDir.absolutePath,
                versionName = BuildConfig.VERSION_NAME,
                gitVersion = "",
                sdkVersion = Build.VERSION.SDK_INT,
            )

            val runtime = RuntimeConfig.prepare(context).getOrElse { error ->
                fail("配置生成失败: ${error.message}")
                return
            }

            NativeBridge.nativeLoadConfig(runtime.absolutePath)?.let { error ->
                fail("配置错误: $error")
                return
            }

            ClashApi.configure(SettingsStore.controllerSecret(context))

            val dnsV4 = SettingsStore.dns.ifBlank { TUN_DNS }

            val builder = Builder()
                .setSession("nyaclash")
                .setMtu(TUN_MTU)
                .addAddress(TUN_GATEWAY, TUN_GATEWAY_PREFIX)
                .addRoute("0.0.0.0", 0)
                .addDnsServer(dnsV4)
                .setBlocking(false)

            // IPv6 is best-effort: some networks/devices do not support it.
            runCatching {
                builder.addAddress(TUN_GATEWAY6, TUN_GATEWAY6_PREFIX)
                builder.addRoute("::", 0)
                builder.addDnsServer(TUN_DNS6)
            }

            // Keep the app's own traffic outside the tunnel so the UI can always
            // download profiles / show logs even if the proxy misbehaves.
            runCatching { builder.addDisallowedApplication(packageName) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            val pfd = builder.establish()
            if (pfd == null) {
                fail("VpnService.establish() failed")
                return
            }

            val rc = NativeBridge.nativeStartTun(
                fd = pfd.detachFd(),
                stack = SettingsStore.tunStack,
                gateway = "$TUN_GATEWAY/$TUN_GATEWAY_PREFIX,$TUN_GATEWAY6/$TUN_GATEWAY6_PREFIX",
                portal = "",
                dns = "$dnsV4,$TUN_DNS6",
                callback = this,
            )
            if (rc != 0) {
                pfd.close()
                fail("启动 TUN 失败 (rc=$rc)")
                return
            }

            tunnel = pfd
            TunnelState.running = true
            TunnelState.status = "已连接"
            updateNotification("已连接")
        } catch (e: Exception) {
            fail(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun stopTunnel() {
        if (NativeBridge.available) {
            runCatching { NativeBridge.nativeStopTun() }
        }
        runCatching { tunnel?.close() }
        tunnel = null
        TunnelState.running = false
        TunnelState.status = "未连接"
    }

    private fun fail(message: String) {
        TunnelState.running = false
        TunnelState.status = message
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    override fun markSocket(fd: Int) {
        protect(fd)
    }

    override fun querySocketUid(protocol: Int, source: String, target: String): Int = -1

    override fun onRevoke() {
        scope.launch { stopTunnel() }
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        runCatching { NativeBridge.nativeStopTun() }
        runCatching { tunnel?.close() }
        tunnel = null
        TunnelState.running = false
        if (TunnelState.status == "已连接") TunnelState.status = "未连接"
        scope.cancel()
        super.onDestroy()
    }

    // --- notification ---

    private fun buildNotification(text: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "VPN", NotificationManager.IMPORTANCE_LOW),
            )
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, NyaVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_vpn)
            .setContentTitle("nyaclash")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(0, "断开", stopIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(text))
    }
}
