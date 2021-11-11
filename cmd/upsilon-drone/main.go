package main

import (
	"github.com/go-co-op/gocron"
	log "github.com/sirupsen/logrus"
	"github.com/upsilonproject/upsilon-drone/internal/amqp"
	"github.com/upsilonproject/upsilon-drone/internal/updater"
	"github.com/upsilonproject/upsilon-drone/internal/buildconstants"
	"github.com/spf13/viper"
	"github.com/spf13/cobra"
	"time"
	"os"
	"fmt"
)

var rootCmd = &cobra.Command {
	Use:   "main",
	Run: func(cmd *cobra.Command, args[] string) {
		mainDrone()
	},
}

var cmdVersion = &cobra.Command {
	Use: "version",
	Short: "Print version and exit",
	Run: func(cmd *cobra.Command, args[] string) {
		fmt.Println(buildconstants.Timestamp)
		os.Exit(0)
	},
}

func init() {
	cobra.OnInitialize(initConfig)

	rootCmd.AddCommand(cmdVersion)
}

func initConfig() {
	viper.SetConfigType("yaml")
	viper.AddConfigPath("/etc/upsilon-drone/")
	viper.SetConfigName("upsilon-drone")

	viper.AutomaticEnv() // read in environment variables that match

	if err := viper.ReadInConfig(); err == nil {
		log.Errorf("Using config file:", viper.ConfigFileUsed())
	}
}

func mainDrone() {
	log.WithFields(log.Fields{
		"timestamp": buildconstants.Timestamp,
	}).Infof("upsilon-drone")

	s := gocron.NewScheduler(time.UTC)
	s.Every(4).Hours().Do(func() {
		go updater.Update()
	})

	go amqp.StartServerListener()

	amqp.StartHeartbeater()
}

func main() {
	cobra.CheckErr(rootCmd.Execute())
}
