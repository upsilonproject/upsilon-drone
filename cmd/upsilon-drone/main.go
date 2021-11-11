package main

import (
	"fmt"
	"github.com/go-co-op/gocron"
	log "github.com/sirupsen/logrus"
	"github.com/spf13/cobra"
	"github.com/spf13/viper"
	"github.com/upsilonproject/upsilon-drone/internal/amqp"
	"github.com/upsilonproject/upsilon-drone/internal/buildconstants"
	"github.com/upsilonproject/upsilon-drone/internal/updater"
	"os"
	"time"
)

var rootCmd = &cobra.Command{
	Use: "main",
	Run: func(cmd *cobra.Command, args []string) {
		mainDrone()
	},
}

var cmdVersion = &cobra.Command{
	Use:   "version",
	Short: "Print version and exit",
	Run: func(cmd *cobra.Command, args []string) {
		fmt.Println(buildconstants.Timestamp)
		os.Exit(0)
	},
}

var cmdUpdate = &cobra.Command{
	Use:   "update",
	Short: "Force update and exit",
	Run: func(cmd *cobra.Command, args []string) {
		updater.Update()
		os.Exit(0)
	},
}

func disableLogTimestamps() {
	log.SetFormatter(&log.TextFormatter{
		DisableColors:    false,
		DisableTimestamp: true,
	})
}

func init() {
	disableLogTimestamps()

	cobra.OnInitialize(initConfig)

	rootCmd.AddCommand(cmdVersion)
	rootCmd.AddCommand(cmdUpdate)
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
	s.Every(15).Minutes().Do(func() {
		go updater.Update()
	})

	s.StartAsync()

	go amqp.StartServerListener()

	amqp.StartHeartbeater()
}

func main() {
	cobra.CheckErr(rootCmd.Execute())
}
