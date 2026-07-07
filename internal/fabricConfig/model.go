package fabricConfig

func (cfg *FabricConfig) FindCommand(search string) *Command {
	for _, cmd := range cfg.Commands {
		if cmd.Name == search {
			return &cmd
		}
	}

	return nil
}

func (cfg *FabricConfig) FindPipeline(search string) *CommandPipeline {
	for _, pl := range cfg.Pipelines {
		if pl.Name == search {
			return &pl
		}
	}

	return nil
}

func (cfg *FabricConfig) normalize() {
	for i := range cfg.Groups {
		group := &cfg.Groups[i]
		if len(group.Commands) == 0 && len(group.Mappings) > 0 {
			group.Commands = group.Mappings
		}
	}
}

type FabricConfig struct {
	Commands  []Command          `yaml:"commands"`
	Groups    []CommandGroup     `yaml:"groups"`
	Pipelines []CommandPipeline  `yaml:"pipelines"`
}

type CommandGroup struct {
	Hosts     []string           `yaml:"hosts"`
	Commands  []CommandMapping   `yaml:"commands"`
	Mappings  []CommandMapping   `yaml:"mappings"`
	Pipelines []PipelineMapping  `yaml:"pipelines"`
}

type Command struct {
	Name  string `yaml:"name"`
	Exec  string `yaml:"exec"`
	Label string `yaml:"label"`
}

type CommandMapping struct {
	Command   string         `yaml:"command"`
	Interval  int            `yaml:"interval"`
	Arguments map[string]any `yaml:"arguments"`
}

type PipelineMapping struct {
	Pipeline  string         `yaml:"pipeline"`
	Interval  int            `yaml:"interval"`
	Arguments map[string]any `yaml:"arguments"`
}

type CommandPipeline struct {
	Name     string           `yaml:"name"`
	Sequence []CommandMapping `yaml:"sequence"`
}
