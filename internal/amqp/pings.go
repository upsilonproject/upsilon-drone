package amqp

import (
	log "github.com/sirupsen/logrus"
	"github.com/upsilonproject/upsilon-gocommon/pkg/amqp"
	pb "github.com/upsilonproject/upsilon-drone/gen/amqpproto"
)

func ListenForPings() {
	c, err := amqp.GetChannel()

	if err != nil {
		log.Warnf("%v", err);
		return
	}

	log.Infof("Listening for ping")

	amqp.Consume(c, "PingRequest", func(d amqp.Delivery) {
		//hb := &pb.PingRequest{}
		//log.Infof("%v", hb)
		d.Message.Ack(true)
		
		log.Infof("Responding to ping")

		res := &pb.PingResponse{}
		res.Hostname = getHostname()

		amqp.PublishPb(c, res)
	})

	log.Infof("Finished listening")
}
