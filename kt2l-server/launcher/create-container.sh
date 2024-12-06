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
cd ..

if [ ! -f target/kt2l-server.zip ]; then
    echo "Fatal: kt2l-server.zip not found"
    exit 1
fi

REGISTRY_URL="https://index.docker.io/v1/"
docker login "$REGISTRY_URL" -u "$DOCKER_USERNAME" -p "$DOCKER_PASSWORD"

CREATED=$(date +"%Y-%m-%d")
docker buildx build --progress=plain --platform linux/amd64,linux/arm64 -t mhus/kt2l-server:snapshot-$CREATED -f launcher/Dockerfile --push . || exit 1

docker manifest create mhus/kt2l-server:snapshot mhus/kt2l-server:snapshot-$CREATED || exit 1
docker manifest push mhus/kt2l-server:snapshot || exit 1

echo "Build image mhus/kt2l-server:snapshot-$CREATED"
echo snapshot-$CREATED > target/kt2l-container.version
