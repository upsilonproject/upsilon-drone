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
	"time"
)

var rootCmd = &cobra.Command{
	Use: "main",
	Run: func(cmd *cobra.Command, args []string) {
		disableAmqp, _ := cmd.PersistentFlags().GetBool("disable-amqp")
		disableUpdates, _ := cmd.PersistentFlags().GetBool("disable-updates")
		offline, _ := cmd.PersistentFlags().GetBool("offline")

		if offline {
			disableAmqp = true
			disableUpdates = true
		}

		mainDrone(disableAmqp, disableUpdates)
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

func mainDrone(disableAmqp bool, disableUpdates bool) {
	serviceConfig.Refresh()

	log.WithFields(log.Fields{
		"Build Timestamp": buildconstants.Timestamp,
	}).Infof("upsilon-drone")

	if disableUpdates {
		log.Infof("Updates are disabled")
	} else {
		checkForceUpdate()

		go updater.StartCron()
	}

	if disableAmqp {
		log.Infof("AMQP is disabled, ping, requests, heartbeats etc will not work")

		commonAmqp.Offline = true
	} else {
		commonAmqp.ConnectionIdentifier = "upsilon-drone " + buildconstants.Timestamp

		go amqp.ListenForPings()
		go amqp.ListenForUpdateRequests()
		go amqp.ListenForGitPulls()
		go amqp.ListenForExecutionRequests()
		go amqp.SendStartup()
		go amqp.StartHeartbeater()
	}

	fabricConfig.SetupConfig("/etc/upsilon-drone-fabric/upsilon-config/")

	for {
		time.Sleep(1 * time.Second) // FIXME ugly hack
	}

}

func main() {
	disableLogTimestamps()

	rootCmd.AddCommand(cmdVersion)
	rootCmd.AddCommand(cmdUpdate)
	rootCmd.AddCommand(cmdCheckFabricConfig)
	rootCmd.PersistentFlags().Bool("disable-amqp", false, "disables AMQP")
	rootCmd.PersistentFlags().Bool("disable-updates", false, "disables updates")
	rootCmd.PersistentFlags().Bool("offline", false, "disable AMQP, Updates, etc")

	cobra.CheckErr(rootCmd.Execute())
}
