#!/bin/bash
UUID=`uuidgen`
docker run -itd --name $UUID upsilon-node
docker cp tests.sh $UUID:/opt/
docker exec $UUID /opt/tests.sh
docker stop $UUID
docker rm $UUID
