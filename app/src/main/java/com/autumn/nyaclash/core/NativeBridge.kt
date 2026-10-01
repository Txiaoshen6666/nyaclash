package com.autumn.nyaclash.core

/**
 * Thin JNI facade over the bundled mihomo core (`libclash.so`).
 *
 * The library is produced by CI (see `.github/workflows/ci.yml`) and packaged
 * from `app/src/main/jniLibs/arm64-v8a/`. When it is absent (e.g. a local build
 * without the native step) [available] is `false` and the app must not call the
 * native methods.
 */
object NativeBridge {

    /** True when `libclash.so` exists and was loaded successfully. */
    val available: Boolean = try {
        System.loadLibrary("clash")
        true
    } catch (_: Throwable) {
        false
    }

    /** Bundled mihomo version string. */
    external fun nativeVersion(): String

    /** Sets mihomo's working directory. */
    external fun nativeInit(homeDir: String)
}
