package de.mhus.kt2l;

import de.mhus.commons.tools.MCast;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.OutputType;

import javax.swing.*;
import java.io.File;

@Slf4j
public class DebugTestUtil {

    public static final boolean TEST_DEBUG = MCast.toboolean(System.getenv("TEST_DEBUG"), true);
    public static final boolean TEST_SCREENSHOTS = MCast.toboolean(System.getenv("TEST_SCREENSHOTS"), false);
    private static JFrame frame;

    public static void debugPrepare() {
        if (TEST_DEBUG) {
            if (frame != null) return;
            System.setProperty("java.awt.headless", String.valueOf(!TEST_DEBUG));
            frame = new JFrame();
            frame.setSize(200,50);
            frame.getContentPane().add(new JLabel("Test in debug mode"));
            frame.setVisible(true);
        }
        if (TEST_SCREENSHOTS) {
            File dir = new File("target/screenshots");
            if (!dir.exists()) dir.mkdirs();
        }
    }

    public static void debugBreakpoint(String msg) {
        if (!TEST_DEBUG) return;

        JOptionPane.showConfirmDialog(null,
                msg, "Breakpoint", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE);

    }

    public static void doScreenshot(org.openqa.selenium.chrome.ChromeDriver driver, String name) {
        if (!TEST_SCREENSHOTS) return;

        var screenshot = driver.getScreenshotAs(OutputType.FILE);
        screenshot.renameTo(new File("target/screenshots/" + name + ".png"));
        LOGGER.info("Screenshot {} at {}", name, screenshot.getAbsolutePath());

    }
}
