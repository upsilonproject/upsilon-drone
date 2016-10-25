FROM centos:7
RUN curl http://upsilon-project.co.uk/repositories/upsilon-rpm-el7/upsilon-rpm-el7.repo > /etc/yum.repos.d/upsilon-rpm-el7.repo
RUN yum install upsilon-node -y
ENTRYPOINT /usr/share/upsilon-node/bin/upsilon-node
