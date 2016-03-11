# This makefile relies on a built distribution (built by gradle) 3 levels up. 
# It's mostly intended for a jenkins job.
default:
	rm -rf bin etc lib
	cp -r ../../../bin ./
	cp -r ../../../etc ./
	cp -r ../../../lib ./
	cp -r ../../../var/tools ./bin/
	cp etc/config.xml.sample > etc/config.xml

	docker build -t upsilon-node . 
	./startup.sh
