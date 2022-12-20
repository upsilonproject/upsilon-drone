package util

import (
	log "github.com/sirupsen/logrus"
	"github.com/upsilonproject/upsilon-drone/internal/easyexec"
	"os"
	"path"
)

func GitPull(gitUrl string, localDir string) {
	repoName := path.Base(gitUrl)

	log.Infof("GitPull: %v %v %v", gitUrl, localDir, repoName)

	if _, err := os.Stat(localDir); os.IsNotExist(err) {
		os.Mkdir(localDir, 0755)
	}

	if _, err := os.Stat(localDir + "/" + repoName); os.IsNotExist(err) {
		err = os.Chdir(localDir)

		if err != nil {
			log.Errorf("%v", err)
		}

		stdout, _, runerr, _ := easyexec.ExecLog("git", []string { "clone", gitUrl})

		if runerr != nil {
			SendEvent("gitclone: " + stdout)
		}
	} else {
		err = os.Chdir(localDir + "/" + repoName)

		if err != nil {
			log.Errorf("%v", err)
		}

		stdout, _, runerr, _ := easyexec.ExecLog("git", []string { "pull"})

		if runerr != nil {
			SendEvent("gitpull: " + stdout)
		} else {
			SendEvent("gitpull: clean")
		}
	}
}

