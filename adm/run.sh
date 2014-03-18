#!/bin/sh
cd `dirname $0`/.

CONFIG_FILE=dev-resources/config_dev.edn \
PORT=3000 \
exec $JAVA_HOME/bin/java -server -Xms256m -Xmx512m -jar omlab-standalone.jar
