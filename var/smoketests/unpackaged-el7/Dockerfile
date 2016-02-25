FROM centos:7
MAINTAINER "Upsilon Development Team"
RUN yum install java initscripts -y
run mkdir /usr/lib/upsilon-node/
ADD ../../../upsilon-node-*/bin/ /usr/lib/upsilon-node/
ADD ../../../upsilon-node-*/lib/ /usr/lib/upsilon-node/
RUN mkdir /etc/upsilon-node/
ADD ../../../upsilon-node-*/etc/config.xml.sample /etc/upsilon-node/
