build:
	go build -ldflags="-X github.com/upsilonproject/upsilon-drone/internal/buildconstants.Timestamp=$(shell date +"%s")" -o upsilon-drone

rollout: build
	scp upsilon-drone root@upsilon:/var/www/html/
