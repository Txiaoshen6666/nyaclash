package main

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"

	"github.com/metacubex/mihomo/log"
)

const maxLogBytes = 512 * 1024

var (
	logMu   sync.Mutex
	logFile *os.File
)

// initLogging writes mihomo's log stream (and our own app messages) to
// <home>/logs/core.log so failures can be read from inside the app.
func initLogging(home string) {
	dir := filepath.Join(home, "logs")
	if err := os.MkdirAll(dir, 0o700); err != nil {
		return
	}

	path := filepath.Join(dir, "core.log")
	truncateLog(path)

	file, err := os.OpenFile(path, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0o600)
	if err != nil {
		return
	}

	logMu.Lock()
	logFile = file
	logMu.Unlock()

	subscription := log.Subscribe()
	go func() {
		for event := range subscription {
			// DEBUG is far too noisy for the in-app viewer.
			if event.LogLevel == log.DEBUG {
				continue
			}
			writeLog(event.LogLevel.String(), event.Payload)
		}
	}()

	appLog("INFO", "logging started (%s)", path)
}

// truncateLog keeps the file bounded by dropping the oldest half.
func truncateLog(path string) {
	info, err := os.Stat(path)
	if err != nil || info.Size() <= maxLogBytes {
		return
	}
	data, err := os.ReadFile(path)
	if err != nil {
		return
	}
	if len(data) > maxLogBytes/2 {
		data = data[len(data)-maxLogBytes/2:]
		if idx := strings.IndexByte(string(data), '\n'); idx >= 0 {
			data = data[idx+1:]
		}
	}
	_ = os.WriteFile(path, data, 0o600)
}

func writeLog(level, payload string) {
	logMu.Lock()
	defer logMu.Unlock()
	if logFile == nil {
		return
	}
	ts := time.Now().Format("2006-01-02 15:04:05.000")
	_, _ = fmt.Fprintf(logFile, "%s [%s] %s\n", ts, level, payload)
}

// appLog records a message coming from our own code.
func appLog(level, format string, args ...any) {
	writeLog(level, "[app] "+fmt.Sprintf(format, args...))
}
