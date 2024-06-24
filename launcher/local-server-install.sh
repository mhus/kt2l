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

while getopts hcpus flag
do
    case "${flag}" in
        h) echo "Usage: $0 [-h] [-c] [-p] [-u] [-s]"
               echo "  -h  Display this help"
               echo "  -c  Skip compile"
               echo "  -p  Skip prepare"
               echo "  -u  uninstall before install"
               echo "  -s  Start server after install"
               exit 0;;
        c) SKIP_COMPILE=true;;
        p) SKIP_PREPARE=true;;
        u) UNINSTALL=true;;
        s) START=true;;
    esac
done

cd "$(dirname "$0")"
cd ..
if [ -z "$SKIP_PREPARE" ]; then
  echo "------------------------------------------------------------"
  echo "Prepare project"
  echo "------------------------------------------------------------"
  ./launcher/prepare.sh
fi

if [ -z "$SKIP_COMPILE" ]; then
  echo "------------------------------------------------------------"
  echo "Compile project"
  echo "------------------------------------------------------------"
  mvn clean install -B -Pproduction -Dspring.profiles.active=prod -Dvaadin.force.production.build=true || exit 1
fi

cd kt2l-server

if [ -x ~/.kt2l/kt2l-server ]; then
  echo "------------------------------------------------------------"
  echo "Stop running server if exists"
  echo "------------------------------------------------------------"
  ~/.kt2l/kt2l-server/bin/service.sh zap || true

  if [ -n "$UNINSTALL" ]; then
    echo "------------------------------------------------------------"
    echo "Uninstall existing server installation"
    echo "------------------------------------------------------------"
    rm -rf ~/.kt2l/kt2l-server
  fi

fi

if [ ! -x ~/.kt2l/kt2l-server ]; then
  echo "------------------------------------------------------------"
  echo "Install server to ~/.kt2l/kt2l-server"
  echo "------------------------------------------------------------"
  ./launcher/create-zip.sh
  mkdir -p ~/.kt2l
  unzip -u target/kt2l-server.zip -d ~/.kt2l
fi

echo "------------------------------------------------------------"
echo "Copy launcher files"
echo "------------------------------------------------------------"
cp -v launcher/package/bin/* ~/.kt2l/kt2l-server/bin/
cp -v target/kt2l-server-0.0.1-SNAPSHOT.jar ~/.kt2l/kt2l-server/lib/kt2l-server.jar

if [ -n "$START" ]; then
  echo "------------------------------------------------------------"
  echo "Start server"
  echo "------------------------------------------------------------"
  ~/.kt2l/kt2l-server/bin/service.sh start
fi
