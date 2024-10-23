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
package de.mhus.kt2l;

import de.mhus.commons.crypt.MD5;
import de.mhus.commons.io.Zip;
import de.mhus.commons.tools.MFile;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipOutputStream;

@Disabled
public class ZipTest {


    @Test
    public void testZipUnzip() throws IOException {
        File src = new File("src/main/java");
        File dst = new File("target/ziptestdir");
        File zipFile = new File("target/ziptest.zip");
        if (!src.exists()) throw new RuntimeException("src not found");
        if (dst.exists()) MFile.deleteDir(dst);
        if (dst.exists()) throw new RuntimeException("can't delete dst");
        if (!dst.mkdirs()) throw new RuntimeException("can't create dst");
        if (zipFile.exists())
            zipFile.delete();
        if (zipFile.exists()) throw new RuntimeException("can't delete zip file");

        List<File> list = new LinkedList<>();
        collectFiles(list, src);
        // pack
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
        for (File file : list) {
            System.out.println("Zip " + file);
            try (var stream = new FileInputStream(file)) {
                zos.putNextEntry(new java.util.zip.ZipEntry(file.getPath()));
                MFile.copyFile(stream, zos);
                zos.flush();
                zos.closeEntry();
            }
        }
        zos.finish();
        zos.flush();
        zos.close();

        // unpack
        Zip.builder().src(zipFile).dst(dst).build().unzip();

        // validate
        for (File file : list) {
            System.out.println("Validate " + file);
            var dstFile = new File(dst, file.getPath());
            if (!dstFile.exists())
                throw new RuntimeException("File not found " + dstFile.getAbsolutePath());
            var srcMD5 = MD5.toHexString(MFile.readBinaryFile(file));
            var dstMD5 = MD5.toHexString(MFile.readBinaryFile(dstFile));
            if (!srcMD5.equals(dstMD5)) throw new RuntimeException("MD5 check failed for " + dstFile.getAbsolutePath());
        }

    }

    private void collectFiles(List<File> list, File src) {
        if (src.getName().startsWith(".")) return;
        if (src.isFile()) {
            list.add(src);
            return;
        }
        for (File next : src.listFiles())
            collectFiles(list, next);
    }

}
