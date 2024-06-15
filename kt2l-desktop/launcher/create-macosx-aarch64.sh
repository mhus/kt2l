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

cp -r ../../launcher/mac/* .
if [ -f ../kt2l-desktop-macosx-aarch64.jar ]; then
  cp ../kt2l-desktop-macosx-aarch64.jar kt2l-desktop-macosx-aarch64.jar
else
  cp ../kt2l-desktop-macosx-aarch64-0.0.1-SNAPSHOT.jar kt2l-desktop-macosx-aarch64.jar
fi

jpackage \
  --name KT2L \
  --input . \
  --main-jar kt2l-desktop-macosx-aarch64.jar \
  --resource-dir package/macos \
  --type dmg \
  --java-options "XstartOnFirstThread" \
  --java-options "Dspring.profiles.active=prod" \
  --icon kt2l.icns \
  --vendor "www.kt2l.org"

mv KT2L-1.0.dmg KT2L.dmg
