package config

var (
	RuntimeConfig *Config = DefaultConfig()
)

type Config struct {
}

func DefaultConfig() *Config {
	cfg := Config{}

	return &cfg
}
