---
sidebar_position: 8
---

# Desktop

The desktop is a bundled server version with a browser frontend and a java SDK.
They are all together bundled to a desktop application for the target OS.

## Mac OSX Bundle

The bundle is a dmg image file which contains the application. If you install
the application in /Applications you can run it like each other application.

It will use autologin and create a `.kt2l` directory in user home by default.

## Windows Bundle

The bundle is a exe file which contains the application. If you download it
you can use it as native windows application.

It will use autologin and create a `.kt2l` directory in user home by default.

## Linux DEB

Linux distributions supporting deb packages can use the deb package to install the desktop.
To install the latest snapshot version of the desktop, add the following repository to your system:

```bash
echo "deb [trusted=yes] https://apt.kt2l.org/ /" | sudo tee /etc/apt/sources.list.d/kt2l.list
sudo apt update
sudo apt install kt2l-desktop
```
