/*
 * kt2l - KT2L (ktool) is a web based tool to manage your kubernetes clusters.
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
import de.mhus.commons.tools.MThread;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import javax.swing.*;
import java.io.File;

import static de.mhus.commons.tools.MString.isEmpty;

@Slf4j
public class DebugTestUtil {

    public static final boolean TEST_DEBUG = MCast.toboolean(System.getenv("TEST_DEBUG"), MSystem.isVmDebug());
    public static final boolean TEST_DEBUG_STEPS = MCast.toboolean(System.getenv("TEST_DEBUG_STEPS"), false);
    public static final boolean TEST_HEADLESS = MCast.toboolean(System.getenv("TEST_HEADLESS"), TEST_DEBUG);
    public static final boolean TEST_SCREENSHOTS = MCast.toboolean(System.getenv("TEST_SCREENSHOTS"), false);
    private static JFrame frame;
    private static JLabel frameLabel;
    private static String currentTestName = "";

    public static void debugPrepare(String testName) {
        if (TEST_DEBUG) {
            if (frame != null) {
                DebugTestUtil.currentTestName = testName;
                updateFrame();
                return;
            }
            System.setProperty("java.awt.headless", String.valueOf(!TEST_DEBUG));
            frame = new JFrame();
            frame.setSize(600,50);
            frame.setUndecorated(false);
            frameLabel = new JLabel("Running in debug mode");
            frame.getContentPane().add(frameLabel);
            frame.setVisible(true);
            frame.setAlwaysOnTop(true);
            updateFrame();
        }
        if (TEST_SCREENSHOTS) {
            File dir = new File("target/screenshots");
            if (!dir.exists()) dir.mkdirs();
        }
    }

    private static void updateFrame() {
        if (frameLabel == null) return;
        if (isEmpty(currentTestName))
            frameLabel.setText("Running in debug mode");
        else
            frameLabel.setText("Running: " + currentTestName);
    }

    public static void debugStep(String msg) {
        debugStep(msg, TEST_DEBUG_STEPS);
    }

    public static void debugStep(String msg, boolean force) {
        if (!TEST_DEBUG) return;

        frame.toFront();
        frameLabel.setText("Waiting: " + currentTestName + " - " + msg);

        if (force) {
            JOptionPane.showConfirmDialog(null,
                    msg, "Breakpoint", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE);
        } else {
            MThread.sleep(1000);
        }
        updateFrame();
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

    public static void debugClose() {
        currentTestName = "closed";
        updateFrame();
    }
}
