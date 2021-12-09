package config

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
}

func DefaultConfig() *Config {
	cfg := Config{
		AmqpHost: "upsilon",
		AmqpUser: "guest",
		AmqpPass: "guest",
		AmqpPort: 5672,
	}

	return &cfg
}

func Refresh() {
	if err := viper.ReadInConfig(); err == nil {
		log.WithFields(log.Fields{
			"cfgfile": viper.ConfigFileUsed(),
		}).Infof("Using config file")
	} else if err != nil {
		log.Warn(err)
	}

	if err := viper.UnmarshalExact(&RuntimeConfig); err != nil {
		log.Warn(err)
	} else if err == nil {
		// FIXME do this somewhere else
		commonAmqp.AmqpHost = RuntimeConfig.AmqpHost
		commonAmqp.AmqpPort = RuntimeConfig.AmqpPort
		commonAmqp.AmqpUser = RuntimeConfig.AmqpUser
		commonAmqp.AmqpPass = RuntimeConfig.AmqpPass
	}
}

func init() {
	viper.SetConfigType("yaml")
	viper.AddConfigPath("/etc/upsilon-drone/")
	viper.SetConfigName("upsilon-drone")
	viper.AutomaticEnv() // read in environment variables that match
}
