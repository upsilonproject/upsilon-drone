package amqp

import (
	buildconstants "github.com/upsilonproject/upsilon-drone/internal/buildconstants"
	pb "github.com/upsilonproject/upsilon-drone/gen/amqpproto"
	amqp "github.com/upsilonproject/upsilon-gocommon/pkg/amqp"

	"github.com/upsilonproject/upsilon-drone/internal/util"
	log "github.com/sirupsen/logrus"

	"time"
)

func StartHeartbeater() {
	for {
		heartbeat()
		time.Sleep(10 * time.Second)
	}

	log.Warn("The heartbeater has stopped.")
}

func heartbeat() {
	amqp.PublishPb(newMessageHeartbeat())
}

func newMessageHeartbeat() *pb.Heartbeat {
	hb := &pb.Heartbeat{
		UnixTimestamp: time.Now().Unix(),
		Type: "drone",
		Hostname:      util.GetHostname(),
		Version: buildconstants.Timestamp,
	}

	return hb
}
