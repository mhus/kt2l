---
sidebar_position: 1
title: Configuration Files
---

# Configuration Files

The configuration are located at the config directory and will be overwritten
each time the server starts. In this way you have in avery case the actual version of
default configuration files. If you want to overwrite a config file place it in
the `local` directory. This files will be used with priority.

## Local configuration files

The files n the `local` directory will be used before the default configuration files and
will not be overwritten. Yu can copy and modify files from the default configuration. The
directory is located in the default directory by default.

## User configuration files

You can place files in `users/USERNAME` to overwrite default and local configuration files
for a specified loggedin user. Only user related files can be overwritten.

## Configuration files

* aaa: Authorization configuration
* ai: AI configuration
* clusters: Cluster configuration
* cmd-OSSYSTEM: `macos`, `linux' and `windows` specific commands for local execution.
* help: Configure help context links
* login: Login specific configuration.
* shell: Configure which shell is user for which container image
* users: Known users and user configuration.
* views: Special configurations for views.

## Configuration directory pathes

The paths can be overwritten for the server via environment variables:

* CONFIGURATION_DIRECTORY
* CONFIGURATION_USER_DIRECTORY
* CONFIGURATION_LOCAL_DIRECTORY
