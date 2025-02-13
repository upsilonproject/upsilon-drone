package amqp

import (
	buildconstants "github.com/upsilonproject/upsilon-drone/internal/buildconstants"
	pb "github.com/upsilonproject/upsilon-gocommon/pkg/amqpproto"
	amqp "github.com/upsilonproject/upsilon-gocommon/pkg/amqp"

	"github.com/upsilonproject/upsilon-drone/internal/util"
	"github.com/upsilonproject/upsilon-drone/internal/fabricConfig"
	log "github.com/sirupsen/logrus"

	"time"
	"fmt"
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
		StatusLine: fmt.Sprintf("cfg: %+v", fabricConfig.ConfigStatus),
		Version: buildconstants.Timestamp,
	}

	return hb
}
