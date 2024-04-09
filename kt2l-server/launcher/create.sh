#!/bin/bash

cd "$(dirname "$0")"
cd ../target
rm -rf kt2l-server
mkdir kt2l-server
cd kt2l-server
cp ../kt2l-server-0.0.1-SNAPSHOT.jar .
cp -r ../../launcher/package/* .
cp -r ../../../config .
cd ..
zip -r kt2l-server.zip kt2l-server
#tar -czf kt2l-server.tar.gz kt2l-server

cd ..
docker build --progress=plain --platform linux/amd64 -t kt2l-server:snapshot -f launcher/Dockerfile .
