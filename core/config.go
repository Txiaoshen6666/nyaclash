package main

import (
	"os"

	"github.com/metacubex/mihomo/config"
	myaml "github.com/metacubex/mihomo/common/yaml"
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

// prepareConfig writes a runtime copy of the profile with the values the UI
// needs injected (external controller for the REST API, a secret, log level).
//
// The merge is done with mihomo's own YAML implementation so anchors, tags and
// multi-document profiles are preserved.
func prepareConfig(profilePath, outPath, controller, secret string) error {
	data, err := os.ReadFile(profilePath)
	if err != nil {
		return err
	}

	root := map[string]any{}
	if err := myaml.Unmarshal(data, &root); err != nil {
		return err
	}
	if root == nil {
		root = map[string]any{}
	}

	root["external-controller"] = controller
	root["secret"] = secret
	if _, ok := root["log-level"]; !ok {
		root["log-level"] = "info"
	}

	out, err := myaml.Marshal(root)
	if err != nil {
		return err
	}

	return os.WriteFile(outPath, out, 0o600)
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
