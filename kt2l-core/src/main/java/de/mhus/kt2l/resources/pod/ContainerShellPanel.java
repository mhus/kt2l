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

package de.mhus.kt2l.resources.pod;

import com.flowingcode.vaadin.addons.xterm.ITerminalClipboard;
import com.flowingcode.vaadin.addons.xterm.ITerminalOptions;
import com.flowingcode.vaadin.addons.xterm.XTerm;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.mhus.commons.tools.MJson;
import de.mhus.commons.tools.MLang;
import de.mhus.commons.tools.MThread;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.config.ShellConfiguration;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.ui.UiUtil;
import io.kubernetes.client.Attach;
import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.io.IOException;
import java.io.InputStream;

@Configurable
@Slf4j
@Uses(XTerm.class)
public class ContainerShellPanel extends VerticalLayout implements DeskTabListener {


    @Autowired
    private ShellConfiguration shellConfiguration;

    private final Cluster cluster;
    private final ApiProvider apiProvider;
    private final Core core;
    private final V1Pod pod;
    private final String containerName;
    private final boolean attach;
    private DeskTab tab;
    private XTerm xterm;
    private Thread threadInput;
    private Process procExec;
    private Thread threadError;
    private MenuBar xTermMenuBar;
    private Attach.AttachResult procAttach;

    public ContainerShellPanel(Cluster cluster, Core core, V1Pod pod, String containerName, boolean attach) {
        this.cluster = cluster;
        this.apiProvider = cluster.getApiProvider();
        this.core = core;
        this.pod = pod;
        this.containerName = containerName == null ? K8sUtil.getAttachableContainer(pod) : containerName;
        this.attach = attach;
    }

    @Override
    public void tabInit(DeskTab deskTab) {
        this.tab = deskTab;

        xterm = new XTerm();
        xterm.writeln("Start console" + (containerName == null ? "" : " on " + containerName) +"\n\n");
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
                write(keyBytes);
            } catch (IOException ex) {
                LOGGER.error("Write error", ex);
                closeTerminal();
            }
        });
        xterm.addPasteListener(e -> {
            try {
                var json = MJson.load(e.getText());
                var text = json.get("text").asText();
                write(text.getBytes());
            } catch (IOException ex) {
                LOGGER.error("Write error", ex);
                closeTerminal();
            }
        });
        xterm.addActionListener(e -> {
            UiUtil.showSuccessNotification(e.getEvent());
        });

        xTermMenuBar = new MenuBar();
        xTermMenuBar.addItem("ESC", e -> {
            MLang.tryThis(() -> write("\u001b".getBytes()));
            xterm.focus();
        });
        xTermMenuBar.addItem("TAB", e -> {
            MLang.tryThis(() -> write("\t".getBytes()));
            xterm.focus();
        });
        xTermMenuBar.addItem("Ctrl+C", e -> {
            MLang.tryThis(() -> write("\u0003".getBytes()));
            xterm.focus();
        });

        add(xTermMenuBar);
        add(xterm);

        xterm.focus();

        setSizeFull();
        setPadding(false);
        setMargin(false);

        if (attach) {
            try {
                Attach attach = new Attach(apiProvider.getClient());
                boolean tty = containerName == null || K8sUtil.hasTty(pod, containerName);
                procAttach = attach.attach(pod, containerName, tty, tty);

                threadInput = Thread.startVirtualThread(this::loopInput);
                threadError = Thread.startVirtualThread(this::loopError);

            } catch (Exception e) {
                LOGGER.error("Attach", e);
            }
        } else {
            try {
                Exec exec = new Exec(apiProvider.getClient());
                procExec = exec.exec(pod, new String[]{shellConfiguration.getShellFor(cluster, pod)}, containerName, true, true);

                threadInput = Thread.startVirtualThread(this::loopInput);
                threadError = Thread.startVirtualThread(this::loopError);
            } catch (Exception e) {
                LOGGER.error("Execute", e);
            }
        }
    }

    private void write(byte[] bytes) throws IOException {
        if (attach) {
            procAttach.getStandardInputStream().write(bytes);
            procAttach.getStandardInputStream().flush();
        } else {
            procExec.getOutputStream().write(bytes);
            procExec.getOutputStream().flush();
        }
    }

    private void closeTerminal() {
        procExec = null;
        if (procAttach != null)
            procAttach.close();
        procAttach = null;
        if (threadError != null)
            threadError.interrupt();
        if (threadInput != null)
            threadInput.interrupt();
        xterm.write("\n+++ Terminal closed +++\n");
        xterm.setEnabled(false);
        xTermMenuBar.setEnabled(false);
    }

    private void handleKey(Key key) {
        System.out.println("Key: " + key);
    }

    private void loopError() {
        try {
            byte[] buffer = new byte[1024];
            InputStream is = attach ? procAttach.getErrorStream() : procExec.getErrorStream();
            while (true) {
                int len = is.read(buffer);
                if (len <= 0) {
                    MThread.sleep(100);
                    continue;
                }
                String line = new String(buffer, 0, len);
                core.ui().access(() -> xterm.write(UiUtil.xtermPrepareEsc(line)));
            }
        } catch (Exception e) {
            if ( e instanceof InterruptedException)
                LOGGER.debug("loopError Interrupted");
            else
                LOGGER.error("loopError", e);
        }
    }

    private void loopInput() {
        try {
            byte[] buffer = new byte[1024];
            InputStream is = attach ? procAttach.getStandardOutputStream() : procExec.getInputStream();
            while (true) {
                int len = is.read(buffer);
                if (len <= 0) {
                    MThread.sleep(100);
                    continue;
                }
                String line = new String(buffer, 0, len);
                core.ui().access(() -> xterm.write(line));
            }
        } catch (Exception e) {
            if ( e instanceof InterruptedException)
                LOGGER.debug("loopInput Interrupted");
            else
                LOGGER.error("loopInput", e);
        }
    }


    @Override
    public void tabSelected() {

    }

    @Override
    public void tabUnselected() {
    }

    @Override
    public void tabDestroyed() {
        LOGGER.debug("Destroy xterm");
        if (procExec != null) {
            procExec.destroy();
        }
        if (procAttach != null) {
            procAttach.close();
        }
        if (threadInput != null) {
            threadInput.interrupt();
        }
        if (threadError != null) {
            threadError.interrupt();
        }
        threadInput = null;
        threadError = null;
        procExec = null;
        procAttach = null;
    }

    @Override
    public void tabRefresh(long counter) {
    }

}
