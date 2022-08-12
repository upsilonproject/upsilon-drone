package amqp

import (
	pb "github.com/upsilonproject/upsilon-drone/gen/amqpproto"

	"github.com/upsilonproject/upsilon-drone/internal/util"
	amqp "github.com/upsilonproject/upsilon-gocommon/pkg/amqp"
)

func SendStartup() {
	startup := &pb.Startup {
		Hostname: util.GetHostname(),
	}

	amqp.PublishPb(startup)
}
