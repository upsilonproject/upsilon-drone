package amqp

import (
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
	amqp.PublishPb(newMessageHeartbeat())
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
