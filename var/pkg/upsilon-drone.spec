%include SPECS/.upsilon-drone.rpmmacro

Name:		upsilon-drone
Version:	%{version_formatted_short}
Release:	%{timestamp}.%{?dist}
Summary:	Monitoring software
BuildArch:	noarch

Group:		Applications/System
License:	GPLv2
URL:		http://upsilon-project.co.uk
Source0:	upsilon-drone.zip

BuildRequires:	java
Requires:	java python upsilon-pycommon python2-pika
Conflicts: upsilon-node

%description
Monitoring software

%prep
rm -rf $RPM_BUILD_DIR/*
%setup -q -n upsilon-drone-%{tag}

%pre
/usr/bin/getent group upsilon || /usr/sbin/groupadd -r upsilon
/usr/bin/getent passwd upsilon || /usr/sbin/useradd -r -d /usr/share/upsilon-drone/home/ -s /sbin/nologin -g upsilon upsilon

%postun

if [ "$1" -eq 0 ]; then
	/usr/sbin/userdel upsilon
fi

%systemd_postun upsilon-drone.service

%post 
%systemd_post upsilon-drone.service

%build
# Docs
mkdir -p %{buildroot}/usr/share/doc/upsilon-drone
cp README.md %{buildroot}/usr/share/doc/upsilon-drone/

# Share dir 
mkdir -p %{buildroot}/usr/share/upsilon-drone/bin/
cp bin/nix-native/upsilon-drone %{buildroot}/usr/share/upsilon-drone/bin/

mkdir -p %{buildroot}/usr/share/upsilon-drone/lib/
cp -r lib/* %{buildroot}/usr/share/upsilon-drone/lib/

mkdir -p %{buildroot}/usr/share/upsilon-drone/home/

mkdir -p %{buildroot}/sbin/
cp -r var/tools/* %{buildroot}/sbin/ 

mkdir -p %{buildroot}/etc/upsilon-drone/
cp etc/config.xml.sample %{buildroot}/etc/upsilon-drone/
cp etc/logging.syslog.xml %{buildroot}/etc/upsilon-drone/logging.xml

mkdir -p %{buildroot}/etc/upsilon-drone/includes.d/
mkdir -p %{buildroot}/etc/upsilon-drone/remotes.d/

%if 0%{?rhel} == 6
mkdir -p %{buildroot}/etc/init.d/
cp etc/upsilon-drone-rhel-init.sh %{buildroot}/etc/init.d/upsilon-drone
%else 
mkdir -p %{buildroot}/lib/systemd/system/
cp etc/upsilon-drone.service %{buildroot}/lib/systemd/system/
%endif

mkdir -p %{buildroot}/etc/rsyslog.d/
cp etc/upsilon.syslog.conf %{buildroot}/etc/rsyslog.d/upsilon-drone

mkdir -p %{buildroot}/etc/logrotate.d/
cp etc/upsilon-drone.logrotate %{buildroot}/etc/logrotate.d/upsilon-drone

mkdir -p %{buildroot}/usr/share/man1/
cp etc/manpage/*.gz %{buildroot}/usr/share/man1/


%files
%doc /usr/share/doc/upsilon-drone/README.md
/usr/share/upsilon-drone/bin/*
/usr/share/upsilon-drone/lib/*
%attr(700, upsilon, upsilon)/usr/share/upsilon-drone/home/
/sbin/*
%attr(755, upsilon, upsilon) /etc/upsilon-drone/
%attr(644, upsilon, upsilon) /etc/upsilon-drone/*
%config(noreplace) /etc/upsilon-drone/config.xml.sample
%config(noreplace) /etc/upsilon-drone/logging.xml
%attr(755, upsilon, upsilon) /etc/upsilon-drone/includes.d/
%attr(755, upsilon, upsilon) /etc/upsilon-drone/remotes.d/
%config(noreplace) /etc/logrotate.d/upsilon-drone
%config(noreplace) /etc/rsyslog.d/upsilon-drone
%doc /usr/share/man1/*.gz

%if 0%{?rhel} == 6
%config(noreplace) /etc/init.d/upsilon-drone
%else
%config(noreplace) /lib/systemd/system/upsilon-drone.service
%endif

%changelog
* Thu Mar 05 2015 James Read <contact@jwread.com> 2.0.0-1
	Version 2.0.0
