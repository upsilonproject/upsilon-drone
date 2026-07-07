package util

import (
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestGetIdentifierUsesEnvOverride(t *testing.T) {
	t.Setenv("UPSILON_IDENTIFIER", "spot-worker-01")

	assert.Equal(t, "spot-worker-01", GetIdentifier())
}

func TestGetIdentifierFallsBackToHostname(t *testing.T) {
	os.Unsetenv("UPSILON_IDENTIFIER")

	hostname, err := os.Hostname()
	assert.NoError(t, err)
	assert.Equal(t, hostname, GetIdentifier())
}
