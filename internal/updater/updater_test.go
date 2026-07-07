package updater

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestVersionUpdateAvailableIntegerTimestamps(t *testing.T) {
	assert.True(t, versionUpdateAvailable("100", "200"))
	assert.False(t, versionUpdateAvailable("200", "100"))
	assert.False(t, versionUpdateAvailable("200", "200"))
}

func TestVersionUpdateAvailableSemverStrings(t *testing.T) {
	assert.True(t, versionUpdateAvailable("3.1.2", "3.1.3"))
	assert.False(t, versionUpdateAvailable("3.1.2", "3.1.2"))
}

func TestVersionUpdateAvailableEmptyRemote(t *testing.T) {
	assert.False(t, versionUpdateAvailable("3.1.2", ""))
}
