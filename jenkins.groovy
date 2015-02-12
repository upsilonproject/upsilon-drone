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
