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

set -x
cd "$(dirname "$0")"
VERSION=$(cat ../pom.xml | grep '<version>' | head -n 1 | sed -e 's/.*<version>\(.*\)<\/version>.*/\1/')
echo "Version: $VERSION"
cd ../target
rm -rf kt2l-server
mkdir kt2l-server
cd kt2l-server
cp -r ../../launcher/package/* .
cp ../kt2l-server-${VERSION}.jar lib/kt2l-server.jar
cp -r ../../../config .
cd ..
zip -r kt2l-server.zip kt2l-server
