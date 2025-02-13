package amqp

import (
	log "github.com/sirupsen/logrus"
	"github.com/upsilonproject/upsilon-gocommon/pkg/amqp"
	pb "github.com/upsilonproject/upsilon-gocommon/pkg/amqpproto"
	"github.com/upsilonproject/upsilon-drone/internal/util"
	"github.com/upsilonproject/upsilon-drone/internal/fabricConfig"
)

func updateFabricConfig() {
	util.GitPull("ssh://git@upsilon/opt/upsilon-config/", "/etc/upsilon-drone-fabric/")
	fabricConfig.SetupConfig("/etc/upsilon-drone-fabric/upsilon-config/")
}

func updatePyCommon() {
	util.GitPull("ssh://git@upsilon/opt/upsilon-pycommon/", "/opt/upsilon/")
}

func updateProbes() {
	util.GitPull("ssh://git@upsilon/opt/upsilon-drone-probes/", "/opt/upsilon/")
}

func ListenForGitPulls() {
	amqp.ConsumeForever("GitPullRequest", func(d amqp.Delivery) {
		gp := &pb.GitPullRequest{}

		amqp.Decode(d.Message.Body, &gp)

		log.Infof("Got GitPull: %v", gp)

		switch gp.GitUrlAlias {
		case "pycommon": updatePyCommon()
		case "probes": updateProbes()
		case "fabric-config": updateFabricConfig()
		}

		d.Message.Ack(true)
	})
}
