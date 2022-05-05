package amqp

import (
	log "github.com/sirupsen/logrus"
	"github.com/upsilonproject/upsilon-gocommon/pkg/amqp"
	pb "github.com/upsilonproject/upsilon-drone/gen/amqpproto"
)

func ListenForPings() {
	err := amqp.Consume("PingRequest", func(d amqp.Delivery) {
		//hb := &pb.PingRequest{}
		//log.Infof("%v", hb)
		d.Message.Ack(true)
		
		log.Infof("Responding to ping")

		res := &pb.PingResponse{}
		res.Hostname = getHostname()

		amqp.PublishPb(res)
	})

	if err != nil {
		log.Warnf("Could not setup Ping consumer: %v", err)
	}
}
