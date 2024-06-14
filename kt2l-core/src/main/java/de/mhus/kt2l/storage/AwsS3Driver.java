/*
 * kt2l-core - kt2l core implementation
 * Copyright © 2024 Mike Hummel (mh@mhus.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.kt2l.storage;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import de.mhus.commons.tools.MFile;
import de.mhus.commons.tools.MString;
import de.mhus.kt2l.config.Configuration;
import io.azam.ulidj.ULID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Component
@Slf4j
public class AwsS3Driver implements BucketDriver {

    public static final String NAME = "aws-s3";

    @Autowired
    private Configuration config;


    @Override
    public boolean supports(StorageConfiguration.Bucket bucket) {
        return NAME.equals(bucket.getType());
    }

    @Override
    public Storage createStorage(StorageConfiguration.Bucket bucket, String userName) {
        var path = MString.substitute(bucket.getRoot(), "username", userName);

        AWSCredentials credentials = null;
        var propertiesFile = bucket.getNode().getExtracted("accessPropertiesFile");
        if (propertiesFile != null) {
            try {
                credentials = new PropertiesCredentials(new File(propertiesFile));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            credentials = new BasicAWSCredentials(
                    bucket.getNode().getExtracted("accessKey"),
                    bucket.getNode().getExtracted("secretKey")
            );
        }
        AmazonS3 s3client = AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(Regions.fromName(bucket.getNode().getExtracted("region")))
                .build();

        var s3Bucket = bucket.getNode().getString("bucket").get();

        var tmpDir = new File(config.getTmpDirectoryFile(), "aws_s3");
        tmpDir.mkdirs();
        return new S2Storage(s3client, path, tmpDir, s3Bucket);
    }

    private static class S2Storage extends Storage {

        private final AmazonS3 client;
        private final String rootPath;
        private final File tmpDir;
        private final String s3Bucket;

        public S2Storage(AmazonS3 client, String path, File tmpDir, String s3Bucket) {
            this.client = client;
            this.rootPath = path;
            this.tmpDir = tmpDir;
            this.s3Bucket = s3Bucket;
        }

        @Override
        protected InputStream openFileStream(String path) throws IOException {
            path = rootPath + "/" + MFile.normalizePath(path);
            try {
                S3Object o = client.getObject(s3Bucket, path);
                S3ObjectInputStream s3is = o.getObjectContent();
                return s3is;
            } catch (SdkClientException e) {
                throw new IOException(e);
            }
        }

        @Override
        protected OutputStream createFileStream(String path) throws IOException {
            path = rootPath + "/" + MFile.normalizePath(path);
            var tmpFile = new File(tmpDir, ULID.random() + ".tmp");
            return new TmpOutputStream(this, tmpFile, path);
        }

        @Override
        public List<StorageFile> listFiles(String path) throws IOException {
            path = rootPath + "/" + MFile.normalizePath(path);
            try {
                var list = client.listObjects(s3Bucket, path);
                return list.getObjectSummaries().stream()
                        .map(o -> new StorageFile(
                                this,
                                MFile.getFileDirectory(o.getKey()),
                                MFile.getFileName(o.getKey()),
                                false,
                                o.getSize(),
                                o.getLastModified().getTime()
                        )).toList();
            } catch (SdkClientException e) {
                throw new IOException(e);
            }
        }

        @Override
        public boolean isLocal() {
            return false;
        }

        @Override
        public String getLocalPath(StorageFile path) throws IOException {
            return null;
        }

        @Override
        public void delete(StorageFile file) {
            var path = rootPath + "/" + MFile.normalizePath(file.getPath());
            try {
                LOGGER.debug("Delete {}", path);
                client.deleteObject(s3Bucket, path);
            } catch (SdkClientException e) {
                LOGGER.warn("delete {} failed", path, e);
            }
        }
    }

    private static class TmpOutputStream extends FileOutputStream {
        private final S2Storage storage;
        private final File tmpFile;
        private final String path;

        public TmpOutputStream(S2Storage storage, File tmpFile, String path) throws FileNotFoundException {
            super(tmpFile);
            this.storage = storage;
            this.tmpFile = tmpFile;
            this.path = path;
        }

        @Override
        public void close() throws IOException {
            super.flush();
            super.close();
            try {
                LOGGER.debug("Upload {} to {}", tmpFile, path);
                storage.client.putObject(storage.s3Bucket, path, tmpFile);
            } catch (AmazonServiceException e) {
                throw new IOException(e);
            } finally {
                tmpFile.delete();
            }
        }
    }

}