FROM centos:7

LABEL io.openshift.expose-services 4000:http
LABEL io.openshift.non-scalable true

ADD http://upsilon-project.co.uk/repositories/upsilon-rpm-el7/upsilon-rpm-el7.repo /etc/yum.repos.d/
ADD RPMS/noarch/upsilon-node.rpm /opt/
RUN yum localinstall /opt/upsilon-node.rpm -y

ENTRYPOINT /usr/share/upsilon-node/bin/upsilon-node
