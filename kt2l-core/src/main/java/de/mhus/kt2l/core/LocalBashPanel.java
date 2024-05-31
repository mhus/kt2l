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
package de.mhus.kt2l.core;

import com.flowingcode.vaadin.addons.xterm.ITerminalClipboard;
import com.flowingcode.vaadin.addons.xterm.ITerminalOptions;
import com.flowingcode.vaadin.addons.xterm.XTerm;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.mhus.commons.tools.MCollection;
import de.mhus.commons.tools.MLang;
import de.mhus.commons.tools.MThread;
import de.mhus.kt2l.config.ViewsConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.io.IOException;
import java.io.InputStream;

import static net.logstash.logback.util.StringUtils.isBlank;

@Configurable
@Slf4j
public class LocalBashPanel extends VerticalLayout implements DeskTabListener {

    private final Core core;
    private XTerm xterm;
    private volatile Process proc;

    @Autowired
    private ViewsConfiguration viewsConfiguration;

    public LocalBashPanel(Core core) {
        this.core = core;
    }

    @Override
    public void tabInit(DeskTab deskTab) {

        xterm = new XTerm();
        xterm.writeln("Start console\n\n");
        xterm.setCursorBlink(true);
        xterm.setCursorStyle(ITerminalOptions.CursorStyle.UNDERLINE);

        xterm.setSizeFull();
        xterm.setCopySelection(true);
        xterm.setUseSystemClipboard(ITerminalClipboard.UseSystemClipboard.READWRITE);
        xterm.setPasteWithRightClick(true);
        xterm.addAnyKeyListener(e -> {
            byte[] keyBytes = UiUtil.xtermKeyToBytes(e.getKey());
            if (keyBytes == null) return;
            try {
                proc.getOutputStream().write(keyBytes);
                proc.getOutputStream().flush();
            } catch (IOException ex) {
                LOGGER.error("Write error", ex);
                closeTerminal();
            }
        });

        var xTermMenuBar = new MenuBar();
        xTermMenuBar.addItem("ESC", e -> {
            MLang.tryThis(() -> proc.getOutputStream().write("\u001b".getBytes()));
            xterm.focus();
        });
        xTermMenuBar.addItem("TAB", e -> {
            MLang.tryThis(() -> proc.getOutputStream().write("\t".getBytes()));
            xterm.focus();
        });
        xTermMenuBar.addItem("Ctrl+C", e -> {
            MLang.tryThis(() -> proc.getOutputStream().write("\u0003".getBytes()));
            xterm.focus();
        });

        add(xTermMenuBar);
        add(xterm);

        xterm.focus();

        setSizeFull();
        setPadding(false);
        setMargin(false);

        try {
            var bash = viewsConfiguration.getConfig("localBash").getString("path", "/bin/bash");
            var arg = viewsConfiguration.getConfig("localBash").getString("argument", "-i");
            if (isBlank(arg)) arg = null;
            ProcessBuilder builder = new ProcessBuilder(MCollection.notNull(bash, arg));
            builder.redirectErrorStream(true);
            proc = builder.start();
            Thread.startVirtualThread(this::readFromTerminalLoop);
        } catch (Exception e) {
            LOGGER.error("Error", e);
            UiUtil.showErrorNotification("Error creating bash " + e.getMessage());
        }

    }

    private void readFromTerminalLoop() {
        try {
            byte[] buffer = new byte[1024];
            InputStream is = proc.getInputStream();
            while (true) {
                int len = is.read(buffer);
                if (len <= 0) {
                    MThread.sleep(100);
                    continue;
                }
                // System.out.println("SH Read: " + len);
                String line = new String(buffer, 0, len);
                core.ui().access(() -> xterm.write(line));
            }
        } catch (Exception e) {
            LOGGER.error("Loop", e);
        }
    }

    private void closeTerminal() {
        proc.destroy();
        proc = null;
    }

    @Override
    public void tabSelected() {

    }

    @Override
    public void tabUnselected() {

    }

    @Override
    public void tabDestroyed() {
        closeTerminal();
    }

    @Override
    public void tabRefresh(long counter) {

    }

}
