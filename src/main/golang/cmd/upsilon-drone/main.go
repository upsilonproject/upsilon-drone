package main

import (
	"go.uber.org/zap"
	"github.com/upsilonproject/upsilon-drone/internal/amqp"
)

func setupLogger() {
	logger, _ := zap.NewDevelopment();

	zap.ReplaceGlobals(logger);
}

func main() {
	setupLogger();

	log := zap.S()

	log.Info("upsilon-drone")

	amqp.StartServerListener()
}
