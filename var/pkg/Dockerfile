FROM centos:7

LABEL io.openshift.expose-services 4000:http
LABEL io.openshift.non-scalable true

RUN yum install -y https://dl.fedoraproject.org/pub/epel/7/x86_64/e/epel-release-7-9.noarch.rpm
ADD http://upsilon-project.co.uk/repositories/upsilon-rpm-el7/upsilon-rpm-el7.repo /etc/yum.repos.d/
ADD RPMS/noarch/upsilon-node.rpm /opt/
RUN yum localinstall /opt/upsilon-node.rpm -y

ENTRYPOINT /usr/share/upsilon-node/bin/upsilon-node
