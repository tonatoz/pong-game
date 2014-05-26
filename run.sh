#!/bin/bash
 
MY_APP_HOME="/home/tonatoz/www/pong"
MY_APP_JAR="target/pong_server.jar"
PIDFILE="$MY_APP_HOME/pids/pong_server.pid"
 
java -Xms512m -Xmx1024m -jar $MY_APP_HOME/$MY_APP_JAR &
echo $! > $PIDFILE
exit 0