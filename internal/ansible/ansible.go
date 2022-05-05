package ansible

import (
	"fmt"

	ansplay "github.com/apenella/go-ansible/pkg/playbook"
	ansopts "github.com/apenella/go-ansible/pkg/options"
	"context"
)

type MyExecutor struct{}

func (e *MyExecutor) Execute(command string, args []string, prefix string) error {
	fmt.Println("Exec", command)

	return nil
}

func runAnsible() {
	playbook := &ansplay.AnsiblePlaybookCmd{
		Playbooks:          []string{"foo.yml"},
		ConnectionOptions: &ansopts.AnsibleConnectionOptions{Connection: "local"},
		Options: &ansplay.AnsiblePlaybookOptions{
			Inventory: "localhost,",
		},
		//		Exec: &MyExecutor{},
	}

	playbook.Run(context.Background())
}
