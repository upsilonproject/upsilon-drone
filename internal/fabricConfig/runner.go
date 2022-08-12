package fabricConfig

import (
	"gopkg.in/yaml.v2"
	log "github.com/sirupsen/logrus"
	"io/ioutil"
	"github.com/go-co-op/gocron"
	"github.com/upsilonproject/upsilon-drone/internal/easyexec"
	"github.com/upsilonproject/upsilon-drone/internal/util"
	"time"
	pb "github.com/upsilonproject/upsilon-drone/gen/amqpproto"
	amqp "github.com/upsilonproject/upsilon-gocommon/pkg/amqp"
)

var cfg *FabricConfig;
var s *gocron.Scheduler

func init() {
	s = gocron.NewScheduler(time.UTC)
	s.Clear()
	s.StartAsync()
}

func unmarshalConfig(path string) bool {
	log.Infof("Running config: %v", path)

	yamlFile, err := ioutil.ReadFile(path + "/config.yml")

	if err != nil {
		log.Errorf("Read file error: %v", err)
		return false
	}

	err = yaml.Unmarshal(yamlFile, &cfg)

	if err != nil {
		util.SendEventErr("Unmarshal event error", err)
	}

	log.Infof("Got config: %v", cfg)

	return true
}

func SetupConfig(path string) {
	if unmarshalConfig(path) {
		scheduleConfig()
	}
}

func scheduleConfig() {
	s.Clear()

	hostname := util.GetHostname()
	
	for _, group := range cfg.Groups{
		if len(group.Hosts) == 0 {
			continue
		}

		if !util.SliceContainsElement(group.Hosts, "all") || !util.SliceContainsElement(group.Hosts, hostname) {
			continue;
		}

		scheduleCommandGroup(&group)
	}
}

func max(a int, b int) int {
	if a > b {
		return a
	} else {
		return b
	}
}

func scheduleCommandGroup(cfg *CommandGroup) {
	for i := 0; i < len(cfg.Commands); i++  {
		cmd := cfg.Commands[i]
		interval := max(1, cmd.Interval)

		log.Infof("Scheduling command %v with interval: %v", cmd.Name, interval)

		s.Every(interval).Minutes().Do(func() {
			execCommand(&cmd)
		})
	}
}

func ExecCommandByName(name string) {
	for _, group := range cfg.Groups {
		// Deliberately don't check hostname here, execreqs ignore if a group
		// is assigned to a host and use the command only

		for _, command := range group.Commands {
			if command.Name == name {
				execCommand(&command)
			}
		}
	}
}

func execCommand(cmd *Command) {
	runerrString := ""

	log.Infof("Executing: %v", cmd.Name)

	stdout, stderr, runerr := easyexec.Exec(cmd.Exec, cmd.Args)

	if runerr != nil {
		runerrString = runerr.Error()
	}

	res := &pb.ExecutionResult{
		UnixTimestamp: time.Now().Unix(),
		Hostname: util.GetHostname(),
		Name: cmd.Name,
		Runerr: runerrString,
		Stdout: stdout,
		Stderr: stderr,
	}


	log.Infof("stdout: %v", stdout)
	log.Infof("stderr: %v", stderr)

	if runerr != nil {
		log.Errorf("runerr: %v", runerr)
	}

	amqp.PublishPb(res)
}
