package fabricConfig

type FabricConfig struct {
	Groups []CommandGroup
}

type CommandGroup struct {
	Hosts []string
	Commands []Command
}

type Command struct {
	Name string
	Exec string
	Args []string
	Interval int
}
