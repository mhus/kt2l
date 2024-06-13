#!/bin/bash
#
# kt2l - KT2L (ktool) is a web based tool to manage your kubernetes clusters.
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
mvn clean install -B -Pproduction -Dspring.profiles.active=prod -Dvaadin.force.production.build=true || exit 1

cd kt2l-server

if [ ! -x ~/.kt2l/kt2l-server ]; then
  ./launcher/create-zip.sh
  mkdir -p ~/.kt2l
  unzip -u target/kt2l-server.zip -d ~/.kt2l
fi

cp -v target/kt2l-server-0.0.1-SNAPSHOT.jar ~/.kt2l/kt2l-server/bin

