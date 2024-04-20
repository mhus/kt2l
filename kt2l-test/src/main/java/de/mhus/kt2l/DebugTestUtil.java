package de.mhus.kt2l;

import de.mhus.commons.tools.MCast;

import javax.swing.*;

public class DebugTestUtil {

    public static final boolean TEST_DEBUG = MCast.toboolean(System.getenv("TEST_DEBUG"), true);
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
    }

    public static void debugBreakpoint(String msg) {
        if (TEST_DEBUG) {
            JOptionPane.showConfirmDialog(null,
                    msg, "Breakpoint", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE);
        }
    }

}
