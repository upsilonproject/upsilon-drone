# This makefile relies on a built distribution (built by gradle) 3 levels up. 
# It's mostly intended for a jenkins job.
default:
	rm -rf bin lib etc
	cp -r ../../../bin/ ./
	cp -r ../../../lib/ ./
	cp -r ../../../etc ./
	cp -r ../../../var ./
	sed 's/localhost/upsilon/' ../../../etc/config.xml.sample > config.xml

	docker build -t upsilon-node . 
	./startup.sh
