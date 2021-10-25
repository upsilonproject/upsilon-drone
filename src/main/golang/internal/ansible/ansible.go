package ansible

import (
	"fmt"

	ans "github.com/apenella/go-ansible"
)

type MyExecutor struct {}

func (e *MyExecutor) Execute(command string, args []string, prefix string) error {
	fmt.Println("Exec", command);

	return nil;
}

func runAnsible() {
	playbook := &ans.AnsiblePlaybookCmd {
		Playbook: "foo.yml",
		ConnectionOptions: &ans.AnsiblePlaybookConnectionOptions { Connection: "local" },
		Options: &ans.AnsiblePlaybookOptions {
			Inventory: "localhost,",
		},
//		Exec: &MyExecutor{},
	}

	playbook.Run();
}
