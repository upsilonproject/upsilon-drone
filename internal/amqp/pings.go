package amqp

import (
	log "github.com/sirupsen/logrus"
	"github.com/upsilonproject/upsilon-gocommon/pkg/amqp"
	pb "github.com/upsilonproject/upsilon-gocommon/pkg/amqpproto"
	"github.com/upsilonproject/upsilon-drone/internal/util"
)

func ListenForPings() {
	amqp.ConsumeForever("PingRequest", func(d amqp.Delivery) {
		//hb := &pb.PingRequest{}
		//log.Infof("%v", hb)
		d.Message.Ack(true)
		
		log.Infof("Responding to ping")

		res := &pb.PingResponse{}
		res.Hostname = util.GetHostname()

		amqp.PublishPb(res)
	})
}
