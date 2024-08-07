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

if [ ! -f kt2l-server.zip ]; then
    echo "Fatal: kt2l-server.zip not found"
    exit 1
fi

if [ -z "$AWS_ACCESS_KEY_ID" ]; then
    echo "Fatal: AWS_ACCESS_KEY_ID not correct set"
    exit 1
fi
if [ -z "$AWS_SECRET_ACCESS_KEY" ]; then
    echo "Fatal: AWS_SECRET_ACCESS_KEY not correct set"
    exit 1
fi

rm -rf deploy
mkdir deploy
cd deploy


CREATED=$(date +"%Y-%m-%d")
git clone https://github.com/mhus/kt2l.git -b gh-pages gh-pages || exit 1

FILENAME=kt2l-server-$CREATED.zip
TITLE="Server ZIP"
DESCRIPTION="Can be started as Server and accessed via Browser, locally also. Java JDK 21 is required."
HREF="https://kt2l-downloads.s3.eu-central-1.amazonaws.com/snapshots/$FILENAME"
HREF_HELP="/docs/installation/server"
SIZE=$(echo $(du -m ../kt2l-server.zip)|cut -d ' ' -f 1)MB
. ./gh-pages/kt2l.org/templates/download.ts.sh > download-snapshot-server.ts

# cleanup
echo "Cleanup old snapshots in aws"
ENTRIES=$(aws s3 ls kt2l-downloads/snapshots/|cut -b 32-|grep -e ^kt2l-server-)
echo Found $ENTRIES
if [ ! -z "$ENTRIES" ]; then
    for e in $(echo $ENTRIES); do
        echo "Delete $e"
        aws s3 rm "s3://kt2l-downloads/snapshots/$e"
    done
fi

# copy to aws
echo "Copy kt2l-server.zip to aws"
aws s3 cp ../kt2l-server.zip s3://kt2l-downloads/snapshots/$FILENAME --quiet || exit 1
echo "Copy kt2l-server.jar to aws cache"
aws s3 cp ../kt2l-server-${VERSION}.jar s3://kt2l-downloads/cache/kt2l-server.jar --quiet || exit 1
echo "Copy download-snapshot-server.ts to cache"
aws s3 cp download-snapshot-server.ts s3://kt2l-downloads/cache/downloads/download-snapshot-server.ts --quiet || exit 1

