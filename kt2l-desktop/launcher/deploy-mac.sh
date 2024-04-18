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

#
# This file is part of kt2l-desktop.
#
# kt2l-desktop is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# kt2l-desktop is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with kt2l-desktop.  If not, see <http://www.gnu.org/licenses/>.
#


cd "$(dirname "$0")"
cd ../target

if [ ! -f launcher/KT2L.dmg ]; then
    echo "Fatal: KT2L.dmg not found"
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
git clone https://github.com/mhus/kt2l.git -b gh-pages gh-pages || exit 1

FILENAME=kt2l-desktop-mac-$CREATED.dmg
TITLE="Desktop Mac OSX Bundled"
DESCRIPTION="Can be executed directly in Mac OS X M1. Java JDK 21 is included."
HREF="https://kt2l-downloads.s3.eu-central-1.amazonaws.com/snapshots/$FILENAME"
# create download information
. ./gh-pages/kt2l.org/templates/download.ts.sh > download-snapshot-desktop-mac.ts

# cleanup
echo "Cleanup old snapshots in aws"
ENTRIES=$(aws s3 ls kt2l-downloads/snapshots/|cut -b 32-|grep -e ^kt2l-desktop-mac-)
echo Found $ENTRIES
if [ ! -z "$ENTRIES" ]; then
  for e in $(echo $ENTRIES); do
      echo "Delete $e"
      aws s3 rm "s3://kt2l-downloads/snapshots/$e"
  done
fi

# copy
echo "Copy KT2L.dmg to aws"
aws s3 cp ../launcher/KT2L.dmg s3://kt2l-downloads/snapshots/$FILENAME --quiet || exit 1
echo "Copy download-snapshot-desktop-mac.ts to cache"
aws s3 cp download-snapshot-desktop-mac.ts s3://kt2l-downloads/cache/downloads/download-snapshot-desktop-mac.ts --quiet || exit 1

