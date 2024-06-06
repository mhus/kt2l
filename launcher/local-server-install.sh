#!/bin/bash

cd "$(dirname "$0")"
cd ../kt2l-core
mvn clean install -B -Pproduction -Dspring.profiles.active=prod -Dvaadin.force.production.build=true || exit 1
cd ../kt2l-server
mvn clean package -B -Pproduction -Dspring.profiles.active=prod -Dvaadin.force.production.build=true || exit 1

if [ ! -x ~/.kt2l/kt2l-server ]; then
  ./launcher/create-zip.sh
  mkdir -p ~/.kt2l
  unzip -u target/kt2l-server.zip -d ~/.kt2l
fi

cp -v target/kt2l-server-0.0.1-SNAPSHOT.jar ~/.kt2l/kt2l-server/bin

