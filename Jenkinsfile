#!groovy    

properties(                                                                        
    [                                                                              
        buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10')), 
        [$class: 'CopyArtifactPermissionProperty', projectNames: '*'],             
        pipelineTriggers([[$class: 'PeriodicFolderTrigger', interval: '1d']])   
    ]                                                                              
)                                                                         

def prepareEnv() {
    deleteDir()                                                                    
    unstash 'binaries'                                                             
                                                                                   
    env.WORKSPACE = pwd()                                                          
                                                                                   
    sh "find ${env.WORKSPACE}"                                                     
                                                                                   
    sh 'mkdir -p SPECS SOURCES'                                                    
    sh "cp build/distributions/*.zip SOURCES/upsilon-drone.zip"                                  
}

def buildDockerContainer() {
	prepareEnv()

	unstash 'el7'
	sh 'mv RPMS/noarch/*.rpm RPMS/noarch/upsilon-drone.rpm'

	sh 'unzip -jo SOURCES/upsilon-drone.zip "upsilon-drone-*/var/pkg/Dockerfile" "upsilon-drone-*/.buildid" -d . '

	tag = sh script: 'buildid -pk tag', returnStdout: true

	println "tag: ${tag}"

	sh "docker build -t 'upsilonproject/drone:${tag}' ."
	sh "docker tag 'upsilonproject/drone:${tag}' 'upsilonproject/drone:latest' "
	sh "docker save upsilonproject/drone:${tag} > upsilon-drone-docker-${tag}.tgz"

	archive "upsilon-drone-docker-${tag}.tgz"
}
                                                                                   
def buildRpm(dist) {                                                               
	prepareEnv()
                                                                                                                                                                      
    sh 'unzip -jo SOURCES/upsilon-drone.zip "upsilon-drone-*/var/pkg/upsilon-drone.spec" "upsilon-drone-*/.upsilon-drone.rpmmacro" -d SPECS/'
    sh "find ${env.WORKSPACE}"                                                     
                                                                                   
    sh "rpmbuild -ba SPECS/upsilon-drone.spec --define '_topdir ${env.WORKSPACE}' --define 'dist ${dist}'"
                                                                                   
    archive 'RPMS/noarch/*.rpm'                                                    
	stash includes: "RPMS/noarch/*.rpm", name: dist
}                    

def buildDeb(dist) {
	prepareEnv()
	
	sh 'unzip -jo SOURCES/upsilon-drone.zip "upsilon-drone-*/var/pkg/deb/" -d . '
    sh "find ${env.WORKSPACE}"                                                     

	sh "dpkg-buildpackage -d "
}

node {
	stage("Prep") {                                                                                
		deleteDir()
		checkout scm
	}

	stage("Compile") {
		def gradle = tool 'gradle'

		sh "${gradle}/bin/gradle distZip distTar"

		archive 'build/distributions/**'

		stash includes: "build/distributions/**" , name: "binaries"
	}
}

node {
	stage("Smoke") {
		echo "Smokin' :)"
	}
}

stage("Package") {
	node {                                                                             
		buildRpm("el7")                                                                
	}                                                                                  
																					   
	node {                                                                             
		buildRpm("el6")                                                                
	}                                                                                  
																					   
	node {                                                                             
		buildRpm("fc24")                                                               
	}

	node {
	//	buildDeb("ubuntu-16.4")
	}

	node {
	//	buildDockerContainer()
	}
}
