package main

import (
	"os"

	"github.com/metacubex/mihomo/config"
	"github.com/metacubex/mihomo/hub"
	"github.com/metacubex/mihomo/log"
)

// loadConfig parses a mihomo YAML file and applies it.
func loadConfig(path string) error {
	data, err := os.ReadFile(path)
	if err != nil {
		return err
	}

	cfg, err := config.Parse(data)
	if err != nil {
		return err
	}

	hub.ApplyConfig(cfg)

	log.Infoln("[core] config loaded: %s", path)

	return nil
}

// loadDefaultConfig applies mihomo's built-in defaults.
func loadDefaultConfig() {
	cfg, err := config.Parse([]byte{})
	if err != nil {
		log.Errorln("[core] default config: %s", err.Error())
		return
	}

	hub.ApplyConfig(cfg)
}
