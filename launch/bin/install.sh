#!/bin/bash
#
# This file is part of kt2l.
#
# kt2l is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# kt2l is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with kt2l.  If not, see <http://www.gnu.org/licenses/>.
#

# config

REPO_PATH_ZIP="de/mhus/kt2l/0.0.1-SNAPSHOT/kt2l-0.0.1-SNAPSHOT-install.zip"
LOCAL_REPO_PATH_ZIP="$HOME/.m2/repository/$REPO_PATH_ZIP"
REMOTE_REPO_PATH_ZIP="https://repo1.maven.org/maven2/$REPO_PATH_ZIP"

# init

if [ ! -d $HOME/.kt2l/bin/0.0.1-SNAPSHOT ]; then
  mkdir -p $HOME/.kt2l/bin/0.0.1-SNAPSHOT
fi
if [ ! -d $HOME/.kt2l/tmp ]; then
  mkdir -p $HOME/.kt2l/tmp
fi

# download

if [ ! -e $LOCAL_REPO_PATH_ZIP ]; then
  if command -v mvn &> /dev/null; then
    mvn org.apache.maven.plugins:maven-dependency-plugin:3.1.2:get \
      -Dartifact=de.mhus:kt2l:0.0.1-SNAPSHOT:zip:install

  elif command -v curl &> /dev/null; then
    if [ -e $HOME/.kt2l/tmp/kt2l-install.zip ]; then
      rm $HOME/.kt2l/tmp/kt2l-install.zip
    fi
    curl --output $HOME/.kt2l/tmp/kt2l-install.zip $REMOTE_REPO_PATH_ZIP
    LOCAL_REPO_PATH_ZIP=$HOME/.kt2l/tmp/kt2l-install.zip
  else
     echo "Either mvn nor curl found - exit"
     exit 1
  fi
fi

if [ ! -e $LOCAL_REPO_PATH_ZIP ]; then
  echo "Can't download kt2l install zip"
  echo $REMOTE_REPO_PATH_ZIP
  exit 1
fi

# unpack and setup

cd $HOME/.kt2l/bin/0.0.1-SNAPSHOT
unzip -o $LOCAL_REPO_PATH_ZIP
chmod +x $HOME/.kt2l/bin/0.0.1-SNAPSHOT/*.sh

if [ -e $HOME/.kt2l/bin/kt2l ]; then
  rm $HOME/.kt2l/bin/kt2l
fi
ln -s $HOME/.kt2l/bin/0.0.1-SNAPSHOT/kt2l.sh $HOME/.kt2l/bin/kt2l

# cleanup

if [ -e $HOME/.kt2l/tmp/kt2l-install.zip ]; then
  rm $HOME/.kt2l/tmp/kt2l-install.zip
fi

echo "Installed 0.0.1-SNAPSHOT in $HOME/.kt2l"
echo "Add directory $HOME/.kt2l/bin to \$PATH (export PATH=$PATH:$HOME/.kt2l/bin/kt2l) or link $HOME/.kt2l/bin/kt2l in a binary directory like /usr/local/bin"
