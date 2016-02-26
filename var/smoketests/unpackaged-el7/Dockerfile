FROM centos:7
MAINTAINER "Upsilon Development Team"
RUN yum install java initscripts rabbitmq-server -y
RUN mkdir /usr/share/upsilon-node/
ADD bin /usr/share/upsilon-node/bin/
ADD lib /usr/share/upsilon-node/lib/
RUN mkdir /etc/upsilon-node/
ADD etc/config.xml.sample /etc/upsilon-node/config.xml
