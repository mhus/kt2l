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

unset AWS_SECRET_ACCESS_KEY
unset AWS_ACCESS_KEY_ID

while getopts hcpust flag
do
    case "${flag}" in
        h) echo "Usage: $0 [-h] [-c] [-p] [-u] [-s] [-t]"
               echo "  -h  Display this help"
               echo "  -c  Skip compile"
               echo "  -p  Skip prepare"
               echo "  -s  Open .dmg file"
               echo "  -t  Also execute tests"
               exit 0;;
        c) SKIP_COMPILE=true;;
        p) SKIP_PREPARE=true;;
        s) START=true;;
        t) TEST=true;;
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

if [ -n "$TEST" ]; then
  echo "------------------------------------------------------------"
  echo "Execute tests"
  echo "------------------------------------------------------------"
  cd kt2l-tests
  # export TEST_DEBUG=true
  mvn test || exit 1
  cd ..
fi

cd kt2l-desktop

if [ -z "$SKIP_COMPILE" ]; then
  echo "------------------------------------------------------------"
  echo "Compile desktop"
  echo "------------------------------------------------------------"
  mvn clean install -B -Pmacosx-aarch64 -Pproduction -Dspring.profiles.active=prod -Dvaadin.force.production.build=true || exit 1
fi

VERSION=$(cat pom.xml | grep '<version>' | head -n 1 | sed -e 's/.*<version>\(.*\)<\/version>.*/\1/')
PACK_VERSION=$(echo $VERSION | cut -d - -f 1)
if [[ "$PACK_VERSION" =~ ^0.* ]]; then
  PACK_VERSION=1
fi
echo "Version: $VERSION - Pack Version: $PACK_VERSION"

cd target
rm -rf launcher
mkdir launcher
cd launcher

cp -r ../../launcher/mac/* .
if [ -f ../kt2l-desktop-macosx-aarch64.jar ]; then
  cp ../kt2l-desktop-macosx-aarch64.jar kt2l-desktop-macosx-aarch64.jar
else
  cp ../kt2l-desktop-macosx-aarch64-${VERSION}.jar kt2l-desktop-macosx-aarch64.jar
fi

jpackage \
  --name KT2L \
  --input . \
  --main-jar kt2l-desktop-macosx-aarch64.jar \
  --resource-dir package/macos \
  --app-version "$PACK_VERSION" \
  --type dmg \
  --java-options "-XstartOnFirstThread" \
  --java-options "-Dspring.profiles.active=prod" \
  --java-options "--add-opens java.base/java.util=ALL-UNNAMED" \
  --java-options "--add-opens java.base/java.lang=ALL-UNNAMED" \
  --icon kt2l.icns \
  --vendor "www.kt2l.org"

mv KT2L-${PACK_VERSION}.dmg KT2L.dmg

if [ -n "$START" ]; then
  echo "------------------------------------------------------------"
  echo "Open .dmg file"
  echo "------------------------------------------------------------"
  open KT2L.dmg
fi
