package amqp

import (
	log "github.com/sirupsen/logrus"
	"github.com/upsilonproject/upsilon-gocommon/pkg/amqp"
	"github.com/upsilonproject/upsilon-drone/internal/updater"
)

func ListenForUpdateRequests() {
	amqp.Consume("UpdateRequest", func(d amqp.Delivery) {
		d.Message.Ack(true)
		
		log.Infof("Responding to update request")

		updater.Update()
	})
}
