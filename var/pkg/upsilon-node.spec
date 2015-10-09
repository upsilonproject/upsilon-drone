Name:		upsilon-node
Version:	%{buildid_version}
Release:	%{buildid_timestamp}1%{?dist}
Summary:	Monitoring software
BuildArch:	noarch

Group:		Applications/System
License:	GPLv2
URL:		http://upsilon-project.co.uk
Source0:	upsilon-node-%{buildid_tag}.zip

BuildRequires:	java
Requires:	java

%description
Monitoring software

%prep
rm -rf $RPM_BUILD_DIR/*
%setup -q -n upsilon-node-%{buildid_tag}

%pre
/usr/bin/getent group upsilon || /usr/sbin/groupadd -r upsilon
/usr/bin/getent passwd upsilon || /usr/sbin/useradd -r -d /usr/share/upsilon-node/home/ -s /sbin/nologin upsilon

%postun
/usr/sbin/userdel upsilon

%build
mkdir -p %{buildroot}/usr/share/doc/upsilon-node
cp README.md %{buildroot}/usr/share/doc/upsilon-node/

mkdir -p %{buildroot}/usr/share/upsilon-node/lib/
cp -r lib/* %{buildroot}/usr/share/upsilon-node/lib/

mkdir -p %{buildroot}/usr/share/upsilon-node/home/

mkdir -p %{buildroot}/etc/upsilon-node/
cp etc/config.xml.sample %{buildroot}/etc/upsilon-node/
cp etc/logging.syslog.xml %{buildroot}/etc/upsilon-node/logging.xml

%if 0%{?rhel} == 6
mkdir -p %{buildroot}/etc/init.d/
cp etc/upsilon-node-rhel-init.sh %{buildroot}/etc/init.d/upsilon-node
%else 
mkdir -p %{buildroot}/lib/systemd/system/
cp etc/upsilon-node.service %{buildroot}/lib/systemd/system/
%endif

mkdir -p %{buildroot}/etc/rsyslog.d/
cp etc/upsilon.syslog.conf %{buildroot}/etc/rsyslog.d/upsilon-node

mkdir -p %{buildroot}/etc/logrotate.d/
cp etc/upsilon-node.logrotate %{buildroot}/etc/logrotate.d/upsilon-node

mkdir -p %{buildroot}/etc/yum.repos.d/
cp etc/upsilon-node-rpm-fedora.repo %{buildroot}/etc/yum.repos.d/upsilon-node.repo

%files
%doc /usr/share/doc/upsilon-node/README.md
/usr/share/upsilon-node/lib/*
/usr/share/upsilon-node/home/
%config(noreplace) /etc/upsilon-node/config.xml.sample
%config(noreplace) /etc/upsilon-node/logging.xml
%config(noreplace) /etc/logrotate.d/upsilon-node
%config(noreplace) /etc/yum.repos.d/upsilon-node.repo
%config(noreplace) /etc/rsyslog.d/upsilon-node

%if 0%{?rhel} == 6
%config(noreplace) /etc/init.d/upsilon-node
%else
%config(noreplace) /lib/systemd/system/upsilon-node.service
%endif

%changelog
* Thu Mar 05 2015 James Read <contact@jwread.com> 2.0.0-1
	Version 2.0.0
