#!/bin/bash
cd "$(dirname "$0")"
cd ../target
rm -rf launcher
mkdir launcher
cd launcher

#cp ../../src/main/resources/icons/kt2l128.png kt2l.png
#sips --resampleHeightWidth 120 120 --padToHeightWidth 175 175 \
#   kt2l.png --out kt2l-background.png
#cp -v kt2l-background.png kt2l-background-darkAqua.png
#mkdir kt2l.iconset
#sips --resampleHeightWidth 128 128 \
#   kt2l.png --out kt2l.iconset/icon_128x128.png
#iconutil --convert icns kt2l.iconset
#mkdir -p package/macos
#cp -v *.png *.icns package/macos

cp -r ../../launcher/mac/* .
cp ../kt2l-desktop-0.0.1-SNAPSHOT.jar .

jpackage \
  --name KT2L \
  --input . \
  --main-jar kt2l-desktop-0.0.1-SNAPSHOT.jar \
  --resource-dir package/macos \
  --type dmg \
  --java-options "-XstartOnFirstThread -Dspring.profiles.active=prod" \
  --icon kt2l.icns \
  --vendor "www.kt2l.org"

mv KT2L-1.0.dmg KT2L.dmg
