#!/bin/bash
set -x
cd "$(dirname "$0")"
cd ../target

if [ ! -f kt2l-server.zip ]; then
    echo "Fatal: kt2l-server.zip not found"
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

echo "====================================="
cat .git/config
echo "====================================="

FILENAME=kt2l-server-$NOW.zip
TITLE="Server Bundled"
DESCRIPTION="Can be started as Server and accessed via Browser, locally also. Java JDK 21 is required."
HREF="https://kt2l-downloads.s3.eu-central-1.amazonaws.com/snapshots/$FILENAME"

# cleanup
echo "Cleanup old snapshots in aws"
ENTRIES=$(aws s3 ls kt2l-downloads/snapshots/|sed "s/  / /g"|cut -d ' ' -f 4|grep -e ^kt2l-server)
echo Found $ENTRIES
for e in "$ENTRIES"; do
    echo "Delete $e"
    aws s3 rm "s3://kt2l-downloads/snapshots/$e"
done

# copy
echo "Copy kt2l-server.zip to aws"
aws s3 cp ../../kt2l-server.zip s3://kt2l-downloads/snapshots/$FILENAME || exit 1

# create download information
cat kt2l.org/templates/download.ts \
 | sed s/xhrefx/"$HREF"/g \
 | sed s/xcreatedx/"$NOW"/g \
 | sed s/xtitlex/"$TITLE"/g \
 | sed s/xdescriptionx/"$DESCRIPTION"/g \
 > kt2l.org/src/downloads/download-snapshot-server.ts  || exit 1

git config --global user.name 'Robot'
git config --global user.email 'mhus@users.noreply.github.com'
git commit -am "Update server snapshot $NOW"
git push

