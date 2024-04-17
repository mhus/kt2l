#!/bin/bash
#
# This file is part of kt2l-server.
#
# kt2l-server is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# kt2l-server is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with kt2l-server.  If not, see <http://www.gnu.org/licenses/>.
#


cd "$(dirname "$0")"

. ./env.sh

JAVA_BIN=$JAVA_HOME/bin/java

exec $JAVA_BIN -Dspring.profiles.active=prod $JAVA_VM_OPTS -jar kt2l-server-0.0.1-SNAPSHOT.jar
