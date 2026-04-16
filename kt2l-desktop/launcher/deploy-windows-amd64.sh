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

if [ ! -f KT2L.exe ]; then
    echo "Fatal: KT2L.exe not found"
    ls -la
    exit 1
fi
if [ -z "$RELEASE_TAG" ]; then
    echo "Fatal: RELEASE_TAG not set"
    exit 1
fi

FILENAME=kt2l-desktop-windows-amd64.exe
cp KT2L.exe "$FILENAME"
gh release upload "$RELEASE_TAG" "$FILENAME" --clobber || exit 1
