package de.mhus.kt2l;

import de.mhus.commons.tools.MFile;
import de.mhus.commons.tools.MLang;
import de.mhus.commons.tools.MSystem;
import de.mhus.commons.tools.MThread;
import de.mhus.kt2l.util.TestResultDebugWatcher;
import io.kubernetes.client.util.Streams;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@ExtendWith(TestResultDebugWatcher.class)
public class DesktopTest {

    @Test
    public void test() throws IOException, AWTException {

        // setup
        System.setProperty("java.awt.headless", "false");

        // start desktop application
        var opts = "-Dspring.profiles.active=prod";
        var jar = new File("../kt2l-desktop/target/kt2l-desktop-linux-amd64-0.0.1-SNAPSHOT.jar");
        if (MSystem.isMac()) {
            opts = "-XstartOnFirstThread -Dspring.profiles.active=prod";
            jar = new File("../kt2l-desktop/target/kt2l-desktop-macosx-aarch64-0.0.1-SNAPSHOT.jar");
        }
        if (!jar.exists()) {
            System.err.println("File not found: " + jar.getAbsolutePath());
            System.err.println("SKIP TEST");
            return;
        }
        var cmd = "java " + opts + " -jar " + jar.getAbsolutePath();
        System.out.println("Execute: " + cmd);

        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", cmd);
        pb.redirectErrorStream(true);
        pb.directory(new File("target"));
        Process proc = pb.start();
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        AtomicBoolean foundLogin = new AtomicBoolean(false);
        Thread.startVirtualThread(() -> {
            try {
                String line;
                while ((line = stdInput.readLine()) != null) {
                    if (line.contains("Do auto login for autologin")) {
                        LOGGER.info("Found: " + line);
                        foundLogin.set(true);
                    }
                    System.err.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        MThread.sleep(1000); // to be sure the application is logged in

        // take screenshot
        try {
            MLang.awaitTrue(() -> foundLogin.get(), 60000);

            Robot r = new Robot();
            Toolkit t = Toolkit.getDefaultToolkit();
            Dimension d = t.getScreenSize();
            Image i = r.createScreenCapture(new Rectangle(0, 0, d.width, d.height));
            BufferedImage bi = new BufferedImage(d.width, d.height, BufferedImage.TYPE_3BYTE_BGR);
            Graphics g = bi.getGraphics();
            g.drawImage(i, 0, 0, null);
            var screenshotFile = new File("target/screenshots/kt2l-desktop.png");
            screenshotFile.getParentFile().mkdirs();
            ImageIO.write(bi, "png", screenshotFile);

        } finally {
            proc.destroy();
        }

    }
}
