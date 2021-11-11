package amqp

import (
	log "github.com/sirupsen/logrus"
	"github.com/streadway/amqp"
	"github.com/upsilonproject/upsilon-drone/internal/buildconstants"
)

var (
	conn    *amqp.Connection
	channel *amqp.Channel
)

func GetChannel() (*amqp.Channel, error) {
	var err error

	if channel == nil {
		cfg := amqp.Config {
			Properties: amqp.Table {
				"connection_name": "upsilon-drone " + buildconstants.Timestamp,
			},
		}

		conn, err = amqp.DialConfig("amqp://guest:guest@upsilon.teratan.net:5672", cfg)

		if err != nil {
			return nil, err
		}

		//defer conn.Close()

		if err != nil {
			log.Warnf("Could not get chan: %s", err)
		}

		channel, err = conn.Channel()
	}

	return channel, err
}

func StartServerListener() {
	//c, err := GetChannel()

	log.Info("Started listening")
}

