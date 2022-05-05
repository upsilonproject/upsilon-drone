package updater

import (
	log "github.com/sirupsen/logrus"
	"io"
	"net/http"
	"os"
	"os/exec"
	"path"
	"path/filepath"
	"strconv"
	"strings"
	"time"
	"errors"

	"github.com/upsilonproject/upsilon-drone/internal/buildconstants"
	"github.com/go-co-op/gocron"
)

const DRONE_PATH = "/usr/local/sbin/upsilon-drone"
const DRONE_PATH_UPDATE = "/usr/local/sbin/upsilon-drone.update"

func writeSystemdUnit() {
	const UNIT_FILE_PATH = "/etc/systemd/system/upsilon-drone.service"

	f, err := os.OpenFile(UNIT_FILE_PATH, os.O_RDWR | os.O_CREATE, 0644)

	defer f.Close()

	if err != nil {
		log.Errorf("Open systemd unit error: %s", err.Error())
		return
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

		f.Truncate(0)
		f.Seek(0, 0)

		_, err := f.Write([]byte(approvedContent))

		if err != nil {
			log.Errorf("Error writing unit file: %v", err)
		}

		// Enable should do a daemon-reload
		cmd := exec.Command("systemctl", "enable", "--now", "upsilon-drone")
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

	out, err := os.Create(DRONE_PATH_UPDATE)

	if err != nil {
		log.Errorf("%v", err)
		return
	}

	io.Copy(out, resp.Body)

	defer out.Close()

	tryChmod(DRONE_PATH_UPDATE)

	writeSystemdUnit()

	log.Info("Update downloaded")
}

func getUpdatedTimestamp() int {
	url := "http://upsilon/upsilon-drone.timestamp"

	resp, err := http.Get(url)

	if err != nil {
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
	log.Info("Copying " + in + " to " + out)

	i, e := os.Open(in)
	if e != nil {
		return 0, e
	}
	defer i.Close()
	o, e := os.Create(out)
	if e != nil {
		return 0, e
	}
	defer o.Close()

	return o.ReadFrom(i)
}

func tryChmod(path string) {
	err := os.Chmod(path, 0777)

	if err != nil {
		log.Warnf("Chmod error: %v", err)
		return
	}
}

func Update() {
	if _, err := os.Stat(DRONE_PATH); errors.Is(err, os.ErrNotExist) {
		log.Infof("Downloading update due to not existing locally")
		downloadUpdate()
		log.Fatalf("Exiting due to update")
	}

	currentTimestamp, _ := strconv.Atoi(buildconstants.Timestamp)
	updatedTimestamp := getUpdatedTimestamp()

	log.WithFields(log.Fields{
		"current": currentTimestamp,
		"updated": updatedTimestamp,
	}).Infof("Version comparison")

	if updatedTimestamp > currentTimestamp {
		log.Infof("Downloading update due to newer timestamp")
		downloadUpdate()
		log.Fatalf("Exiting due to update")
	} else {
		log.Infof("No update required")
	}
}

func StartCron() {
	s := gocron.NewScheduler(time.UTC)
	s.Every(15).Minutes().Do(func() {
		Update()
	})
	s.StartAsync()
}
