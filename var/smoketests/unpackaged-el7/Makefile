# This makefile relies on a built distribution (built by gradle) 3 levels up. 
# It's mostly intended for a jenkins job.
default:
	cp -r ../../../upsilon-node/* ./
	sed 's/localhost/upsilon/' etc/config.xml.sample > etc/config.xml

	docker build -t upsilon-node . 
	./startup.sh
