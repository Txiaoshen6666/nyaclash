// Package main builds the nyaclash native core (libclash.so).
//
// It is compiled with `go build -buildmode=c-shared -tags with_gvisor` for
// Android/arm64. The JNI entry points live in bridge.c (same package), which
// calls the Go functions exported below via the generated `_cgo_export.h`.
package main

/*
#include <stdlib.h>
*/
import "C"

import (
	"runtime"
	"runtime/debug"

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
	initDelegate(C.GoString(home), C.GoString(versionName), C.GoString(gitVersion), int(sdkVersion))
	loadDefaultConfig()
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
	if err := loadConfig(C.GoString(path)); err != nil {
		return C.CString(err.Error())
	}
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
		return C.CString(err.Error())
	}
	return nil
}

// coreStartTun starts the TUN listener on the given file descriptor.
// Returns 0 on success, -1 on failure.
//
//export coreStartTun
func coreStartTun(fd C.int, stack, gateway, portal, dns *C.char) C.int {
	err := startTunInternal(
		int(fd),
		C.GoString(stack),
		C.GoString(gateway),
		C.GoString(portal),
		C.GoString(dns),
	)
	if err != nil {
		return -1
	}
	return 0
}

// coreStopTun stops the TUN listener.
//
//export coreStopTun
func coreStopTun() {
	stopTunInternal()
}
