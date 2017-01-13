FROM centos:7
ENV UPSILON_IDENTIFIER unnamed-node
ENV UPSILON_CONFIG_SYSTEM_AMQPHOST upsilon
ADD http://upsilon-project.co.uk/repositories/upsilon-rpm-el7/upsilon-rpm-el7.repo /etc/yum.repos.d/
ADD RPMS/noarch/upsilon-node.rpm /opt/
RUN yum localinstall /opt/upsilon-node.rpm -y
ENTRYPOINT /usr/share/upsilon-node/bin/upsilon-node
