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
