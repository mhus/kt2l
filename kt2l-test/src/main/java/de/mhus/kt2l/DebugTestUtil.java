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

import de.mhus.commons.tools.MCast;
import de.mhus.commons.tools.MSystem;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.HasFullPageScreenshot;

import javax.swing.*;
import java.io.File;

@Slf4j
public class DebugTestUtil {

    public static final boolean TEST_DEBUG = MCast.toboolean(System.getenv("TEST_DEBUG"), MSystem.isVmDebug());
    public static final boolean TEST_HEADLESS = MCast.toboolean(System.getenv("TEST_HEADLESS"), TEST_DEBUG);
    public static final boolean TEST_SCREENSHOTS = MCast.toboolean(System.getenv("TEST_SCREENSHOTS"), false);
    private static JFrame frame;
    private static JLabel frameLabel;

    public static void debugPrepare() {
        if (TEST_DEBUG) {
            if (frame != null) return;
            System.setProperty("java.awt.headless", String.valueOf(!TEST_DEBUG));
            frame = new JFrame();
            frame.setSize(200,50);
            frameLabel = new JLabel("Running in debug mode");
            frame.getContentPane().add(frameLabel);
            frame.setVisible(true);
            frame.setAlwaysOnTop(true);
        }
        if (TEST_SCREENSHOTS) {
            File dir = new File("target/screenshots");
            if (!dir.exists()) dir.mkdirs();
        }
    }

    public static void debugBreakpoint(String msg) {
        if (!TEST_DEBUG) return;

        frame.toFront();
        frameLabel.setText("Waiting for user input");

        JOptionPane.showConfirmDialog(null,
                msg, "Breakpoint", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE);

        frameLabel.setText("Running in debug mode");

    }

    public static void doScreenshot(WebDriver driver, String name) {
        if (!TEST_SCREENSHOTS) return;

        if (driver instanceof TakesScreenshot screenshotDriver) {
            var screenshot = screenshotDriver.getScreenshotAs(OutputType.FILE);
            screenshot.renameTo(new File("target/screenshots/" + name + ".png"));
            LOGGER.info("Screenshot {} at {}", name, screenshot.getAbsolutePath());
        } else
            LOGGER.error("Screenshot not supported by driver");
    }
}
