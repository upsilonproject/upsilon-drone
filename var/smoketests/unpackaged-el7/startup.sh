#!/bin/bash

function onError() {
	echo "Non-Zero exit status, $(caller $((n++)))"
}

trap onErr ERR

UUID=`uuidgen`
docker create $UUID
docker start $UUID
docker exec -it $UUID /usr/share/upsilon-node/bin/tools/upsilon-test-envionment
docker stop $UUID
docker rm $UUID
