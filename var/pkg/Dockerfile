FROM centos:7
RUN yum install unzip -y
RUN unzip upsilon-node.zip /opt/
RUN mv /opt/upsilon-node-* /opt/upsilon-node
ENTRYPOINT /opt/upsilon-node/share/upsilon-node/bin/upsilon-node
