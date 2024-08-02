#!/bin/bash
#
# kt2l-desktop - kt2l desktop app
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

set -x
cd "$(dirname "$0")"
VERSION=$(cat ../pom.xml | grep '<version>' | head -n 1 | sed -e 's/.*<version>\(.*\)<\/version>.*/\1/')
echo "Version: $VERSION"
cd ../target
rm -rf launcher
mkdir launcher
cd launcher

#cp ../../src/main/resources/icons/kt2l128.png kt2l.png
#sips --resampleHeightWidth 120 120 --padToHeightWidth 175 175 \
#   kt2l.png --out kt2l-background.png
#cp -v kt2l-background.png kt2l-background-darkAqua.png
#mkdir kt2l.iconset
#sips --resampleHeightWidth 128 128 \
#   kt2l.png --out kt2l.iconset/icon_128x128.png
#iconutil --convert icns kt2l.iconset
#mkdir -p package/macos
#cp -v *.png *.icns package/macos

if [ -f ../kt2l-desktop-linux-amd64.jar ]; then
  cp ../kt2l-desktop-linux-amd64.jar kt2l-desktop-linux-amd64.jar
else
  cp ../kt2l-desktop-linux-amd64-${VERSION}.jar kt2l-desktop-linux-amd64.jar
fi

PACK_VERSION=$(echo $VERSION | cut -d . -f 1-2)
jpackage \
  --name kt2l-desktop \
  --input . \
  --main-jar kt2l-desktop-linux-amd64.jar \
  --type deb \
  --java-options "-Dspring.profiles.active=prod" \
  --app-version "$PACK_VERSION" \
  --linux-menu-group "Utility;Administration;kt2l" \
  --linux-app-category "Administration" \
  --linux-shortcut \
  --vendor "www.kt2l.org"
