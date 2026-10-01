# nyaclash

An Android client for the [mihomo](https://github.com/MetaCubeX/mihomo) (Clash Meta) proxy core,
with a Kotlin + Jetpack Compose **Material 3 Expressive** UI.

- Package: `com.autumn.nyaclash`
- License: **GPL-3.0** (required because the app links against the GPL-3.0 mihomo core)
- Target ABI: `arm64-v8a` only
- minSdk 24 / target & compile SDK 36

## Status

Milestone 1 — UI skeleton + CI. See [`agent.md`](agent.md) for the full plan, decisions and
milestone roadmap.

## Building

Nothing is built locally; **GitHub Actions builds, packages and signs the app**.

- `.github/workflows/ci.yml` — builds a debug APK on every push / PR and uploads it as an artifact.
- `.github/workflows/release.yml` — (later milestone) builds a signed release APK from repository
  secrets and publishes a GitHub Release.

## Architecture (planned)

```
:app   Compose M3 Expressive UI + VpnService (foreground service)
          └─ dynamic data via mihomo external-controller REST/WS on 127.0.0.1
:core  Kotlin facade over a single libclash.so
          = Go (mihomo) c-shared + JNI bridge in the same shared object
```

The mihomo core is retrieved by pinned Go module version and built for Android in CI. See
`agent.md` for details and the pinned versions.
