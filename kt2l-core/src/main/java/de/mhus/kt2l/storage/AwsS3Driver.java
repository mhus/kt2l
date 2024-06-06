package de.mhus.kt2l.storage;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import de.mhus.commons.tools.MString;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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