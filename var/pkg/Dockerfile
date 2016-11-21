FROM centos:7
ADD RPMS/noarch/upsilon-node.rpm /opt/
RUN rpm -U /opt/upsilon-node.rpm
ENTRYPOINT /usr/share/upsilon-node/bin/upsilon-node
