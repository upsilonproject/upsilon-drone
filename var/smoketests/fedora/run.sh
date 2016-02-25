#!/bin/bash
docker stop foo
docker rm foo
docker run -itd --name foo jread/upsilon-node
docker cp test.sh foo:/opt/
docker exec foo /opt/test.sh
