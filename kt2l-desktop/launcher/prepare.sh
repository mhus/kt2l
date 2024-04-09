#!/bin/bash
cd "$(dirname "$0")"
cd ..

echo "====================================="
cat ../.git/config
echo "====================================="

mkdir target
cd target
rm -rf config
cp -r ../../config .
rm -rf config/users/*
cd config
zip -r ../config.zip *
cd ../..
cp target/config.zip ../kt2l-core/src/main/resources/config.zip

