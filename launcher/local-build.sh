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

unset AWS_SECRET_ACCESS_KEY
unset AWS_ACCESS_KEY_ID

while getopts hcptniaof flag
do
    case "${flag}" in
        h) echo "Usage: $0 [-h] [-c] [-p] [-t] [-n] [-i] [-a] [-o] [-f]"
               echo "  -h  Display this help"
               echo "  -c  Skip compile"
               echo "  -p  Skip prepare"
               echo "  -t  Also execute tests"
               echo "  -n  Compile native"
               echo "  -i  Compile native image"
               echo "  -a  Compile all (not core only)"
               echo "  -o  Offline"
               echo "  -f  Rebuild frontend"
               exit 0;;
        c) SKIP_COMPILE=true;;
        n) COMPILE_NATIVE=true;;
        i) COMPILE_NATIVE_IMAGE=true;;
        p) SKIP_PREPARE=true;;
        t) TEST=true;;
        a) COMPLE_ALL=true;;
        o) COMPILE_OPTS="$COMPILE_OPTS -o";;
        f) REBUILD_FRONTEND=true;;
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

if [ -n "$REBUILD_FRONTEND" ]; then
  echo "------------------------------------------------------------"
  echo "Rebuild frontend"
  echo "------------------------------------------------------------"
  mvn clean vaadin:clean-frontend  -Pintegration_tests $COMPILE_OPTS
  rm package-lock.json package.json tsconfig.json types.d.ts vite.config.ts vite.generated.ts
  rm kt2l-core/package-lock.json kt2l-core/package.json kt2l-core/tsconfig.json kt2l-core/types.d.ts kt2l-core/vite.config.ts kt2l-core/vite.generated.ts kt2l-core/vite.config.ts.timestamp*
  rm kt2l-server/package-lock.json kt2l-server/package.json kt2l-server/tsconfig.json kt2l-server/types.d.ts kt2l-server/vite.config.ts kt2l-server/vite.generated.ts kt2l-server/vite.config.ts.timestamp*
  rm kt2l-desktop/package-lock.json kt2l-desktop/package.json kt2l-desktop/tsconfig.json kt2l-desktop/types.d.ts kt2l-desktop/vite.config.ts kt2l-desktop/vite.generated.ts kt2l-desktop/vite.config.ts.timestamp*
  rm kt2l-test/package-lock.json kt2l-test/package.json kt2l-test/tsconfig.json kt2l-test/types.d.ts kt2l-test/vite.config.ts kt2l-test/vite.generated.ts kt2l-test/vite.config.ts.timestamp*
  rm kt2l-native/package-lock.json kt2l-native/package.json kt2l-native/tsconfig.json kt2l-native/types.d.ts kt2l-native/vite.config.ts kt2l-native/vite.generated.ts kt2l-native/vite.config.ts.timestamp*
  mvn -DskipTests -Pintegration_tests install $COMPILE_OPTS
  mvn vaadin:build-frontend -Pintegration_tests $COMPILE_OPTS
fi

if [ -z "$SKIP_COMPILE" ]; then
  if [ -n "$COMPLE_ALL" ]; then
    echo "------------------------------------------------------------"
    echo "Compile all projects"
    echo "------------------------------------------------------------"
    mvn clean install -B -Pproduction -Dspring.profiles.active=prod -Dvaadin.force.production.build=true $COMPILE_OPTS || exit 1
  else
    echo "------------------------------------------------------------"
    echo "Compile core project"
    echo "------------------------------------------------------------"
    cd kt2l-core
    mvn clean install -B -Pproduction -Dspring.profiles.active=prod -Dvaadin.force.production.build=true $COMPILE_OPTS || exit 1
    cd ..
  fi
fi

if [ -n "$TEST" ]; then
  echo "------------------------------------------------------------"
  echo "Execute tests"
  echo "------------------------------------------------------------"
  cd kt2l-tests
  # export TEST_DEBUG=true
  mvn test $COMPILE_OPTS || exit 1
  cd ..
fi

if [ -n "$COMPILE_NATIVE" ]; then
  echo "------------------------------------------------------------"
  echo "Compile native"
  echo "------------------------------------------------------------"
  cd kt2l-native
  mvn -Pnative native:compile $COMPILE_OPTS
  cd ..
fi

if [ -n "$COMPILE_NATIVE_IMAGE" ]; then
  echo "------------------------------------------------------------"
  echo "Compile native image"
  echo "------------------------------------------------------------"
  cd kt2l-native
  mvn -Pnative -Pproduction spring-boot:build-image -Dspring-boot.build-image.imageName=kt2l-native $COMPILE_OPTS || exit 1
  cd ..
fi
