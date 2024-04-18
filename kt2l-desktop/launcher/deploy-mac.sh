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

NOW=$(date +"%Y-%m-%d")
git clone https://github.com/mhus/kt2l.git -b gh-pages gh-pages || exit 1
cd gh-pages

if [ "$(grep -c "extraheader" ../../../../.git/config)" -gt "0" ]; then
  set -x
  cd gh-pages
  pwd
  echo "[gc]" >> .git/config
  echo "	auto = 0" >> .git/config
  echo "[http \"https://github.com/\"]" >> .git/config
  cat ../../../../.git/config|grep "extraheader" >> .git/config
fi

FILENAME=kt2l-desktop-mac-$NOW.dmg
TITLE="Desktop Mac OSX Bundled"
DESCRIPTION="Can be executed directly in Mac OS X M1. Java JDK 21 is included."
HREF="https://kt2l-downloads.s3.eu-central-1.amazonaws.com/snapshots/$FILENAME"

# cleanup
echo "Cleanup old snapshots in aws"
ENTRIES=$(aws s3 ls kt2l-downloads/snapshots/|sed "s/  / /g"|cut -d ' ' -f 4|grep -e ^kt2l-desktop-mac-)
echo Found $ENTRIES
for e in $ENTRIES; do
    echo "Delete $e"
    aws s3 rm "s3://kt2l-downloads/snapshots/$e"
done

# copy
echo "Copy KT2L.dmg to aws"
aws s3 cp ../../launcher/KT2L.dmg s3://kt2l-downloads/snapshots/$FILENAME --quiet || exit 1

# create download information
CREATED=$NOW
. ./kt2l.org/templates/download.ts.sh > kt2l.org/src/downloads/download-snapshot-desktop-mac.ts

git config --global user.name 'Robot'
git config --global user.email 'mhus@users.noreply.github.com'
git commit -am "Update desktop mac snapshot $NOW"
git push
