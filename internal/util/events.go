package util

import (
	log "github.com/sirupsen/logrus"
	"github.com/upsilonproject/upsilon-gocommon/pkg/amqp"
	pb "github.com/upsilonproject/upsilon-gocommon/pkg/amqpproto"
	"fmt"
)

func SendEvent(msg string) {
	log.Error(msg) // Log here, so that callers don't have to log also

	event := &pb.Event {
		Hostname: GetHostname(),
		Content: msg,
	}

	amqp.PublishPb(event)
}

func SendEventErr(msg string, err error) {
	msg = fmt.Sprintf("%v: %v", msg, err)

	SendEvent(msg)
}
