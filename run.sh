#!/bin/bash
 
MY_APP_HOME="/home/tonatoz/www/pong"
MY_APP_JAR="target/pong_server.jar"
 
java -Xms512m -Xmx1024m -jar $MY_APP_HOME/$MY_APP_JAR &
echo $! > $MY_APP_HOME/tmp/pids/pong_server.pid
exit 0