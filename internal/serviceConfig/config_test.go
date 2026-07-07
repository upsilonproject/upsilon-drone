package serviceConfig

import (
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
	commonAmqp "github.com/upsilonproject/upsilon-gocommon/pkg/amqp"
)

func TestApplyEnvOverridesAmqpHost(t *testing.T) {
	RuntimeConfig = DefaultConfig()

	t.Setenv("UPSILON_CONFIG_SYSTEM_AMQPHOST", "amqp.example.com")
	applyEnvOverrides()
	updateAmqpConfigFromRuntime()

	assert.Equal(t, "amqp.example.com", RuntimeConfig.AmqpHost)
	assert.Equal(t, "amqp.example.com", commonAmqp.AmqpHost)

	os.Unsetenv("UPSILON_CONFIG_SYSTEM_AMQPHOST")
}
