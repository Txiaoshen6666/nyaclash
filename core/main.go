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
	"github.com/metacubex/mihomo/constant"
)

func main() {}

// coreVersion returns the bundled mihomo version.
//
//export coreVersion
func coreVersion() *C.char {
	return C.CString(constant.Version)
}

// coreInit sets the working directory used by mihomo (profiles, geo data, ...).
//
//export coreInit
func coreInit(home *C.char) {
	constant.SetHomeDir(C.GoString(home))
}
