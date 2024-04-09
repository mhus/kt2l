#!/bin/bash

cd "$(dirname "$0")"
cd ../target

if [ ! -f launcher/KT2L.dmg ]; then
    echo "Fatal: KT2L.dmg not found"
    exit 1
fi

if [ "x$AWS_ACCESS_KEY_ID" -ne "xAKIASJLQRE2PBBXZ34PC" ]; then
    echo "Fatal: AWS_ACCESS_KEY_ID not correct set"
    exit 1
fi

rm -rf deploy
mkdir deploy
cd deploy

NOW=$(date +"%Y-%m-%d")
git clone https://github.com/mhus/kt2l.git -b gh-pages gh-pages || exit 1
cd gh-pages

FILENAME=kt2l-desktop-mac-$NOW.dmg
TITLE="Desktop Mac OSX Bundled"
DESCRIPTION="Can be executed directly in Mac OS X M1. Java JDK 21 is included."
HREF="https://kt2l-downloads.s3.eu-central-1.amazonaws.com/snapshots/$FILENAME"

# cleanup
echo "Cleanup old snapshots in aws"
ENTRIES=$(aws s3 ls kt2l-downloads/snapshots/|sed "s/  / /g"|cut -d ' ' -f 4|grep -e ^kt2l-desktop-mac-)
echo Found $ENTRIES
for (e in $ENTRIES); do
    echo "Delete $e"
    aws s3 rm "s3://kt2l-downloads/snapshots/$e"
done

# copy
echo "Copy KT2L.dmg to aws"
aws s3 cp ../../launcher/KT2L.dmg s3://kt2l-downloads/snapshots/$FILENAME || exit 1

# create download information
cat kt2l.org/templates/download.ts \
 | sed s/xhrefx/$HREF/g \
 | sed s/xcreatedx/$NOW/g \
 | sed s/xtitlex/"$TITLE"/g \
 | sed s/xdescriptionx/"$DESCRIPTION"/g \
 > kt2l.org/src/downloads/download-snapshot-desktop-mac.ts  || exit 1

git add ./kt2l.org/src/downloads/download-snapshot-desktop-mac.ts
git commit -m "Update desktop mac snapshot $NOW"
git push

