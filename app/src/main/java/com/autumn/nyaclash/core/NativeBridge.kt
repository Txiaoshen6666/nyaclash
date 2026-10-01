package com.autumn.nyaclash.core

/**
 * Callbacks mihomo invokes from native code.
 *
 * Implemented by the VPN service so mihomo can protect its outbound sockets and
 * resolve the source UID of TUN connections.
 */
interface TunInterface {
    /** Must call `VpnService.protect(fd)`. */
    fun markSocket(fd: Int)

    /** Returns the UID owning the connection, or -1 if unknown. */
    fun querySocketUid(protocol: Int, source: String, target: String): Int
}

/**
 * Thin JNI facade over the bundled mihomo core (`libclash.so`).
 *
 * The library is produced by CI (see `.github/workflows/ci.yml`) and packaged
 * from `app/src/main/jniLibs/arm64-v8a/`. When it is absent (e.g. a local build
 * without the native step) [available] is `false` and the native methods must
 * not be called.
 */
object NativeBridge {

    /** True when `libclash.so` exists and was loaded successfully. */
    val available: Boolean = try {
        System.loadLibrary("clash")
        true
    } catch (_: Throwable) {
        false
    }

    @Volatile
    private var initialized = false

    /**
     * Configures the core exactly once per process.
     *
     * Re-initialising would reset the tunnel, so callers must use this instead
     * of [nativeInit] directly.
     */
    @Synchronized
    fun ensureInit(homeDir: String, versionName: String, gitVersion: String, sdkVersion: Int) {
        if (!available || initialized) return
        nativeInit(homeDir, versionName, gitVersion, sdkVersion)
        initialized = true
    }

    /** Bundled mihomo version string. */
    external fun nativeVersion(): String

    /** Configures the core and sets its working directory. Prefer [ensureInit]. */
    external fun nativeInit(homeDir: String, versionName: String, gitVersion: String, sdkVersion: Int)

    /** Tears down the tunnel and reloads defaults. */
    external fun nativeReset()

    /** Requests a non-blocking garbage collection. */
    external fun nativeForceGc()

    /** Loads a mihomo YAML config. Returns null on success, else an error message. */
    external fun nativeLoadConfig(path: String): String?

    /** Starts TUN on [fd]. Returns 0 on success, -1 on failure. */
    external fun nativeStartTun(
        fd: Int,
        stack: String,
        gateway: String,
        portal: String,
        dns: String,
        callback: TunInterface,
    ): Int

    /** Stops the TUN listener. */
    external fun nativeStopTun()
}
