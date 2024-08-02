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

ARCH="macosx-aarch64 macosx-x86_64 windows-amd64 linux-amd64"

for A in $ARCH; do
    if [ ! -f kt2l-desktop-$A-$VERSION.jar ]; then
        echo "Warning: kt2l-desktop-$A-$VERSION.jar not found"
    fi
done

if [ -z "$AWS_ACCESS_KEY_ID" ]; then
    echo "Fatal: AWS_ACCESS_KEY_ID not correct set"
    exit 1
fi
if [ -z "$AWS_SECRET_ACCESS_KEY" ]; then
    echo "Fatal: AWS_SECRET_ACCESS_KEY not correct set"
    exit 1
fi

for A in $ARCH; do
    if [ -f kt2l-desktop-$A-$VERSION.jar ]; then
        echo "Copy: kt2l-desktop-$A-$VERSION.jar to cache"
        echo $VERSION $(date +"%Y-%m-%d") > kt2l-desktop-$A.txt
        aws s3 cp kt2l-desktop-$A-$VERSION.jar s3://kt2l-downloads/cache/kt2l-desktop-$A.jar --quiet || exit 1
        aws s3 cp kt2l-desktop-$A.txt s3://kt2l-downloads/cache/kt2l-desktop-$A.txt --quiet || exit 1
    fi
done

# Release
if [[ ${VERSION} != *"SNAPSHOT"* ]];then

  for A in $ARCH; do
      if [ -f kt2l-desktop-$A-$VERSION.jar ]; then
          echo "Copy: kt2l-desktop-$A-$VERSION.jar to release"
          echo $VERSION $(date +"%Y-%m-%d") > kt2l-desktop-$A.txt
          aws s3 cp kt2l-desktop-$A-$VERSION.jar s3://kt2l-downloads/releases/kt2l-desktop-$A.jar --quiet || exit 1
      fi
  done

fi