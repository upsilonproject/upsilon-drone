package fabricConfig

import (
	"gopkg.in/yaml.v2"
	log "github.com/sirupsen/logrus"
	"io/ioutil"
	"os"
	"github.com/go-co-op/gocron"
	"github.com/upsilonproject/upsilon-drone/internal/easyexec"
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

func Run(path string) {
	log.Infof("Running config: %v", path)

	yamlFile, err := ioutil.ReadFile(path + "/config.yml")

	if err != nil {
		log.Errorf("Read file error: %v", err)
		return
	}

	err = yaml.Unmarshal(yamlFile, &cfg)

	if err != nil {
		log.Errorf("Unmarshal issue: %v", err)
	}

	log.Infof("Got config: %v", cfg)


	s.Clear()

	hostname, _ := os.Hostname()
	
	for _, group := range cfg.Groups{
		skipGroup := true

		if len(group.Hosts) == 0 {
			skipGroup = false;
		} else { 
			for _, hn := range group.Hosts {
				log.Infof("%v == %v", hn, hostname)

				if hostname == hn {
					skipGroup = false
					break;
				}
			}
		}

		if skipGroup {
			continue;
		}

		Schedule(&group)
	}
}

func max(a int, b int) int {
	if a > b {
		return a
	} else {
		return b
	}
}

func Schedule(cfg *CommandGroup) {
	for i := 0; i < len(cfg.Commands); i++  {
		cmd := cfg.Commands[i]
		interval := max(1, cmd.Interval)

		log.Infof("Scheduling command %v with interval: %v", cmd.Name, interval)

		s.Every(interval).Minutes().Do(func() {
			Exec(&cmd)
		})
	}
}

func Exec(cmd *Command) {
	runerrString := ""

	log.Infof("Executing: %v", cmd.Name)

	stdout, stderr, runerr := easyexec.Exec(cmd.Exec, cmd.Args)

	if runerr != nil {
		runerrString = runerr.Error()
	}

	res := &pb.ExecutionResult{
		UnixTimestamp: time.Now().Unix(),
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
