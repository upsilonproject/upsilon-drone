FROM centos:7
MAINTAINER "Upsilon Development Team"
ENTRYPOINT ["/usr/share/upsilon-node/bin/upsilon-node"]
RUN yum install java initscripts rabbitmq-server -y
RUN mkdir /usr/share/upsilon-node/
ADD bin/nix-native/upsilon-node /usr/share/upsilon-node/bin/upsilon-node
ADD lib /usr/share/upsilon-node/lib/
ADD bin/tools/ /usr/share/upsilon-node/bin/tools
RUN mkdir /etc/upsilon-node/
ADD etc/config.xml /etc/upsilon-node/
