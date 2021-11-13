package amqp

import (
	log "github.com/sirupsen/logrus"

	pb "github.com/upsilonproject/upsilon-drone/gen/amqpproto"

	amqp "github.com/upsilonproject/upsilon-gocommon/pkg/amqp"
	
	"os"
	"time"
)

func StartHeartbeater() {
	for {
		heartbeat()
		time.Sleep(10 * time.Second)
	}
}

func heartbeat() {
	c, err := amqp.GetChannel()

	if err != nil {
		log.Warnf("Could not send heartbeat: %s", err)
		return
	}

	amqp.PublishPb(c, newMessageHeartbeat())

	if err != nil {
		log.Warnf("Publish fail:", err)
	}
}

func getHostname() string {
	hostname, err := os.Hostname()

	if err != nil {
		return "unknown"
	}

	return hostname
}

func newMessageHeartbeat() *pb.Heartbeat {
	hb := &pb.Heartbeat{
		UnixTimestamp: time.Now().Unix(),
		Hostname:      getHostname(),
	}

	return hb
}
