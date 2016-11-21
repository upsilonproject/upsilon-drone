FROM centos:7
ADD RPMS/noarch/upsilon-node.rpm /opt/
RUN yum localinstall /opt/upsilon-node.rpm -y
ENTRYPOINT /usr/share/upsilon-node/bin/upsilon-node
