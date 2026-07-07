package fabricConfig

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"gopkg.in/yaml.v2"
)

func TestUnmarshalMappingsAlias(t *testing.T) {
	raw := []byte(`
commands:
  - name: date
    exec: date
groups:
  - hosts: [all]
    mappings:
      - command: date
        interval: 1
`)

	var cfg FabricConfig
	err := yaml.UnmarshalStrict(raw, &cfg)
	assert.NoError(t, err)

	cfg.normalize()

	assert.Len(t, cfg.Groups, 1)
	assert.Len(t, cfg.Groups[0].Commands, 1)
	assert.Equal(t, "date", cfg.Groups[0].Commands[0].Command)
	assert.Equal(t, 1, cfg.Groups[0].Commands[0].Interval)
}

func TestUnmarshalCommandsField(t *testing.T) {
	raw := []byte(`
commands:
  - name: date
    exec: date
groups:
  - hosts: [worker-1]
    commands:
      - command: date
        interval: 5
`)

	var cfg FabricConfig
	err := yaml.UnmarshalStrict(raw, &cfg)
	assert.NoError(t, err)

	cfg.normalize()

	assert.Len(t, cfg.Groups[0].Commands, 1)
	assert.Equal(t, 5, cfg.Groups[0].Commands[0].Interval)
}
