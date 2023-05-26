package fabricConfig

type FabricConfig struct {
	Groups []CommandGroup
	Commands []Command
}

func (cfg *FabricConfig) FindCommand(search string) *Command {
	for _, cmd := range(cfg.Commands) {
		if cmd.Name == search {
			return &cmd
		}
	}

	return nil
}

type CommandGroup struct {
	Hosts []string
	Mappings []CommandMapping
}

type Command struct {
	Name string
	Exec string
	Label string
}

type CommandMapping struct {
	Command string
	Interval int
	Arguments map[string]any
}

