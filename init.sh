#!/bin/sh
 
PATH=/bin:/usr/bin:/sbin:/usr/sbin
DAEMON=/home/tonatoz/www/pong/run.sh
PIDDIR=/home/tonatoz/www/pong/pids/
PIDFILE="$PIDDIR/pong_server.pid"
USER="tonatoz"
 
export PATH="${PATH:+$PATH:}/usr/sbin:/sbin"
 
if [ ! -d $PIDDIR ]; then
  mkdir $PIDDIR
  chown $USER.$USER $PIDDIR
fi
 
case "$1" in
  start)
    start-stop-daemon --start --quiet --oknodo --background --pidfile $PIDFILE --exec $DAEMON --chuid $USER -- $SERVICES_OPTS
    sleep 5
    $0 status
  ;;
 
  stop)
    if start-stop-daemon --stop --quiet --pidfile $PIDFILE; then
      exit 0
    else
      exit 1
    fi
  ;;
esac