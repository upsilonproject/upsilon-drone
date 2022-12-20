package serviceConfig

import (
	commonAmqp "github.com/upsilonproject/upsilon-gocommon/pkg/amqp"
	"github.com/spf13/viper"
	log "github.com/sirupsen/logrus"
)

var (
	RuntimeConfig *Config = DefaultConfig()
)

type Config struct {
	AmqpHost string
	AmqpUser string
	AmqpPass string
	AmqpPort int

	LogLevel string
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

func Refresh() {
	updateAmqpConfigFromRuntime(); // that way, if config loading fails, we have safe defaults

	if err := viper.ReadInConfig(); err != nil {
		log.Warn(err)
		return;
	}

	if err := viper.UnmarshalExact(&RuntimeConfig); err != nil {
		log.Warn(err)
		return;
	}

	updateAmqpConfigFromRuntime();

	lvl, _ := log.ParseLevel(RuntimeConfig.LogLevel)

	log.SetLevel(lvl)

	log.WithFields(log.Fields{
		"cfgfile": viper.ConfigFileUsed(),
	}).Debugf("Loaded service config")
}

func init() {
	viper.SetConfigType("yaml")
	viper.AddConfigPath("/etc/upsilon-drone/")
	viper.SetConfigName("upsilon-drone")
	viper.AutomaticEnv() // read in environment variables that match
}
