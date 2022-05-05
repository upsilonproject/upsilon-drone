package fabricConfig

import (
	"gopkg.in/yaml.v2"
	log "github.com/sirupsen/logrus"
	"io/ioutil"
)

var cfg *FabricConfig;

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

	Run2(cfg)
}

func Run2(cfg *FabricConfig) {

}
