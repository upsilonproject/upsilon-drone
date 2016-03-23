#!/bin/bash -x

function onErr() {
	STATUS=$?
	echo "Exit status in startup.sh, status: $STATUS at $(caller $((n++)))"
	exit $STATUS
}

trap onErr ERR

UUID=$(docker create upsilon-node)
docker start $UUID

sleep 10 # upsilon-node needs time to start it's internals for testing
docker ps
docker inspect $UUID > docker-inspect.log

docker exec -i $UUID /usr/share/upsilon-node/bin/tools/upsilon-test-envionment
docker stop $UUID
docker rm $UUID
