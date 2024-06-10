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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import de.mhus.commons.tools.MString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

//@Component
@Slf4j
public class AwsS3Driver implements BucketDriver {

    public static final String NAME = "aws-s3";

    @Override
    public boolean supports(StorageConfiguration.Bucket bucket) {
        return NAME.equals(bucket.getType());
    }

    @Override
    public Storage createStorage(StorageConfiguration.Bucket bucket, String userName) {
        var path = MString.substitute(bucket.getRoot(), "username", userName);
        AWSCredentials credentials = new BasicAWSCredentials(
                bucket.getNode().getExtracted("accesskey"),
                bucket.getNode().getExtracted("secretkey")
        );
        AmazonS3 s3client = AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(Regions.fromName(bucket.getNode().getExtracted("region")))
                .build();
        return new S2Storage(s3client, path);
    }

    private static class S2Storage extends Storage {

        private final AmazonS3 client;
        private final String rootPath;

        public S2Storage(AmazonS3 client, String path) {
            this.client = client;
            this.rootPath = path;
        }

        @Override
        protected InputStream openFileStream(String path) throws IOException {
            return null;
        }

        @Override
        protected OutputStream createFileStream(String path) throws IOException {
            return null;
        }

        @Override
        public List<StorageFile> listFiles(String path) throws IOException {
            return List.of();
        }

        @Override
        public boolean isLocal() {
            return false;
        }

        @Override
        public String getLocalPath(StorageFile path) throws IOException {
            return "";
        }

        @Override
        public void delete(StorageFile file) {

        }
    }

}