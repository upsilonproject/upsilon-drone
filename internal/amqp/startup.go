package amqp

import (
	pb "github.com/upsilonproject/upsilon-gocommon/pkg/amqpproto"

	"github.com/upsilonproject/upsilon-drone/internal/util"
	amqp "github.com/upsilonproject/upsilon-gocommon/pkg/amqp"
)

func SendStartup() {
	startup := &pb.Startup {
		Hostname: util.GetIdentifier(),
	}

	amqp.PublishPb(startup)
}
