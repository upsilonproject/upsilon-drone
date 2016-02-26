default:
	rm -rf bin lib etc
	cp -r ../../../bin/ ./
	cp -r ../../../lib/ ./
	cp -r ../../../etc ./

	docker build -t upsilon-node . 
	./startup.sh
