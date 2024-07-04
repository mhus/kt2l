#!/bin/bash
#
# kt2l - KT2L (ktool) is a web based tool to manage your kubernetes clusters.
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

while getopts hcpus flag
do
    case "${flag}" in
        h) echo "Usage: $0 [-h] [-c] [-p]"
               echo "  -h  Display this help"
               echo "  -c  Skip compile"
               echo "  -p  Skip prepare"
               exit 0;;
        c) SKIP_COMPILE=true;;
        p) SKIP_PREPARE=true;;
    esac
done

cd "$(dirname "$0")"
cd ..
if [ -z "$SKIP_PREPARE" ]; then
  echo "------------------------------------------------------------"
  echo "Prepare project"
  echo "------------------------------------------------------------"
  ./launcher/prepare.sh
fi

if [ -z "$SKIP_COMPILE" ]; then
  echo "------------------------------------------------------------"
  echo "Compile project"
  echo "------------------------------------------------------------"
  mvn clean install -B -Pproduction -Dspring.profiles.active=prod -Dvaadin.force.production.build=true || exit 1
fi

cd kt2l-server
./launcher/create-zip.sh
./launcher/create-container.sh
