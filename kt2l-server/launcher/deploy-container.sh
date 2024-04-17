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

set -x
cd "$(dirname "$0")"
cd ../target

if [ ! -f kt2l-server.zip ]; then
    echo "Fatal: kt2l-server.zip not found"
    exit 1
fi

if [ -z "$DOCKER_USERNAME" ]; then
    echo "Fatal: DOCKER_USERNAME not correct set"
    exit 1
fi
if [ -z "$DOCKER_PASSWORD" ]; then
    echo "Fatal: DOCKER_PASSWORD not correct set"
    exit 1
fi


NOW=$(date +"%Y-%m-%d")
rm -rf gh-pages
git clone https://github.com/mhus/kt2l.git -b gh-pages gh-pages || exit 1
cd gh-pages

if [ "$(grep -c "extraheader" ../../../.git/config)" -gt "0" ]; then
  set -x
  cd gh-pages
  pwd
  echo "[gc]" >> .git/config
  echo "	auto = 0" >> .git/config
  echo "[http \"https://github.com/\"]" >> .git/config
  cat ../../../.git/config|grep "extraheader" >> .git/config
fi

cd ../..

FILENAME=kt2l-container-$NOW.zip
TITLE="Docker Container"
DESCRIPTION="Can be started as Server and accessed via Browser, locally also. Java JDK 21 is required."
HREF="/docs/run-container"

REGISTRY_URL="https://index.docker.io/v1/"
docker login "$REGISTRY_URL" -u "$DOCKER_USERNAME" -p "$DOCKER_PASSWORD"

docker push mhus/kt2l-server:snapshot

# create download information
CREATED=$NOW
cd target/gh-pages
. ./kt2l.org/templates/download.ts.sh > kt2l.org/src/downloads/download-snapshot-container.ts

git add kt2l.org/src/downloads/download-snapshot-container.ts
git config --global user.name 'Robot'
git config --global user.email 'mhus@users.noreply.github.com'
git commit -m "Update container snapshot $NOW"
git push

