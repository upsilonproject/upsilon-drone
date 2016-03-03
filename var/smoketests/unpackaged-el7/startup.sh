#!/bin/bash

function onErr() {
	STATUS=$?
	echo "Exit status in startup.sh, status: $STATUS at $(caller $((n++)))"
	exit $STATUS
}

trap onErr ERR

UUID=$(docker create upsilon-node)
docker start $UUID

sleep 5 # upsilon-node needs time to start it's internals for testing

docker exec -i $UUID /usr/share/upsilon-node/bin/tools/upsilon-test-envionment
docker stop $UUID
docker rm $UUID
