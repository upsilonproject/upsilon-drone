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
	"os"
	"reflect"
	"fmt"

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

	for idx, group := range cfg.Groups{
		if len(group.Hosts) == 0 {
			continue
		}

		if !util.SliceContainsElement(group.Hosts, "all") && util.SliceContainsElement(group.Hosts, hostname) == false {
			continue;
		}

		log.Infof("Scheduling command group %v", idx)

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

func checkArgTypesAndFindList(arguments map[string]any) (string, []string) {
	listArgName := ""

	for name, val := range arguments {
		typ := reflect.TypeOf(val).String()

		switch typ {
		case "[]interface {}":
			if listArgName != "" {
				log.Errorf("Cannot have two list args: %v and %v", listArgName, name)
			} else {
				listArgName = name
			}
			break
		case "string": break
		case "int": break
		default:
			log.Errorf("unsupported type for arg %v:%v", name, typ)
		}
	}

	listArgValues := make([]string, 0)

	for _, val := range arguments[listArgName].([]interface{}) {
		listArgValues = append(listArgValues, val.(string))
	}

	return listArgName, listArgValues
}

func argumentsToExecutions(commandName string, arguments map[string]any) []map[string]string {
	ret := make([]map[string]string, 0)

	if len(arguments) == 0 {
		ret = append(ret, make(map[string]string, 0))
	} else {
		listArgName, listArgValues := checkArgTypesAndFindList(arguments)

		if len(listArgValues) == 0 {
			permutation := make(map[string]string, 0)

			for name, val := range arguments {
				permutation[name] = val.(string)
			}

			ret = append(ret, permutation)
		} else {
			for _, laval := range listArgValues {
				permutation := make(map[string]string, 0)

				for name, val := range arguments {
					if (name == listArgName) {
						permutation[name] = laval
					} else {
						permutation[name] = fmt.Sprintf("%v", val)
					}
				}

				ret = append(ret, permutation)
			}
		}
	}

	return ret;
}

func scheduleCommandMapping(mapping *CommandMapping) {
	cmd := cfg.FindCommand(mapping.Command)

	if mapping.Interval == 0 {
		log.Infof("> Skipping schedule of command %v, interval = 0", cmd.Name)
		return;
	}

	interval := max(1, mapping.Interval)

	log.Infof("> Scheduling command %v with %v min interval, and %v static args", cmd.Name, interval, len(mapping.Arguments))

	for _, argmap := range argumentsToExecutions(mapping.Command, mapping.Arguments) {
		s.Every(interval).Minutes().Do(func() {
			execCommand(cmd, argmap)
		})
	}
}

func ExecCommandByName(name string) {
	cmd := cfg.FindCommand(name)

	if cmd != nil {
		execCommandNoArgs(cmd)
	} else {
		util.SendEventErr("CommandByName not found: " + name, nil)
	}
}

func execCommandNoArgs(cmd *Command) {
	execCommand(cmd, map[string]string {})
}

func execCommand(cmd *Command, arguments map[string]string) {
	runerrString := ""

	commandLabel := cmd.Label

	if commandLabel == "" {
		commandLabel = cmd.Name
	}

	shellExec := cmd.Exec
	shellExec = strings.Replace(shellExec, "PROBE:", "PYTHONPATH=/opt/upsilon/upsilon-pycommon/ python /opt/upsilon/upsilon-drone-probes/src/", 1)

	log.Infof("Command %v has %v arguments", cmd.Name, len(arguments))

	for argName := range arguments {
		argVal := arguments[argName]

		log.Infof("arg %v = %v", argName, argVal)

		shellExec = strings.Replace(shellExec, "{{ " + argName + " }}", argVal, 1)
		commandLabel = strings.Replace(commandLabel, "{{ " + argName + " }}", argVal, 1)
	}


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
