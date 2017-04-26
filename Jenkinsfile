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
    sh "cp build/distributions/*.zip SOURCES/upsilon-node.zip"                                  
}

def buildDockerContainer() {
	prepareEnv()

	unstash 'el7'
	sh 'mv RPMS/noarch/*.rpm RPMS/noarch/upsilon-node.rpm'

	sh 'unzip -jo SOURCES/upsilon-node.zip "upsilon-node-*/var/pkg/Dockerfile" "upsilon-node-*/.buildid" -d . '

	tag = sh script: 'buildid -pk tag', returnStdout: true

	println "tag: ${tag}"

	sh "docker build -t 'upsilonproject/node:${tag}' ."
	sh "docker save upsilonproject/node:${tag} > upsilon-node-docker-${tag}.tgz"

	archive "upsilon-node-docker-${tag}.tgz"
}
                                                                                   
def buildRpm(dist) {                                                               
	prepareEnv()
                                                                                                                                                                      
    sh 'unzip -jo SOURCES/upsilon-node.zip "upsilon-node-*/var/pkg/upsilon-node.spec" "upsilon-node-*/.upsilon-node.rpmmacro" -d SPECS/'
    sh "find ${env.WORKSPACE}"                                                     
                                                                                   
    sh "rpmbuild -ba SPECS/upsilon-node.spec --define '_topdir ${env.WORKSPACE}' --define 'dist ${dist}'"
                                                                                   
    archive 'RPMS/noarch/*.rpm'                                                    
	stash includes: "RPMS/noarch/*.rpm", name: dist
}                    

def buildDeb(dist) {
	prepareEnv()
	
	sh 'unzip -jo SOURCES/upsilon-node.zip "upsilon-node-*/var/pkg/deb/" -d . '
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
		buildDockerContainer()
	}
}
