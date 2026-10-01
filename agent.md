# nyaclash — 项目须知 / Agent 工作说明

> 本文件是给 AI Agent 的项目约束与需求说明。任何后续开发都必须遵守本文件。

## 1. 项目目标

从零搭建一个 Android Clash 客户端：

- **代理核心**：mihomo（MetaCubeX/mihomo，Go）。
- **UI**：Kotlin + Jetpack Compose Material 3 **Expressive**。
- **不要求本地 Android Studio 编译**：本地只写代码和配置。
- **构建 / 打包 / 签名**：全部交给 GitHub Actions。

## 2. 关键约束

- **本地只写代码与配置，不本地编译**。正确性靠 CI 反馈迭代。
- 本地环境为 Termux / Android 14 / aarch64，缺少 `gh`、`go`、`java`、`gradle`、Android SDK/NDK。
- 本地网络：`api.github.com` 可用；`raw.githubusercontent.com` **不可用**，需用 `ghproxy.net` 反向代理，例如：
  `https://ghproxy.net/https://raw.githubusercontent.com/<owner>/<repo>/<ref>/<path>`。
  遇到网络问题优先使用 `ghproxy.net`。

## 3. 需要事先确认的动作（硬性规矩）

以下动作**必须先向用户确认**，得到明确同意后才可执行；**禁止自动执行 `gh` / `git push`**：

- 创建 GitHub 仓库
- 配置任何 Secrets
- 推送代码（`git push` 等）
- 启用 / 触发 GitHub Actions
- 安装 `gh` 或使用 PAT（Personal Access Token）

## 4. 已锁定的决策

| 项 | 决定 |
|---|---|
| 包名 | `com.autumn.nyaclash` |
| 应用名 | `nyaclash` |
| 仓库名 | `nyaclash` |
| 仓库可见性 | Public |
| 内核集成方式 | Go `-buildmode=c-shared` + JNI |
| 许可证 | GPL-3.0（链接 mihomo 的必然结果） |
| 首版范围 | 最小可用集，用户测试后再跟进完整功能 |
| minSdk | 24 |
| 目标 ABI | 仅 `arm64-v8a` |
| 本地编译 | 不做 |
| 构建/签名 | 全部 GitHub Actions |
| 本地网络代理 | 需要时使用 `ghproxy.net` |

## 5. 技术基线（版本已在 Google Maven / Maven Central 核实存在）

| 组件 | 固定版本 | 说明 |
|---|---|---|
| JDK | 21 (temurin) | CI 内 |
| Gradle | 8.14.5 | AGP 8.13 要求 Gradle ≥ 8.13 |
| Android Gradle Plugin | 8.13.2 | 8.x 最新稳定；避免 AGP 9 的内置 Kotlin 迁移 |
| Kotlin | 2.2.20 | 与 Compose 1.11 同期；支持 AGP 8.13 |
| **material3** | **1.5.0-alpha18** | 见下方「为什么是这个 alpha」 |
| compose-ui | 1.11.4 | minCompileSdk=35 / minAGP=8.6.0 |
| core-ktx | 1.18.0 | 1.19.x 起要求 compileSdk 37 & AGP 9.1 |
| activity-compose | 1.12.4 | minCompileSdk=36 / minAGP=8.9.1 |
| lifecycle | 2.10.0 | 2.11.0 的 `*-compose` 要求 compileSdk 37 & AGP 9.1；2.10.0 是 minCompileSdk=35 的上限 |
| kotlinx-coroutines | 1.11.0 | |
| compileSdk / targetSdk | 36 | |
| minSdk | 24 | |
| Go | 1.23 / 1.24 | 里程碑 2 使用 |
| Android NDK | r27 / r28 | 里程碑 2 使用 |
| mihomo | **v1.19.32** | 固定 release tag / Go module 版本 |
| 构建标签 | `with_gvisor` | |
| 交叉编译 CC | `aarch64-linux-android24-clang` | 与 minSdk 24 对应 |

### 为什么用 material3 `1.5.0-alpha18`（重要）

- Material 3 **Expressive** 的公开 API（`MaterialExpressiveTheme` / `MotionScheme`）**不在 1.4.0 stable 里**
  （1.4.0 中它们是 `internal`），而在 **1.5.0-alpha** 线里是 public。
- 1.5.0-alpha 的 AAR 元数据分界线：
  - `alpha01`–`alpha18`：`minCompileSdk=35`、`minAndroidGradlePluginVersion=8.6.0` → **可用 AGP 8.13 + compileSdk 36**
  - `alpha19` 起：`minCompileSdk=37`、`minAndroidGradlePluginVersion=9.1.0` → 需要 AGP 9 + compileSdk 37
- 因此采用 **alpha18**，并配 compose-ui **1.11.4**（alpha18 要求 ui ≥ 1.11.0-beta02）。
  这样可继续使用稳定、文档完善的 AGP 8.x DSL，而不必迁移到 AGP 9 的内置 Kotlin。

> 升级路径：若将来想上 material3 1.5.0-alpha19+，需同时把 AGP 升到 ≥9.1、Gradle ≥9.3.1、
> compileSdk 升到 37，并处理 AGP 9 的内置 Kotlin（移除 `org.jetbrains.kotlin.android`）。

> ⚠️ 重要：`github.com/MetaCubeX/mihomo` 的默认分支 `main` 是**无关的 Python 项目**（崩铁 API 库），
> 真正的 Go 内核源码在 **`Meta` 分支**。Go module 路径为 `github.com/metacubex/mihomo`。
> 只通过固定 module 版本 / release tag 获取，**绝不从该仓库 `main` 拉源码**。

## 6. 架构

```
:app   Compose M3 Expressive UI + VpnService(前台服务)
          └── UI 动态数据 → 127.0.0.1:9090 (mihomo external-controller REST/WS)
:core  Kotlin 门面 Core.kt
          └── 单个 libclash.so
                = Go(mihomo v1.19.32) c-shared
                + 同包内 JNI .c（JNI_OnLoad / startTun / stopTun / reset / forceGc）
                + 回调：dialer.DefaultSocketHook → VpnService.protect(fd)
                        process.DefaultPackageNameResolver → querySocketUid
```

### 核心要点

- **JNI 面缩到最小**：只暴露 `coreInit` / `startTun(fd, stack, gateway, portal, dns)` /
  `stopTun` / `reset` / `forceGc`。
- **动态数据全部走 mihomo 自带的 REST API**（`external-controller`）：
  - 节点列表/切换/延迟：`GET /proxies`、`PUT /proxies/{group}`、`GET /proxies/{name}/delay`
  - 流量：`GET /traffic`（WebSocket/流式）
  - 日志：`GET /logs`；连接：`/connections`；配置热重载：`/configs`
- **两个必需的 Go→Kotlin 回调**（Android TUN 的命门）：
  - `dialer.DefaultSocketHook` → 调 `VpnService.protect(fd)`，防止出站 socket 环回。
  - `process.DefaultPackageNameResolver` → 解析来源 UID / 包名（分应用代理、`include-package` 用）。
- **单 so 方案**：把 JNI 的 `.c` 直接放进 Go 包目录（cgo 会一起编译），只产出一个 `libclash.so`，
  **不需要单独的 CMake 工程**，也不需要第二个 `libbridge.so`。CI 把产物放进
  `core/src/main/jniLibs/arm64-v8a/`。
- Go 侧已核实的 mihomo API：`constant.SetHomeDir`、`config.UnmarshalRawConfig` /
  `config.ParseRawConfig`、`hub.ApplyConfig`、
  `sing_tun.New(listenerconfig.Tun{FileDescriptor: fd, ...}, tunnel.Tunnel)`、
  `dialer.DefaultSocketHook`、`process.DefaultPackageNameResolver`。

## 7. 实际目录结构

> 与最初的 `:core` 模块方案相比，简化为**单一 `:app` 模块**：Go/JNI 源码放在顶层
> `core/`，CI 编出的 `libclash.so` 直接落到 `app/src/main/jniLibs/arm64-v8a/`
> （该目录不入库，见 `.gitignore`）。这样避免引入 CMake / 自定义 Gradle 插件，
> 首次跑通更稳。

```
nyaclash/
├── .github/workflows/
│   └── ci.yml          # Go+NDK 编 libclash.so → 编 Debug APK → 上传 artifact
│                       # (release.yml 在里程碑 6 添加)
├── gradle/ (wrapper + libs.versions.toml)
├── gradlew, settings.gradle.kts, build.gradle.kts, gradle.properties
├── app/                # 唯一模块：Compose UI + (后续) VpnService + 数据层
│   └── src/main/{java,res,AndroidManifest.xml,jniLibs}
│       └── java/com/autumn/nyaclash/
│           ├── MainActivity.kt
│           ├── core/NativeBridge.kt     # JNI 门面 (System.loadLibrary("clash"))
│           └── ui/{home,theme}
├── core/               # 原生内核（Go module，非 Gradle 模块）
│   ├── go.mod          # module nyaclash/core, require mihomo v1.19.32
│   ├── main.go         # package main, cgo //export coreVersion/coreInit...
│   └── bridge.c        # JNI_OnLoad + Java_..._nativeXxx（与 Go 同编进 libclash.so）
├── README.md
├── LICENSE (GPL-3.0)
├── NOTICE
└── .gitignore
```

### 单 so 方案

JNI 的 `bridge.c` 与 Go 源码在**同一个 package 目录**，由 cgo 一起编译，
因此只产出一个 `libclash.so`：

- Kotlin `System.loadLibrary("clash")` 直接加载；
- `bridge.c` 里 `JNIEXPORT` 的函数自动导出，JNI 按名查找
  `Java_com_autumn_nyaclash_core_NativeBridge_nativeXxx`；
- `bridge.c` 通过 cgo 生成的 `_cgo_export.h` 调用 Go 的 `//export` 函数；
- Go → Kotlin 的回调（后续 protect/uid）也将在 `bridge.c` 里用缓存的 JavaVM 实现。

> 注意：`_cgo_export.h` 由 cgo 生成，`bridge.c` 用 `#include "_cgo_export.h"` 引用。

## 8. 首版功能范围（最小可用集）

包含：

- 首页连接开关 + 速度 / 总量
- 订阅导入（URL / 文件 / 剪贴板）
- 节点列表与切换
- 延迟测试
- 模式（规则 / 全局 / 直连）
- 实时日志
- 基础设置（主题 / 动态取色、TUN stack、DNS、allow-lan、系统代理端口）
- 前台通知与快捷开关

暂缓（用户测试后再跟进）：

- 分应用代理完整版
- 规则编辑器
- 内置 Web 面板
- 订阅自动更新
- 开机自启

## 9. GitHub Actions 方案

- `ci.yml`：`ubuntu-latest` → JDK 21(temurin) + setup-gradle + Go + NDK →
  `./gradlew assembleDebug` → 上传 APK artifact。**此流程零 Secrets**，先行跑通。
- `release.yml`：从 Secrets 还原 keystore → `assembleRelease` → `softprops/action-gh-release` 发布。
- Secrets 命名：`KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`。
- 缓存：`~/go/pkg/mod`、`~/.cache/go-build`、Gradle。
- 风险点：CMFA 使用了 Go 工具链补丁（`disable_pidfd_on_android`、
  `remove_64bits_syscall_on_32bit_linux`）和自定义 Go 构建。先用上游 Go 试；
  编不过再引入补丁。
- Gradle Wrapper：本地无 gradle，计划经 `ghproxy.net` 从 Gradle 官方仓库取对应版本的
  `gradle-wrapper.jar` 提交入库。

## 10. 实施里程碑（每步都能独立出 CI 结果）

1. **骨架 + CI**：Gradle 工程、Compose 空页面（M3 Expressive 主题）、`ci.yml` 编出 Debug APK artifact。零 Secrets。
2. **Go 内核**：`core` 包装 + JNI `.c` + `libclash.so` 交叉编译，CI 产出产物并打进 APK。
3. **VPN 打通**：`VpnService` + `startTun(fd)` + protect 回调，真机可代理。
4. **数据层**：REST/WebSocket 客户端（proxies/traffic/logs/configs）。
5. **UI**：首页 / 节点 / 订阅 / 日志 / 设置。
6. **发布**：签名 Secrets + `release.yml`。

## 11. 当前进度

- [x] 环境勘查与技术调研
- [x] 方案定稿、决策锁定
- [x] 创建项目目录 `nyaclash/` 并写入本文件
- [x] 里程碑 1：骨架 + CI ✅ **CI 全绿**
      - run: https://github.com/Txiaoshen6666/nyaclash/actions/runs/36814971974
      - 产物：`nyaclash-debug`（约 11.8 MB，debug keystore 签名）
      - Gradle 工程（settings/build/gradle.properties/version catalog/wrapper 8.14.5）
      - `:app` Compose Material 3 Expressive 骨架（主题 + 首页）
      - `.github/workflows/ci.yml`（编 Debug APK 并上传 artifact，零 Secrets）
      - `LICENSE`(GPL-3.0) / `NOTICE` / `README.md` / `.gitignore`
- [x] 里程碑 2：Go 内核（`libclash.so` + JNI）✅ **CI 全绿**
      - 2a 工具链冒烟：Go module + `coreVersion`/`coreInit` + JNI 桥 + CI 交叉编译 + 加载
      - 2b 完整包装：`dialer.DefaultSocketHook`(protect)、配置加载、`startTun(fd)`/`stopTun`
- [x] 里程碑 3：VPN 打通 ✅ **真机测试通过（能连接）**
      - `NyaVpnService`（VpnService + 前台通知 + `TunInterface` 回调）
      - VPN 参数：172.19.0.1/30、fdfe:dcba:9876::1/126、DNS、MTU 9000
      - 用户实测：导入订阅 → Connect → 可正常代理
- [ ] 里程碑 4：数据层（REST/WebSocket）— 与 5 一起提交，待 CI
      - Go 新增 `corePrepareConfig`（用 mihomo `common/yaml` 注入 `external-controller` + `secret`）
      - `RuntimeConfig` 生成 `runtime.yaml`；`ClashApi`（OkHttp）访问 `/traffic` `/proxies` `/configs`
      - `SettingsStore`（动态取色 / 深色模式 / TUN 栈 / DNS）
- [ ] 里程碑 5：UI（仪表板 / 节点 / 订阅 / 设置）+ 淡入淡出动画
      - `NyaClashApp`：Scaffold + NavigationBar + `AnimatedContent`(fadeIn/fadeOut)
      - **节点页只在连接后出现**（`tabs` 依赖 `TunnelState.running`，按名称选中避免索引错位）
      - 节点页：测速(延迟)/连通性(独立 URL)、排序(默认/名称/延迟)、显示方式(列表/网格)
      - 仪表板：连接开关、实时上下行、模式切换（节点已移出）
      - 订阅：多订阅列表、添加/切换/更新/删除、流量与到期
      - 设置：外观 / 网络 / 关于
      - 说明：**不引入** navigation-compose 与 YAML 库；节点图标为自绘 ImageVector
        （避免引入体积巨大的 material-icons-extended）
### 排错：启动 TUN 失败 rc=-1（未 root）

原因：未加 `cmfa` 标签时，`sing_tun.New()` 会走 `listener/sing_tun/server_android.go`
的 `buildAndroidRules()`，其中 `getPackageManager()` 读取 **root-only** 的
`/data/system/packages.xml`，普通 App 读失败 → `sing_tun.New` 报错 → `rc=-1`。

修复与配套：
- 构建标签改为 `with_gvisor,cmfa`（cmfa 标签让该路径变成空操作）
- `cmfa` 会启用 embed 模式并禁用 `PATCH /configs` → 用 `route.SetEmbedMode(false)` 恢复
- `cmfa` 下 mihomo 不再自读系统 DNS → 连接时由 App 调 `coreUpdateSystemDns`
- `coreStartTun` 改为返回**错误字符串**；新增 `coreAppLog` + 「日志」页（`filesDir/logs/core.log`）

- [x] 里程碑 6：发布（签名 Secrets + `release.yml`）✅ **v0.1.0 已发布**
      - Release: https://github.com/Txiaoshen6666/nyaclash/releases/tag/v0.1.0
      - 资源：`app-release.apk`（约 84 MB，PKCS12 签名）
      - 签名 keystore：`~/nyaclash-signing/release.p12`（PKCS12，alias `nyaclash`）——**仓库外，务必备份**
      - Secrets：`KEYSTORE_BASE64` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`
      - `.github/workflows/release.yml`：推 `v*` tag 或手动 → 编签名 Release APK → 发布 GitHub Release
      - 注意：release 包名 `com.autumn.nyaclash`（debug 是 `com.autumn.nyaclash.debug`），二者可共存

### 本地 / GitHub 环境（已就绪）

- GitHub 账户：`Txiaoshen6666`
- 仓库：https://github.com/Txiaoshen6666/nyaclash （Public）
- git 身份：`Txiaoshen6666 <Txiaoshen6666@users.noreply.github.com>`
- SSH：`~/.ssh/id_ed25519_github`（已加入 GitHub），`~/.ssh/config` 固定 `github.com` 用该密钥
- `gh` 2.102.0 已安装并登录（git protocol = ssh，scopes: gist / read:org / repo）
- remote：`origin = git@github.com:Txiaoshen6666/nyaclash.git`
- 首次提交：`98d4660`（main，已推送）

### 待用户确认后才执行的动作

- [ ] 配置 Secrets（发布里程碑）
- [ ] 启用 / 触发 Actions（已随 push 自动触发）
