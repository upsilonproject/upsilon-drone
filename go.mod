module github.com/upsilonproject/upsilon-drone

go 1.14

require (
	github.com/apenella/go-ansible v0.7.0
	github.com/blang/semver v3.5.1+incompatible
	github.com/rhysd/go-github-selfupdate v1.2.3
	github.com/sirupsen/logrus v1.8.1
	github.com/spf13/cobra v1.2.1
	github.com/spf13/viper v1.8.1
	github.com/upsilonproject/upsilon-gocommon v0.0.0-20211112002009-555fc66d9a07
	google.golang.org/protobuf v1.27.1
)

replace github.com/upsilonproject/upsilon-gocommon => /home/xconspirisist/sandbox/Development/upsilon/upsilon-gocommon
