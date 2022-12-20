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

var cmdCheckFabricConfig = &cobra.Command{
	Use: "check-fabric-config",
	Short: "Check Config",
	Run: func(cmd *cobra.Command, args []string) {
		serviceConfig.Refresh()

		path := "./"

		if (len(args) == 1) {
			path = args[0]
		}

		fabricConfig.UnmarshalConfig(path)

		os.Exit(0)
	},
}

func disableLogTimestamps() {
	log.SetFormatter(&log.TextFormatter{
		DisableColors:    false,
		DisableTimestamp: true,
	})
}

func checkForceUpdate() {
	actual, _ := os.Executable()

	log.WithFields(log.Fields {
		"expected": updater.DRONE_PATH,
		"actual": actual,
	}).Debugf("Path check")

	if actual != updater.DRONE_PATH && os.Getenv("DRONE_ANYPATH") == "" {
		log.Warnf("Executable is running from non-standard path, DRONE_ANYPATH is not set, so will force an update.")
		updater.Update()
		os.Exit(0)
	}
}

func mainDrone() {
	serviceConfig.Refresh()

	log.WithFields(log.Fields{
		"Build Timestamp": buildconstants.Timestamp,
	}).Infof("upsilon-drone")

	checkForceUpdate()

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

func main() {
	disableLogTimestamps()

	rootCmd.AddCommand(cmdVersion)
	rootCmd.AddCommand(cmdUpdate)
	rootCmd.AddCommand(cmdCheckFabricConfig)

	cobra.CheckErr(rootCmd.Execute())
}
