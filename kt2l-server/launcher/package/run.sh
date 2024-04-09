#!/bin/bash

cd "$(dirname "$0")"

. ./env.sh

JAVA_BIN=$JAVA_HOME/bin/java

exec $JAVA_BIN -Dspring.profiles.active=prod $JAVA_VM_OPTS -jar kt2l-server-0.0.1-SNAPSHOT.jar
