package amqp

import (
	log "github.com/sirupsen/logrus"
	"github.com/upsilonproject/upsilon-gocommon/pkg/amqp"
	pb "github.com/upsilonproject/upsilon-drone/gen/amqpproto"
	"github.com/upsilonproject/upsilon-drone/internal/easyexec"
	"github.com/upsilonproject/upsilon-drone/internal/fabricConfig"
	"github.com/upsilonproject/upsilon-drone/internal/util"
	"os"
)

func gitPull(gitUrl string) {
	localDir := "/etc/upsilon-drone-fabric/"

	if _, err := os.Stat(localDir); os.IsNotExist(err) {
		os.Mkdir(localDir, 0755)
	}

	if _, err := os.Stat(localDir + "/upsilon-config/"); os.IsNotExist(err) {
		err = os.Chdir(localDir)

		if err != nil {
			log.Errorf("%v", err)
		}

		repoUrl := "ssh://git@upsilon/opt/upsilon-config.git"

		stdout, _, runerr := easyexec.ExecLog("git", []string { "clone", repoUrl})

		if runerr != nil {
			util.SendEvent("gitclone: " + stdout)
		}
	} else {
		err = os.Chdir(localDir + "/upsilon-config/")

		if err != nil {
			log.Errorf("%v", err)
		}

		stdout, _, runerr := easyexec.ExecLog("git", []string { "pull"})

		if runerr != nil {
			util.SendEvent("gitpull: " + stdout)
		}
	}

	fabricConfig.SetupConfig(localDir + "/upsilon-config/")
}

func ListenForGitPulls() {
	amqp.Consume("GitPullRequest", func(d amqp.Delivery) {
		gp := &pb.GitPullRequest{}

		amqp.Decode(d.Message.Body, &gp)

		log.Infof("Got GitPull: %v", gp)

		go gitPull("ssh://upsilon/opt/upsilon-config/")

		d.Message.Ack(true)
	})
}