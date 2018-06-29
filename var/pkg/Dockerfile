FROM centos:7

LABEL io.openshift.non-scalable true

#RUN yum install -y https://dl.fedoraproject.org/pub/epel/7/x86_64/e/epel-release-7-10.noarch.rpm
RUN yum install -y epel-release 
RUN yum install -y nagios-plugins-ping
ADD http://repos.upsilonproject.io/upsilon-rpm-el7/upsilon-rpm-el7.repo /etc/yum.repos.d/
ADD RPMS/noarch/upsilon-drone.rpm /opt/
RUN yum localinstall /opt/upsilon-drone.rpm -y

ENTRYPOINT /usr/share/upsilon-drone/bin/upsilon-drone

USER upsilon
