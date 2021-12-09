package updater

import (
	log "github.com/sirupsen/logrus"
	"github.com/blang/semver"
	"github.com/rhysd/go-github-selfupdate/selfupdate"
)

func Update() {
	latest1, found, err := selfupdate.DetectLatest("upsilonproject/upsilon-drone")

	log.Infof("%v %v %v", latest1, found, err)

	v := semver.MustParse("1.0.0")

	latest, err := selfupdate.UpdateSelf(v, "upsilonproject/upsilon-drone")

	if err != nil {
		log.Warn(err)
		return
	}

	if latest.Version.Equals(v) {
		log.Info("This version appears to be up to date")
		return
	}

	log.Info("Updated to: %v", latest.Version)

}
