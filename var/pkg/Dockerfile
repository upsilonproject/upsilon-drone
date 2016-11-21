FROM centos:7
ADD http://upsilon-project.co.uk/repositories/upsilon-rpm-el7/upsilon-rpm-el7.repo /etc/yum.repos.d/
ADD RPMS/noarch/upsilon-node.rpm /opt/
RUN yum localinstall /opt/upsilon-node.rpm -y
ENTRYPOINT /usr/share/upsilon-node/bin/upsilon-node
