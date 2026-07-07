package util

import (
	"os"
)

func GetHostname() string {
	hostname, err := os.Hostname()

	if err != nil {
		return "unknown"
	}

	return hostname
}

// GetIdentifier returns the node identity used for AMQP routing and fabric
// config host matching. UPSILON_IDENTIFIER overrides the OS hostname.
func GetIdentifier() string {
	if id := os.Getenv("UPSILON_IDENTIFIER"); id != "" {
		return id
	}

	return GetHostname()
}
