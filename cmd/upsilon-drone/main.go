package main

import (
	"fmt"
	log "github.com/sirupsen/logrus"
	"github.com/spf13/cobra"
	commonAmqp "github.com/upsilonproject/upsilon-gocommon/pkg/amqp"
	"github.com/upsilonproject/upsilon-drone/internal/amqp"
	"github.com/upsilonproject/upsilon-drone/internal/fabricConfig"
	"github.com/upsilonproject/upsilon-drone/internal/serviceConfig"
	"github.com/upsilonproject/upsilon-drone/internal/buildconstants"
	"github.com/upsilonproject/upsilon-drone/internal/updater"
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

	go updater.StartCron()
	go amqp.ListenForPings()
	go amqp.ListenForUpdateRequests()
	go amqp.ListenForGitPulls()
	go amqp.ListenForExecutionRequests()
	go fabricConfig.SetupConfig("/etc/upsilon-drone-fabric/upsilon-config/")
	go amqp.SendStartup()

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

	rootCmd.AddCommand(cmdVersion)
	rootCmd.AddCommand(cmdUpdate)

	cobra.OnInitialize(serviceConfig.Refresh)

	exec, _ := os.Executable()

	log.Infof("%v %v", exec, updater.DRONE_PATH)

	if exec != updater.DRONE_PATH && os.Getenv("DRONE_ANYPATH") == "" {
		log.Warnf("Executable is running from non-standard path, DRONE_ANYPATH is not set, so will force an update.")
		updater.Update()
	} else {
		cobra.CheckErr(rootCmd.Execute())
	}
}
