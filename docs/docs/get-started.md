---
sidebar_position: 5
---

# Get Started

## Which kind of installation do you need

* ***Local Server***: You can run KT2L as java server and access it with the browser. The server is secured not to allow 
  connections form others the localhost if you want. The server is doing autologin if accessing from localhost.
* ***Remote Server***: You can install the software on a remote server and use the kube connectivity there, it is 
  recommended to add a SSL layer.
* ***Containerized Bundle***: You can also use the containerized version of the software. This could be problematic
  if you try to access GKE or EKS clouds.
* ***Kubernetes Container***: You can also use the software as a kubernetes container. This will allow you to manage the cluster
  from within the cluster or allow non admin users to access the cluster.
* ***Local Mac OSX (M1) Bundle****: This is a software bundle with the server, Java RJE and a Browser Application. It will run
  as mac native application.
* ***Local Windows Bundle****: This is a windows executable bundle with the server, Java JRE and a Browser Application. It
  will run as windows native application.

## Download

Download the latest snapshot from the website [KT2L Website](https://kt2l.org).

## Requirements

Expect of the Bundles, Java JRE 21 is required to run the software. You can download and install
it from [Temurin Latest Release](https://adoptium.net/de/temurin/releases/).
