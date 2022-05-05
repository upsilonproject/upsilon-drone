package fabricConfig

type FabricConfig struct {
	Commands []Command
}

type Command struct {
	Exec string
	Args []string
}
