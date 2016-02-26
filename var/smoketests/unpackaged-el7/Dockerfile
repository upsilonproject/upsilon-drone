FROM centos:7
MAINTAINER "Upsilon Development Team"
RUN yum install java initscripts rabbitmq-server -y
RUN mkdir /usr/share/upsilon-node/
ADD bin/nix-native/upsilon-node /usr/share/upsilon-node/bin/upsilon-node
ADD lib /usr/share/upsilon-node/lib/
ADD var/tools/ /usr/share/upsilon-node/bin/tools
RUN mkdir /etc/upsilon-node/
ADD config.xml /etc/upsilon-node/
