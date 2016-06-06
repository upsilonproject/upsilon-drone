node {
	stage "Prep"
	def gradle = tool 'gradle'
	checkout scm

	stage "Compile"
	sh '${gradle}/bin/gradle distZip'

	stage "Smoke"
	echo "Smokin' :)"

	stage "Package"
	parallel dockerFedora: {

	}, rpmFedora: {

	}, rpmEl6: {

	}, rpmEl7: {

	}, failFast: true


	stage "Publish"

	parallel publishDockerFedora: {

	}, publishRpmFedora: {

	}, publishRpmEl6: {

	}, publishRpmEl7: {

	}, failFast: true
}
