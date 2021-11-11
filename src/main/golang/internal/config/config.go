package config

var (
	RuntimeConfig *Config = DefaultConfig()
)

type Config struct {
	AmqpExchange string
}

func DefaultConfig() *Config {
	cfg := Config{}
	cfg.AmqpExchange = "ex_upsilon"

	return &cfg
}
