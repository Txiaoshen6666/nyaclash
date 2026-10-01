package main

/*
#include "bridge.h"
*/
import "C"

import (
	"fmt"
	"strings"
	"syscall"

	"github.com/metacubex/mihomo/component/dialer"
	"github.com/metacubex/mihomo/constant"
	"github.com/metacubex/mihomo/log"
)

// initDelegate configures mihomo's global hooks for the Android environment.
func initDelegate(home, versionName, gitVersion string, platformVersion int) {
	constant.SetHomeDir(home)

	versions := strings.Split(gitVersion, "_")
	switch {
	case len(versions) == 3:
		constant.Version = fmt.Sprintf("%s-%s-nyaclash-%s",
			strings.ToLower(versions[0]), versions[1], strings.ToLower(versionName))
		constant.BuildTime = versions[2]
	case gitVersion != "":
		constant.Version = gitVersion
	case versionName != "":
		constant.Version = versionName
	}
	constant.Version = strings.ToLower(constant.Version)

	log.Infoln("[core] init: home=%s version=%s buildTime=%s sdk=%d",
		home, constant.Version, constant.BuildTime, platformVersion)

	// Every outbound socket mihomo opens must be protected via
	// VpnService.protect(fd), otherwise it would be routed back into the TUN
	// device and loop forever.
	dialer.DefaultSocketHook = func(network, address string, conn syscall.RawConn) error {
		return conn.Control(func(fd uintptr) {
			C.jni_protect(C.int(fd))
		})
	}
}
