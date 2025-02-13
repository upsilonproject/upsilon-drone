package amqp

import (
	log "github.com/sirupsen/logrus"
	"github.com/upsilonproject/upsilon-gocommon/pkg/amqp"
	"github.com/upsilonproject/upsilon-drone/internal/fabricConfig"
	"github.com/upsilonproject/upsilon-drone/internal/util"
	pb "github.com/upsilonproject/upsilon-gocommon/pkg/amqpproto"
)

func ListenForExecutionRequests() {
	amqp.ConsumeForever("ExecutionRequest", func(d amqp.Delivery) {
		d.Message.Ack(true)

		execReq := pb.ExecutionRequest{}

		amqp.Decode(d.Message.Body, &execReq)

		if execReq.Hostname == util.GetHostname() {
			log.Infof("Responding to execreq: %v", execReq.CommandName)

			fabricConfig.ExecCommandByName(execReq.CommandName)
		}
	})
}
