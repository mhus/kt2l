package de.mhus.kt2l;

import de.mhus.commons.errors.MException;
import de.mhus.commons.tools.MFile;
import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.storage.DirectoryDriver;
import de.mhus.kt2l.storage.Storage;
import de.mhus.kt2l.storage.StorageConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
public class FsDriverTest {

    public static void testScenario(Storage storage) throws IOException {
        var name = UUID.randomUUID().toString();
        var osFile = storage.createFileStream("test", name + ".txt");
        LOGGER.warn("OS File: {} {}", osFile.getPath(), osFile.getName());
        LOGGER.warn("OS File: {}", osFile.getPathAndName());
        var os = osFile.getStream();
        os.write("Hello World".getBytes());
        os.close();

        // list
        {
            var foundFiles = storage.listFiles(osFile.getPath());
            foundFiles.forEach(f -> LOGGER.warn("Found in Context: {} {}", f.getPath(), f.getName()));
            assertThat(foundFiles.stream().map(s -> s.getName()).anyMatch(n -> n.equals(osFile.getName()))).isTrue();
            assertThat(foundFiles.stream().map(s -> s.getPath()).anyMatch(p -> p.equals(osFile.getPath()))).isTrue();
        }

        var isFile = storage.openFile(osFile.getPathAndName());
        var is = isFile.getStream();
        var content = new String(is.readAllBytes());
        is.close();

        assertThat(content).isEqualTo("Hello World");

        // list root
        {
            var foundFiles = storage.listFiles("");
            foundFiles.forEach(f -> LOGGER.warn("Found in Root: {} {}", f.getPath(), f.getName()));
            assertThat(foundFiles.stream().map(s -> s.getName()).anyMatch(n -> n.equals(osFile.getName()))).isFalse();
        }

        storage.delete(isFile);

        // list
        {
            var foundFiles = storage.listFiles(MFile.getParentPath(osFile.getPath()));
            assertThat(foundFiles.stream().map(s -> s.getName()).anyMatch(n -> n.equals(osFile.getName()))).isFalse();
        }

    }

    @Test
    public void testFsDriver() throws MException, IllegalAccessException, IOException {

        var storage = createStorage();

        testScenario(storage);

    }

    private Storage createStorage() throws IllegalAccessException, MException {
        var driver = new DirectoryDriver();
        StorageConfiguration.Bucket bucket = new StorageConfiguration.Bucket(Set.of("aws-s3"), "directory", "itest",
                MTree.readNodeFromString("""
name: default
"""), "test","");

        var storage = driver.createStorage(bucket, "test");
        return storage;
    }

}
