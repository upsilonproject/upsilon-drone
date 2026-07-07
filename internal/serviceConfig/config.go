package serviceConfig

import (
	"os"
	"strconv"

	commonAmqp "github.com/upsilonproject/upsilon-gocommon/pkg/amqp"
	"github.com/spf13/viper"
	log "github.com/sirupsen/logrus"
)

var (
	RuntimeConfig *Config = DefaultConfig()
)

type Config struct {
	AmqpHost string `mapstructure:"amqphost"`
	AmqpUser string `mapstructure:"amqpuser"`
	AmqpPass string `mapstructure:"amqppass"`
	AmqpPort int    `mapstructure:"amqpport"`
	LogLevel string `mapstructure:"loglevel"`
}

func DefaultConfig() *Config {
	cfg := Config{
		AmqpHost: "upsilon",
		AmqpUser: "guest",
		AmqpPass: "guest",
		AmqpPort: 5672,
		LogLevel: "info",
	}

	return &cfg
}

func updateAmqpConfigFromRuntime() {
	commonAmqp.AmqpHost = RuntimeConfig.AmqpHost
	commonAmqp.AmqpPort = RuntimeConfig.AmqpPort
	commonAmqp.AmqpUser = RuntimeConfig.AmqpUser
	commonAmqp.AmqpPass = RuntimeConfig.AmqpPass
}

func applyEnvOverrides() {
	if v := os.Getenv("UPSILON_CONFIG_SYSTEM_AMQPHOST"); v != "" {
		RuntimeConfig.AmqpHost = v
	}
	if v := os.Getenv("UPSILON_AMQPHOST"); v != "" {
		RuntimeConfig.AmqpHost = v
	}
	if v := os.Getenv("UPSILON_CONFIG_SYSTEM_AMQPUSER"); v != "" {
		RuntimeConfig.AmqpUser = v
	}
	if v := os.Getenv("UPSILON_AMQPUSER"); v != "" {
		RuntimeConfig.AmqpUser = v
	}
	if v := os.Getenv("UPSILON_CONFIG_SYSTEM_AMQPPASS"); v != "" {
		RuntimeConfig.AmqpPass = v
	}
	if v := os.Getenv("UPSILON_AMQPPASS"); v != "" {
		RuntimeConfig.AmqpPass = v
	}
	if v := os.Getenv("UPSILON_CONFIG_SYSTEM_AMQPPORT"); v != "" {
		if port, err := strconv.Atoi(v); err == nil {
			RuntimeConfig.AmqpPort = port
		}
	}
	if v := os.Getenv("UPSILON_AMQPPORT"); v != "" {
		if port, err := strconv.Atoi(v); err == nil {
			RuntimeConfig.AmqpPort = port
		}
	}
	if v := os.Getenv("UPSILON_LOGLEVEL"); v != "" {
		RuntimeConfig.LogLevel = v
	}
}

func Refresh() {
	RuntimeConfig = DefaultConfig()
	updateAmqpConfigFromRuntime()

	if err := viper.ReadInConfig(); err != nil {
		log.Warn(err)
	} else if err := viper.UnmarshalExact(&RuntimeConfig); err != nil {
		log.Warn(err)
	} else {
		log.WithFields(log.Fields{
			"cfgfile": viper.ConfigFileUsed(),
		}).Debugf("Loaded service config")
	}

	applyEnvOverrides()
	updateAmqpConfigFromRuntime()

	lvl, err := log.ParseLevel(RuntimeConfig.LogLevel)
	if err != nil {
		log.Warnf("Invalid log level %q: %v", RuntimeConfig.LogLevel, err)
	} else {
		log.SetLevel(lvl)
	}
}

func init() {
	viper.SetConfigType("yaml")
	viper.AddConfigPath("/etc/upsilon-drone/")
	viper.SetConfigName("upsilon-drone")
	viper.AutomaticEnv()
}
