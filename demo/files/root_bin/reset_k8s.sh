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


kubectl delete deployment -n default --all  --kubeconfig /root/.kube/config
kubectl delete cronjob -n default --all  --kubeconfig /root/.kube/config
kubectl delete statefulset -n default --all  --kubeconfig /root/.kube/config
kubectl delete service -n default --all  --kubeconfig /root/.kube/config
kubectl delete pod -n default --all  --kubeconfig /root/.kube/config

kubectl apply -f /root/k8s/ --kubeconfig /root/.kube/config

# Reset kt2l-server
rm -r /home/user/config/local/*
cp -r /root/local/* /home/user/config/local/
chown -R user:user /home/user/config/local

docker restart kt2l-server
sleep 5
docker exec kt2l-server rm -rf /app/kt2l-server/var/storage/home/users
