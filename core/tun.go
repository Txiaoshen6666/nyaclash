package main

import (
	"encoding/json"
	"io"
	"net"
	"net/netip"
	"strings"
	"sync"

	C "github.com/metacubex/mihomo/constant"
	LC "github.com/metacubex/mihomo/listener/config"
	"github.com/metacubex/mihomo/listener/sing_tun"
	"github.com/metacubex/mihomo/log"
	"github.com/metacubex/mihomo/tunnel"
)

var (
	tunLock   sync.Mutex
	tunCloser io.Closer
)

// startTunInternal builds and starts the TUN listener on the file descriptor
// owned by Android's VpnService.
//
//	gateway: "172.19.0.1/30,fdfe:dcba:9876::1/126" (comma separated)
//	dns:     "172.19.0.2"                          (comma separated)
func startTunInternal(fd int, stack, gateway, portal, dns string) error {
	tunLock.Lock()
	defer tunLock.Unlock()

	stopTunInternal()

	tunStack, ok := C.StackTypeMapping[strings.ToLower(stack)]
	if !ok {
		tunStack = C.TunSystem
	}

	var prefix4, prefix6 []netip.Prefix
	for _, raw := range strings.Split(gateway, ",") {
		raw = strings.TrimSpace(raw)
		if raw == "" {
			continue
		}
		prefix, err := netip.ParsePrefix(raw)
		if err != nil {
			log.Errorln("[core] tun gateway %q: %s", raw, err.Error())
			return err
		}
		if prefix.Addr().Is4() {
			prefix4 = append(prefix4, prefix)
		} else {
			prefix6 = append(prefix6, prefix)
		}
	}

	var dnsHijack []string
	for _, raw := range strings.Split(dns, ",") {
		raw = strings.TrimSpace(raw)
		if raw == "" {
			continue
		}
		dnsHijack = append(dnsHijack, net.JoinHostPort(raw, "53"))
	}

	options := LC.Tun{
		Enable:              true,
		Device:              sing_tun.InterfaceName,
		Stack:               tunStack,
		DNSHijack:           dnsHijack,
		AutoRoute:           false, // routes are set up by TunService on the Android side
		AutoDetectInterface: false, // handled by VpnService.protect via jni_protect
		Inet4Address:        prefix4,
		Inet6Address:        prefix6,
		MTU:                 9000,
		FileDescriptor:      fd,
	}

	if encoded, err := json.Marshal(options); err == nil {
		log.Debugln("[core] tun options: %s", string(encoded))
	}

	listener, err := sing_tun.New(options, tunnel.Tunnel)
	if err != nil {
		log.Errorln("[core] tun error: %s", err.Error())
		return err
	}

	tunCloser = listener

	log.Infoln("[core] tun started: fd=%d stack=%s", fd, stack)

	return nil
}

func stopTunInternal() {
	if tunCloser != nil {
		_ = tunCloser.Close()
		tunCloser = nil
	}
}
