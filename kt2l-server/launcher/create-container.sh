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

if [ ! -f target/kt2l-server.zip ]; then
    echo "Fatal: kt2l-server.zip not found"
    exit 1
fi

now=$(date +"%Y%m%d%H%M%S")
docker build --progress=plain --platform linux/amd64 -t mhus/kt2l-server:snapshot-$now -f launcher/Dockerfile .
docker tag mhus/kt2l-server:snapshot-$now mhus/kt2l-server:snapshot
echo "Build image mhus/kt2l-server:snapshot-$now"
echo snapshot-$now > target/kt2l-container.version
