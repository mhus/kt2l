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


if [ ! -f /root/.kube/config-minikube ]; then
  echo "Info: No kubeconfig found in /root/.kube/config-minikube"
  exit 0
fi
cp /root/.kube/config-minikube /home/user/.kube/config-minikube
cp -r /root/.minikube /home/user/.minikube
