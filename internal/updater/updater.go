package updater

import (
	log "github.com/sirupsen/logrus"
	"io"
	"net/http"
	"os"
	"path"
	"strconv"
	"strings"

	"github.com/upsilonproject/upsilon-drone/internal/buildconstants"
)

const DRONE_PATH = "/usr/local/sbin/upsilon-drone"

func writeSystemdUnit() {
	f, err := os.Create("/etc/systemd/system/upsilon-drone.service")

	if err != nil {
		log.Errorf("Write systemd unit error: %s", err.Error())
		return
	}

	unit := `
[Unit]
Description=upsilon-drone

[Service]
ExecStart=/usr/local/bin/upsilon-drone
Restart=always

[Install]
WantedBy=multi-user.target
	`
	f.Write([]byte(unit))

}

func getCurrentBinary() string {
	ex := os.Args[0]
	dir, _ := os.Getwd()

	fullPath := path.Join(dir, ex)

	log.Infof("Current binary: %v ", fullPath)

	return fullPath
}

func downloadUpdate() {
	log.Infof("Downloading update")

	url := "http://upsilon/upsilon-drone"

	resp, err := http.Get(url)

	if err != nil {
		log.Errorf("%v", err)
		return
	}

	if resp.StatusCode != 200 {
		log.Errorf("Updater HTTP: %v", resp.StatusCode)
		return
	}

	defer resp.Body.Close()

	out, err := os.Create("/tmp/upsilon-drone")

	if err != nil {
		log.Errorf("%v", err)
		return
	}

	defer out.Close()

	io.Copy(out, resp.Body)

	if err != nil {
		log.Errorf("%v", err)
		return
	}

	err = os.Chmod(DRONE_PATH, 0777)

	if err != nil {
		log.Errorf("%v", err)
		return
	}

	log.Info("Update complete")
}

func getUpdatedTimestamp() int {
	url := "http://upsilon/upsilon-drone.timestamp"

	resp, err := http.Get(url)

	if err  != nil {
		log.Errorf("%v", err)
		return -1
	}

	if resp.StatusCode != 200 {
		log.Errorf("timestamp http status code %v", resp.StatusCode)
		return -1
	}

	body, _ := io.ReadAll(resp.Body)

	updateTimestamp, err := strconv.Atoi(strings.Trim(string(body), "\n"))

	if err != nil {
		log.Errorf("%v", err)
		return -1
	}
	
	log.Infof("body %v", updateTimestamp)

	return updateTimestamp
}

func Update() {
	writeSystemdUnit()

	currentTimestamp, _ := strconv.Atoi(buildconstants.Timestamp)
	updatedTimestamp := getUpdatedTimestamp()

	log.WithFields(log.Fields{
		"current": currentTimestamp,
		"updated": updatedTimestamp,
	}).Infof("Version comparison")

	if updatedTimestamp > currentTimestamp {
		log.Infof("Downloading Update")
		downloadUpdate()
		log.Fatalf("Exiting due to update")
	} else {
		log.Infof("No update required")
	}
}
