package easyexec

import (
	"bytes"
	"time"
	"context"
	"os/exec"
	"os"
	log "github.com/sirupsen/logrus"
)


func ExecShell(executable string) (string, string, error) {
	return Exec("sh", []string { "-c", executable })
}

func Exec(executable string, args []string) (string, string, error) {
	ctx, cancel := context.WithTimeout(context.Background(), time.Duration(10) * time.Second)
	defer cancel()

	var stdout bytes.Buffer;
	var stderr bytes.Buffer;

	cmd := exec.CommandContext(ctx, executable, args...)
	cmd.Stdout = &stdout;
	cmd.Stderr = &stderr;

	runerr := cmd.Run()

	return stdout.String(), stderr.String(), runerr
}

func ExecLog(executable string, args []string) (string, string, error) {
	cwd, _ := os.Getwd()

	log.Infof("cwd: %v", cwd)
	log.Infof("cmd: %v %v", executable, args)

	stderr, stdout, runerr := Exec(executable, args)

	if runerr != nil {
		log.Errorf("err: %v", runerr)
	}

	if stderr != "" {
		log.Warnf("stderr: %v", stderr)
	}

	log.Infof("stdout: %v", stdout)

	return stdout, stderr, runerr
}
