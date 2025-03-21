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
if [ ! -f kt2l-container.version ]; then
    echo "Fatal: kt2l-container.version not found"
    exit 1
fi
CREATED=$(cat kt2l-container.version)
rm -rf gh-pages
git clone https://github.com/mhus/kt2l.git -b gh-pages gh-pages || exit 1

FILENAME=kt2l-container-$CREATED.zip
TITLE="Docker Container"
DESCRIPTION="Can be started as Server and accessed via Browser, locally also. Java JDK 21 is required. \\\"mhus/kt2l-server:snapshot-$CREATED\\\""
HREF=
HREF_HELP="/docs/installation/container"
. ./gh-pages/kt2l.org/templates/download.ts.sh > download-snapshot-container.ts

# copy to aws
echo "Copy download-snapshot-container.ts to cache"
aws s3 cp download-snapshot-container.ts s3://kt2l-downloads/cache/downloads/download-snapshot-container.ts --quiet || exit 1
