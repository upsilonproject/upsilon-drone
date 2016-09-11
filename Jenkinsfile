#!groovy                                                                           
                                                                                   
properties(                                                                        
    [                                                                              
        [                                                                          
            $class: 'jenkins.model.BuildDiscarderProperty', strategy: [$class: 'LogRotator', numToKeepStr: '10', artifactNumToKeepStr: '10'],
            $class: 'CopyArtifactPermissionProperty', projectNames: '*'            
        ]                                                                          
    ]                                                                              
)                                                                                  
                                                                                   
def buildRpm(dist) {                                                               
    deleteDir()                                                                    
                                                                                   
    unstash 'binaries'                                                             
                                                                                   
    env.WORKSPACE = pwd()                                                          
                                                                                   
    sh "find ${env.WORKSPACE}"                                                     
                                                                                   
    sh 'mkdir -p SPECS SOURCES'                                                    
    sh "cp build/distributions/*.zip SOURCES/upsilon-node.zip"                                  
                                                                                   
    sh 'unzip -jo SOURCES/upsilon-node.zip "upsilon-node-*/var/upsilon-node.spec" "upsilon-node-*/.upsilon-node.rpmmacro" -d SPECS/'
    sh "find ${env.WORKSPACE}"                                                     
                                                                                   
    sh "rpmbuild -ba SPECS/upsilon-node.spec --define '_topdir ${env.WORKSPACE}' --define 'dist ${dist}'"
                                                                                   
    archive 'RPMS/noarch/*.rpm'                                                    
}                                                                                  
                                                                                   
node {
	stage "Prep"

	deleteDir()
	def gradle = tool 'gradle'

	checkout scm

	stage "Compile"
	sh "${gradle}/bin/gradle distZip"

	stash includes "build/distributions/*.zip", name: "binaries"
}

node {
	stage "Smoke"
	echo "Smokin' :)"
}

stage "Package"

node {                                                                             
    buildRpm("el7")                                                                
}                                                                                  
                                                                                   
node {                                                                             
    buildRpm("el6")                                                                
}                                                                                  
                                                                                   
node {                                                                             
    buildRpm("fc24")                                                               
}


