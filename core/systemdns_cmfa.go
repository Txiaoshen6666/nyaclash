//go:build android && cmfa

package main

import "github.com/metacubex/mihomo/dns"

// With the `cmfa` tag mihomo does not read the system resolver itself; the app
// has to push the device DNS servers.
func updateSystemDns(addrs []string) {
	dns.UpdateSystemDNS(addrs)
}
