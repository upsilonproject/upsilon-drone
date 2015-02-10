checkout changelog: true, poll: true, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'd19d3df4-0f35-40ea-ac03-8a51e4fd31be', url: 'https://github.com/upsilonproject/upsilon-node.git']]]

node {
    ws {
      stage "compile"
      build 'upsilon-node' 

      stage "package"

      parallel fedora: {
	  build("upsilon-node-rpm-fedora")
	  build("publish upsilon-node-rpm-fedora")
      }, rhel: {
	  build("upsilon-node-rpm-rhel")
	  build("publish upsilon-node-rpm-rhel")
      }, deb: {
	  build("upsilon-node-deb");
	  build("publish upsilon-node-deb")
      }

      build("packages-complete")
    }
}
