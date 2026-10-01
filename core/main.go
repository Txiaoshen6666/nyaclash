// Package main builds the nyaclash native core (libclash.so).
//
// It is compiled with
// `go build -buildmode=c-shared -tags "with_gvisor,cmfa"` for Android/arm64.
// The `cmfa` tag keeps sing-tun from reading the root-only
// /data/system/packages.xml (see listener/sing_tun/server_android.go).
//
// The JNI entry points live in bridge.c (same package), which calls the Go
// functions exported below via the generated `_cgo_export.h`.
package main

/*
#include <stdlib.h>
*/
import "C"

import (
	"runtime"
	"runtime/debug"
	"strings"

	"github.com/metacubex/mihomo/constant"
)

func main() {}

// coreVersion returns the bundled mihomo version.
//
//export coreVersion
func coreVersion() *C.char {
	return C.CString(constant.Version)
}

// coreInit configures the core and applies the default configuration.
//
//export coreInit
func coreInit(home, versionName, gitVersion *C.char, sdkVersion C.int) {
	homeDir := C.GoString(home)
	initLogging(homeDir)
	initDelegate(homeDir, C.GoString(versionName), C.GoString(gitVersion), int(sdkVersion))
	loadDefaultConfig()
}

// coreAppLog lets the Kotlin side write into the same log file.
//
//export coreAppLog
func coreAppLog(level, message *C.char) {
	writeLog(C.GoString(level), "[app] "+C.GoString(message))
}

// coreUpdateSystemDns pushes the device DNS servers (comma separated) so that
// configs using `nameserver: system` keep working.
//
//export coreUpdateSystemDns
func coreUpdateSystemDns(addrs *C.char) {
	raw := C.GoString(addrs)
	if raw == "" {
		updateSystemDns(nil)
		return
	}
	list := strings.Split(raw, ",")
	for i := range list {
		list[i] = strings.TrimSpace(list[i])
	}
	appLog("INFO", "system dns updated: %s", raw)
	updateSystemDns(list)
}

// coreReset tears the tunnel down and reloads the defaults.
//
//export coreReset
func coreReset() {
	stopTunInternal()
	loadDefaultConfig()
	runtime.GC()
	debug.FreeOSMemory()
}

// coreForceGc requests a garbage collection without blocking the caller.
//
//export coreForceGc
func coreForceGc() {
	go func() {
		runtime.GC()
		debug.FreeOSMemory()
	}()
}

// coreLoadConfig loads a mihomo YAML file. It returns NULL on success or a
// newly allocated error string (must be freed by the caller) on failure.
//
//export coreLoadConfig
func coreLoadConfig(path *C.char) *C.char {
	p := C.GoString(path)
	if err := loadConfig(p); err != nil {
		appLog("ERROR", "loadConfig(%s) failed: %s", p, err.Error())
		return C.CString(err.Error())
	}
	appLog("INFO", "config applied: %s", p)
	return nil
}

// corePrepareConfig writes a runtime config with the external controller
// injected. Returns NULL on success or a newly allocated error string.
//
//export corePrepareConfig
func corePrepareConfig(profilePath, outPath, controller, secret *C.char) *C.char {
	err := prepareConfig(
		C.GoString(profilePath),
		C.GoString(outPath),
		C.GoString(controller),
		C.GoString(secret),
	)
	if err != nil {
		appLog("ERROR", "prepareConfig failed: %s", err.Error())
		return C.CString(err.Error())
	}
	return nil
}

// coreStartTun starts the TUN listener on the given file descriptor.
// Returns NULL on success or a newly allocated error string on failure.
//
//export coreStartTun
func coreStartTun(fd C.int, stack, gateway, portal, dns *C.char) *C.char {
	s := C.GoString(stack)

	err := startTunInternal(
		int(fd),
		s,
		C.GoString(gateway),
		C.GoString(portal),
		C.GoString(dns),
	)
	if err != nil {
		appLog("ERROR", "startTun failed (fd=%d stack=%s): %s", int(fd), s, err.Error())
		return C.CString(err.Error())
	}

	appLog("INFO", "tun started (fd=%d stack=%s)", int(fd), s)
	return nil
}

// coreStopTun stops the TUN listener.
//
//export coreStopTun
func coreStopTun() {
	appLog("INFO", "stopping tun")
	stopTunInternal()
}
