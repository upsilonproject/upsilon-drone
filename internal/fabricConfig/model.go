package fabricConfig

func (cfg *FabricConfig) FindCommand(search string) *Command {
	for _, cmd := range(cfg.Commands) {
		if cmd.Name == search {
			return &cmd
		}
	}

	return nil
}

func (cfg *FabricConfig) FindPipeline(search string) *CommandPipeline {
	for _, pl := range(cfg.Pipelines) {
		if pl.Name == search {
			return &pl
		}
	}

	return nil
}

type FabricConfig struct {
	Commands []Command
	Groups []CommandGroup
	Pipelines []CommandPipeline
}

type CommandGroup struct {
	Hosts []string
	Commands []CommandMapping
	Pipelines []PipelineMapping
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

type PipelineMapping struct {
	Pipeline string
	Interval int
	Arguments map[string]any
}

type CommandPipeline struct {
	Name string
	Sequence []CommandMapping
}
