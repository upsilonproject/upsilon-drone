package amqp

import (
	log "github.com/sirupsen/logrus"
	"time"
	"github.com/streadway/amqp"
)

func StartServerListener() {
	conn, err := amqp.Dial("amqp://guest:guest@upsilon.teratan.net:5672")

	if err != nil {
		log.Warnf("Could not establish initial connection: %s", err)
	}

	defer conn.Close()

	c, err := conn.Channel()

	if err != nil {
		log.Warnf("Could not get chan: %s", err)
	}
	
	msg := amqp.Publishing {
		DeliveryMode: amqp.Persistent,
		Timestamp: time.Now(),
		ContentType: "text/plain",
		Body: newMessageHeartbeat(),
	}

	err = c.Publish("logs", "info", false, false, msg)

	if err != nil {
		log.Warnf("Publish fail:", err)
	}
}

func newMessageHeartbeat() []byte {
	return []byte("Hello world")
}
