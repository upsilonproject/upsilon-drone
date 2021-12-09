package main

import (
	"fmt"
	log "github.com/sirupsen/logrus"
	"github.com/spf13/cobra"
	commonAmqp "github.com/upsilonproject/upsilon-gocommon/pkg/amqp"
	"github.com/upsilonproject/upsilon-drone/internal/amqp"
	"github.com/upsilonproject/upsilon-drone/internal/config"
	"github.com/upsilonproject/upsilon-drone/internal/buildconstants"
	"github.com/upsilonproject/upsilon-drone/internal/updater/v2"
	"os"
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

func mainDrone() {
	commonAmqp.ConnectionIdentifier = "upsilon-drone " + buildconstants.Timestamp

	go amqp.ListenForPings()

	amqp.StartHeartbeater()
}

func logBanner() {
	log.WithFields(log.Fields{
		"timestamp": buildconstants.Timestamp,
	}).Infof("upsilon-drone")
}

func main() {
	disableLogTimestamps()
	logBanner()

	cobra.OnInitialize(config.Refresh)

	rootCmd.AddCommand(cmdVersion)
	rootCmd.AddCommand(cmdUpdate)

	cobra.CheckErr(rootCmd.Execute())
}
