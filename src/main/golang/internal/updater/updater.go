package updater

import (
	log "github.com/sirupsen/logrus"
	"io"
	"net/http"
	"os"
	"path"
	"strconv"
	"strings"
	"errors"
	"path/filepath"
	"os/exec"

	"github.com/upsilonproject/upsilon-drone/internal/buildconstants"
)

const DRONE_PATH = "/usr/local/sbin/upsilon-drone"

func writeSystemdUnit() {
	const UNIT_FILE_PATH = "/etc/systemd/system/upsilon-drone.service"

	f, err := os.Open(UNIT_FILE_PATH)

	defer f.Close()

	if errors.Is(err, os.ErrNotExist) {
		f, err = os.Create(UNIT_FILE_PATH)

		if err != nil {
			log.Errorf("Write systemd unit error: %s", err.Error())
			return
		}
	}

	currentContent, err := io.ReadAll(f)

	if err != nil {
		log.Errorf("%v", err)
		return
	}

	approvedContent := `
[Unit]
Description=upsilon-drone

[Service]
ExecStartPre=-mv /usr/local/sbin/upsilon-drone.update /usr/local/sbin/upsilon-drone
ExecStart=/usr/local/sbin/upsilon-drone
Restart=always

[Install]
WantedBy=multi-user.target
	`

	if string(currentContent) != approvedContent {
		log.Warnf("Updating systemd unit file")

		f.Write([]byte(approvedContent))

		cmd := exec.Command("systemctl", "daemon-reload")
		cmd.Run()
	}
}

func getCurrentBinary() string {
	ex := os.Args[0]
	dir, _ := os.Getwd()
	dir, _ = filepath.Abs(dir)

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

	out, err := os.Create(DRONE_PATH + ".update")

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
	
	tryChmod(DRONE_PATH + ".update")

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
	
	return updateTimestamp
}

func copyFile(in, out string) (int64, error) {
   i, e := os.Open(in)
   if e != nil { return 0, e }
   defer i.Close()
   o, e := os.Create(out)
   if e != nil { return 0, e }
   defer o.Close()

   return o.ReadFrom(i)
}

func Install() {
	currentbin := getCurrentBinary()

	if currentbin != DRONE_PATH {
		log.Warn("I am not in the right location. Copying myself to: " + DRONE_PATH + ".update")

		_, err := copyFile(currentbin, DRONE_PATH + ".update")

		if err != nil {
			log.Errorf("Error copying myself: %v", err)
			return;
		}
	}
	
	tryChmod(DRONE_PATH)
	writeSystemdUnit()
}

func tryChmod(path string) {
	err := os.Chmod(path, 0777)

	if err != nil {
		log.Errorf("%v", err)
		return
	}
}

func Update() {
	currentTimestamp, _ := strconv.Atoi(buildconstants.Timestamp)
	updatedTimestamp := getUpdatedTimestamp()

	log.WithFields(log.Fields{
		"current": currentTimestamp,
		"updated": updatedTimestamp,
	}).Infof("Version comparison")

	if updatedTimestamp > currentTimestamp {
		log.Infof("Downloading Update")
		downloadUpdate()
		Install()
		log.Fatalf("Exiting due to update")
	} else {
		log.Infof("No update required")
	}
}
