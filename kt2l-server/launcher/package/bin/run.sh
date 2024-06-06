#!/bin/bash
#
# kt2l-server - kt2l as server
# Copyright © 2024 Mike Hummel (mh@mhus.de)
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program. If not, see <http://www.gnu.org/licenses/>.
#

cd "$(dirname "$0")"
cd ..

. ./env.sh

JAVA_BIN=$JAVA_HOME/bin/java
APP_JAR=bin/kt2l-server-0.0.1-SNAPSHOT.jar
KT2L_TMP_DIRECTORY=${KT2L_TMP_DIRECTORY:-var/tmp}

KT2L_RESTART=1
export KT2L_RESTART_POSSIBLE=true
while [ $KT2L_RESTART -eq 1 ]; do
  exec $JAVA_BIN -Dspring.profiles.active=prod $JAVA_VM_OPTS -jar $APP_JAR
  RC=$?
  KT2L_RESTART=0
  if [ $RC -eq 101 ]; then
    echo "Restarting kt2l-server"
    sleep 1
    KT2L_RESTART=1
  fi
done
