/*
 * kt2l-desktop - kt2l desktop app
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
/**
 * This file is part of kt2l-desktop.
 *
 * kt2l-desktop is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * kt2l-desktop is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with kt2l-desktop.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.kt2l;

import de.mhus.commons.tools.MArgs;
import de.mhus.commons.tools.MThread;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class Kt2lDesktopApplication extends Kt2lApplication {

    private static Display display;
    private static Set<BrowserInstance> browserInstances = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        Display.setAppName("KT2L");
        display = new Display();

        MArgs arguments = new MArgs(args,
                MArgs.opt('b', "Browser type: NONE, MOZILLA, WEBKIT")
        );
        BrowserInstance.browserType = arguments.getOption("b");
        new BrowserInstance().setStartupMessage();
        
        Thread.startVirtualThread(() -> {
            SpringApplicationBuilder builder = new SpringApplicationBuilder(Kt2lApplication.class);
            builder.headless(false);
            ConfigurableApplicationContext context = builder.run(args);
        });

        while (!browserInstances.isEmpty()) {
            if (!display.readAndDispatch())
                display.sleep();
        }

        var shell = new Shell(display);
        MessageBox messageBox = new MessageBox(shell, SWT.NONE
                | SWT.ICON_INFORMATION);
        messageBox.setMessage("Shutting down ...");
        Thread.startVirtualThread(() -> {
            MThread.sleep(1000);
            System.exit(0);
        });
        messageBox.open();
        display.dispose();

    }

    public static Display getDisplay() {
        return display;
    }

    public static void register(BrowserInstance instance) {
        browserInstances.add(instance);
    }

    public static void unregister(BrowserInstance instance) {
        browserInstances.remove(instance);
    }

    public static List<BrowserInstance> getBrowserInstances() {
        return browserInstances.stream().toList();
    }
}
