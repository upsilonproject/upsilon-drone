package fabricConfig

import (
	"crypto/sha1"
	"io"
	"encoding/hex"
	"gopkg.in/yaml.v2"
	log "github.com/sirupsen/logrus"
	"io/ioutil"
	"github.com/go-co-op/gocron"
	"github.com/upsilonproject/upsilon-drone/internal/easyexec"
	"github.com/upsilonproject/upsilon-drone/internal/util"
	"time"
	"strings"
	"fmt"
	"os"

	pb "github.com/upsilonproject/upsilon-drone/gen/amqpproto"
	amqp "github.com/upsilonproject/upsilon-gocommon/pkg/amqp"
)

var cfg *FabricConfig;
var s *gocron.Scheduler
var ConfigStatus map[string]string

func init() {
	s = gocron.NewScheduler(time.UTC)
	s.Clear()
	s.StartAsync()

	ConfigStatus = make(map[string]string, 0)
}

func UnmarshalConfig(path string) bool {
	log.Infof("Running fabric config: %v", path)

	ConfigStatus[path] = "new"

	yamlFile, err := ioutil.ReadFile(path + "/config.yml")

	if err != nil {
		log.Errorf("Read file error: %v", err)
		ConfigStatus[path] = "fread error"
		return false
	}

	err = yaml.UnmarshalStrict(yamlFile, &cfg)

	if err != nil {
		ConfigStatus[path] = "unmarshal error"
		util.SendEventErr("Unmarshal event error", err)
		return false
	}

	log.Debugf("Got config: %+v", cfg)
	ConfigStatus[path] = fmt.Sprintf("OK %v", hash_file_sha1(path))

	return true
}

func hash_file_sha1(filePath string) (string) {
	//Initialize variable returnMD5String now in case an error has to be returned
	var returnSHA1String string
	
	//Open the filepath passed by the argument and check for any error
	file, err := os.Open(filePath)
	if err != nil {
		return "cant open"
	}
	
	//Tell the program to call the following function when the current function returns
	defer file.Close()
	
	//Open a new SHA1 hash interface to write to
	hash := sha1.New()
	
	//Copy the file in the hash interface and check for any error
	if _, err := io.Copy(hash, file); err != nil {
		return "cant read"
	}
	
	//Get the 20 bytes hash
	hashInBytes := hash.Sum(nil)[:20]
	
	//Convert the bytes to a string
	returnSHA1String = hex.EncodeToString(hashInBytes)
	
	return returnSHA1String
}

func SetupConfig(path string) {
	if UnmarshalConfig(path) {
		scheduleConfig()
	}
}

func scheduleConfig() {
	s.Clear()

	if cfg == nil {
		log.Warnf("Config is empty. Not scheduling")
		return
	}

	hostname := util.GetHostname()

	log.Infof("Scheduling config for %v", hostname)

	for _, group := range cfg.Groups{
		if len(group.Hosts) == 0 {
			continue
		}

		if !util.SliceContainsElement(group.Hosts, "all") && util.SliceContainsElement(group.Hosts, hostname) == false {
			continue;
		}

		log.Infof("Scheduling command group")

		for _, mapping := range(group.Mappings) {
			scheduleCommandMapping(&mapping)
		}
	}
}

func max(a int, b int) int {
	if a > b {
		return a
	} else {
		return b
	}
}

func scheduleCommandMapping(mapping *CommandMapping) {
	cmd := cfg.FindCommand(mapping.Command)

	interval := max(1, mapping.Interval)

	log.Infof("Scheduling command %v with interval: %v", cmd.Name, interval)

	if len(mapping.Arguments) > 0 {
		arg := mapping.Arguments[0]

		for i := 0; i < len(arg.Values); i++ {
			av := arg.Values[i]
			s.Every(interval).Minutes().Do(func() {
				execCommand(cmd, arg.Name, av)
			})
		}
	} else {
		s.Every(interval).Minutes().Do(func() {
			execCommand(cmd, "ignoreme", "")
		})
	}
}

func ExecCommandByName(name string) {
	cmd := cfg.FindCommand(name)

	if cmd != nil {
		execCommand(cmd, "", "")
	}
}

func execCommand(cmd *Command, argName string, argVal string) {
	runerrString := ""

	if cmd.Label == "" {
		cmd.Label = cmd.Name
	}

	shellExec := strings.Replace(cmd.Exec, "{{ " + argName + " }}", argVal, 1)
	commandLabel := strings.Replace(cmd.Label, "{{ " + argName + " }}", argVal, 1)

	shellExec = strings.Replace(shellExec, "PROBE:", "PYTHONPATH=/opt/upsilon/upsilon-pycommon/ python /opt/upsilon/upsilon-drone-probes/src/", 1)

	log.Infof("Executing: %v = %v", cmd.Name, shellExec)

	stdout, stderr, runerr, exit := easyexec.ExecShell(shellExec)

	if runerr != nil {
		runerrString = runerr.Error()
	}

	res := &pb.ExecutionResult{
		UnixTimestamp: time.Now().Unix(),
		Hostname: util.GetHostname(),
		Name: commandLabel,
		Runerr: runerrString,
		Stdout: stdout,
		Stderr: stderr,
		ExitCode: int64(exit),
	}


	log.Infof("stdout: %v", stdout)
	log.Infof("stderr: %v", stderr)

	if runerr != nil {
		log.Errorf("runerr: %v", runerr)
	}

	amqp.PublishPb(res)
}
