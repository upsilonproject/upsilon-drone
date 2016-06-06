node {
	stage "Prep"
	checkout scm

	stage "Compile"
	sh 'gradle distZip'

	stage "Smoke"
	echo "Smokin' :)"

	stage "Package"
	parallel docker-fedora: {

	}, 
	rpm-fedora: {

	}, 
	rpm-el6: {

	}, 
	rpm-el7: {

	}, 
	failFast: true


	stage "Publish"

	parallel publish-docker-fedora: {

	},
	publish-rpm-fedora: {

	},
	publish-rpm-el6: {

	},
	publish-rpm-el7: {

	},
	failFast: true

}
