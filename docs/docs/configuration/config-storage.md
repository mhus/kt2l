---
sidebar_position: 12
title: Storage
---

# Storage Configuration

This section describes the configuration options available for the storage system. Storage is where
files are written to. The storage is accessible via the Storage Panel.

Configuring storage means to configure storage for users. Storage can be per user or shared among users. To
do so you need to configure the storage buckets. A bucket is a storage location where files are stored.

You can configure multiple buckets and assign them to users. You can also configure the default bucket.

```yaml
buckets:
  - name: common
    users:
      - autoload
    type: directory
    root: ${home}/home/common
  - name: default
    type: directory
    root: ${home}/home/users/${username}
```

To find a bucket for a user all buckets will be processed in the order they are defined in the configuration. If
no `users` key is defined, the bucket will be used for all users. Else the bucket will be used for the defined 
users. In the `root` key you can define the start directory for the user in the bucket (depending on the bucket type).  

## Configuration Options

The following configuration options are available for the storage system:

- `type` The bucket implementation.
- `root` The root directory in the bucket.
- `users` The users that can access the bucket.

## Directory Buckets

The directory bucket is a simple bucket implementation that stores files in a directory.

Placeholders:

- `${home}` The home directory of the server.
- `${username}` The username of the user.

## AWS S3 Buckets

The AWS S3 bucket is a bucket implementation that stores files in an AWS S3 bucket.

Placeholders:

- `${username}` The username of the user.

Additional Properties:

- `accessPropertiesFile` The path to the AWS properties file.
- `accessKey` The AWS access key (if accessPropertiesFile is not set).
- `secretKey` The AWS secret key (if accessPropertiesFile is not set).
- `region` The AWS region.
- `bucket` The AWS S3 bucket name.
