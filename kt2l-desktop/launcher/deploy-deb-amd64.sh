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

cd "$(dirname "$0")"
cd ../target

if [ ! -f launcher/kt2l-desktop_1.0_amd64.deb ]; then
    echo "Fatal: kt2l-desktop_1.0_amd64.deb not found"
    exit 1
fi
if [ -z "$AWS_ACCESS_KEY_ID" ]; then
    echo "Fatal: AWS_ACCESS_KEY_ID not correct set"
    exit 1
fi
if [ -z "$AWS_SECRET_ACCESS_KEY" ]; then
    echo "Fatal: AWS_ACCESS_KEY_ID not correct set"
    exit 1
fi

rm -rf deploy
mkdir deploy
cd deploy

CREATED=$(date +"%Y-%m-%d")
if [ -f kt2l-desktop-linux-amd64.txt ]; then
  CREATED=$(cat kt2l-desktop-linux-amd64.txt|cut -d ' ' -f 2)
  echo "Use existing date $CREATED"
fi
git clone https://github.com/mhus/kt2l.git -b gh-pages gh-pages || exit 1

FILENAME=kt2l-desktop-linux-amd64_${CREATED}_amd64.deb
TITLE="Desktop Linux amd64 DEB"
DESCRIPTION="Can be installed on debian amd64 systems"
HREF="https://kt2l-downloads.s3.eu-central-1.amazonaws.com/snapshots/$FILENAME"
HREF_HELP="/docs/installation/desktop#linux-deb"
SIZE=$(echo $(du -m ../launcher/kt2l-desktop_1.0_amd64.deb)|cut -d ' ' -f 1)MB
# create download information
. ./gh-pages/kt2l.org/templates/download.ts.sh > download-snapshot-desktop-linux-amd64.ts

# cleanup
echo "Cleanup old snapshots in aws"
ENTRIES=$(aws s3 ls kt2l-downloads/snapshots/|cut -b 32-|grep -e ^kt2l-desktop-linux-amd64)
echo Found $ENTRIES
if [ ! -z "$ENTRIES" ]; then
  for e in $(echo $ENTRIES); do
      echo "Delete $e"
      aws s3 rm "s3://kt2l-downloads/snapshots/$e"
  done
fi

# copy
echo "Copy kt2l-desktop_1.0_amd64.deb to aws $FILENAME"
aws s3 cp ../launcher/kt2l-desktop_1.0_amd64.deb s3://kt2l-downloads/snapshots/$FILENAME --quiet || exit 1
echo "Copy download-snapshot-desktop-linux-amd64.ts to cache"
aws s3 cp download-snapshot-desktop-linux-amd64.ts s3://kt2l-downloads/cache/downloads/download-snapshot-desktop-linux-amd64.ts --quiet || exit 1

