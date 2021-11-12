package amqp

import (
	log "github.com/sirupsen/logrus"
	"github.com/streadway/amqp"

	pb "github.com/upsilonproject/upsilon-drone/gen/amqpproto"
	"github.com/upsilonproject/upsilon-drone/internal/config"

	commonAmqp "github.com/upsilonproject/upsilon-gocommon/pkg/amqp"
	
	"github.com/upsilonproject/upsilon-drone/internal/buildconstants"

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
	c, err := commonAmqp.GetChannel("upsilon-drone" + buildconstants.Timestamp + " on " + getHostname())

	if err != nil {
		log.Warnf("Could not send heartbeat: %s", err)
		return
	}

	msg := amqp.Publishing{
		DeliveryMode: amqp.Persistent,
		Timestamp:    time.Now(),
		ContentType:  "text/plain",
		Body:         commonAmqp.EncodeMessage(newMessageHeartbeat()),
	}

	err = c.Publish(
		config.RuntimeConfig.AmqpExchange,
		"heartbeat",
		false, // mandatory
		false, // immediate
		msg,
	)

	log.Infof("Sent heartbeat")

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
