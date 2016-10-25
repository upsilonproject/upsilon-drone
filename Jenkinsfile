#!groovy                                                                           
                                                                                   
properties(                                                                        
    [                                                                              
        [                                                                          
            $class: 'jenkins.model.BuildDiscarderProperty', strategy: [$class: 'LogRotator', numToKeepStr: '10', artifactNumToKeepStr: '10'],
            $class: 'CopyArtifactPermissionProperty', projectNames: '*'            
        ]                                                                          
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

	tag = sh 'buildid -k tag'

	sh 'unzip -jo SOURCES/upsilon-node.zip "upsilon-node-*/var/pkg/Dockerfile" "upsilon-node-*/.buildid" -d . '
	sh "docker build -t upsilonproject/node:${tag} ."
	sh "docker save upsilonproject/node:${tag} > upsilon-node-docker.tgz"

	archive 'upsilon-node-docker.tgz'
}
                                                                                   
def buildRpm(dist) {                                                               
	prepareEnv()
                                                                                                                                                                      
    sh 'unzip -jo SOURCES/upsilon-node.zip "upsilon-node-*/var/pkg/upsilon-node.spec" "upsilon-node-*/.upsilon-node.rpmmacro" -d SPECS/'
    sh "find ${env.WORKSPACE}"                                                     
                                                                                   
    sh "rpmbuild -ba SPECS/upsilon-node.spec --define '_topdir ${env.WORKSPACE}' --define 'dist ${dist}'"
                                                                                   
    archive 'RPMS/noarch/*.rpm'                                                    
}                    

def buildDeb(dist) {
	prepareEnv()
	
	sh 'unzip -jo SOURCES/upsilon-node.zip "upsilon-node-*/var/pkg/deb/" -d . '
    sh "find ${env.WORKSPACE}"                                                     

	sh "cd /var/pkg/deb/; dpkg-buildpackage"


                                                                                   
}

node {
	stage("Prep") {                                                                                
		deleteDir()
		checkout scm
	}

	stage("Compile") {
		def gradle = tool 'gradle'

		sh "${gradle}/bin/gradle distZip"

		archive 'build/distributions/*.zip'

		stash includes:"build/distributions/*.zip", name: "binaries"
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
}

node {
	buildDockerContainer()
}

node {
//	buildDeb("ubuntu-16.4")
}
