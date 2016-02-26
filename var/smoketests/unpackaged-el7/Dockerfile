FROM centos:7
MAINTAINER "Upsilon Development Team"
RUN yum install java initscripts -y
RUN mkdir /usr/lib/upsilon-node/
ADD ../unpackaged-el7 /opt/
ADD ../../../bin/ /usr/lib/upsilon-node/
ADD ../../../lib/ /usr/lib/upsilon-node/
RUN mkdir /etc/upsilon-node/
ADD ../../../etc/config.xml.sample /etc/upsilon-node/
