build:
	go build -ldflags="-X github.com/upsilonproject/upsilon-drone/internal/buildconstants.Timestamp=$(shell date +"%s")" -o upsilon-drone cmd/upsilon-drone/main.go 

rollout: build
	scp upsilon-drone root@upsilon:/var/www/html/
