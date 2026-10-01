//go:build !(android && cmfa)

package main

func updateSystemDns(addrs []string) {}
