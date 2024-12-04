/*
 * kt2l-test - kt2l integration tests
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
package de.mhus.kt2l;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import de.mhus.commons.errors.MException;
import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.config.Configuration;
import de.mhus.kt2l.storage.AwsS3Driver;
import de.mhus.kt2l.storage.Storage;
import de.mhus.kt2l.storage.StorageConfiguration;
import de.mhus.kt2l.util.TestResultDebugWatcher;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.ReflectionUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Set;

@Slf4j
@ExtendWith(TestResultDebugWatcher.class)
public class AwsS3DriverTest {

    @Test
    public void testS3Access() throws MException, IllegalAccessException, IOException {
        if (System.getenv("AWS_ACCESS_KEY_ID") == null) {
            LOGGER.warn("Skip test, no AWS_ACCESS_KEY_ID");
            return;
        }

        var credentials = new BasicAWSCredentials(
                System.getenv("AWS_ACCESS_KEY_ID"),
                System.getenv("AWS_SECRET_ACCESS_KEY"));

        AmazonS3 s3client = AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(Regions.fromName("eu-central-1"))
                .build();

        s3client.listObjects("kt2l-itest", "/").getObjectSummaries().forEach(o -> {
            System.out.println(o.getKey());
        });

    }

    @Test
    public void testS3Driver() throws MException, IllegalAccessException, IOException {

        if (System.getenv("AWS_ACCESS_KEY_ID") == null) {
            LOGGER.warn("Skip test, no AWS_ACCESS_KEY_ID");
            return;
        }

        var storage = createStorage();

        FsDriverTest.testScenario(storage);

    }

    private Storage createStorage() throws IllegalAccessException, MException {
        var driver = createAwsS3Driver();
        StorageConfiguration.Bucket bucket = new StorageConfiguration.Bucket(Set.of("aws-s3"), "directory", "/itest",
                MTree.readNodeFromString("""
name: default
root: itest
region: eu-central-1
bucket: kt2l-itest
accessKey: ${#env.AWS_ACCESS_KEY_ID}
secretKey: ${#env.AWS_SECRET_ACCESS_KEY}
"""), "test","");

        var storage = driver.createStorage(bucket, "test");
        return storage;
    }

    private AwsS3Driver createAwsS3Driver() throws IllegalAccessException {
        var tmp = new File("target/tmp");
        if (!tmp.exists()) tmp.mkdirs();

        var driver = new AwsS3Driver();
        var config = new Configuration();
        {
            Field field = ReflectionUtils.findFields(Configuration.class, f -> f.getName().equals("tmpDirectoryFile"), ReflectionUtils.HierarchyTraversalMode.TOP_DOWN).get(0);
            field.setAccessible(true);
            field.set(config, tmp);
        }

        {
            Field field = ReflectionUtils.findFields(AwsS3Driver.class, f -> f.getName().equals("config"), ReflectionUtils.HierarchyTraversalMode.TOP_DOWN).get(0);
            field.setAccessible(true);
            field.set(driver, new Configuration());
        }
        return driver;
    }

}
