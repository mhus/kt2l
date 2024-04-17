#!/bin/bash
#
# This file is part of kt2l-desktop.
#
# kt2l-desktop is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# kt2l-desktop is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with kt2l-desktop.  If not, see <http://www.gnu.org/licenses/>.
#

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

