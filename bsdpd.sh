#!/bin/bash
#-outfile SYSLOG -errfile SYSLOG
jsvc -cp BSDPServer.jar:./lib/xercesImpl.jar:./lib/commons-daemon-1.0.2.jar -pidfile /var/run/bsdpd.pid -nodetach -debug de.upb.phys.bsdpd.BSDPDaemon $@
