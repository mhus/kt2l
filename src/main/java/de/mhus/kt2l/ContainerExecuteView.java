package de.mhus.kt2l;

import com.flowingcode.vaadin.addons.xterm.ITerminalClipboard;
import com.flowingcode.vaadin.addons.xterm.ITerminalOptions;
import com.flowingcode.vaadin.addons.xterm.XTerm;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ScrollOptions;
import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import de.mhus.commons.tools.MThread;
import io.kubernetes.client.Exec;
import io.kubernetes.client.PodLogs;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class ContainerExecuteView extends VerticalLayout implements XTabListener {


    private static final int MAX = 300000;
    private final ClusterConfiguration.Cluster clusterConfig;
    private final CoreV1Api api;
    private final MainView mainView;
    private final PodGrid.Pod pod;
    private final UI ui;
    private XTab tab;
    private XTerm xterm;
    private Thread threadInput;
    private Process proc;
    private Thread threadError;


    public ContainerExecuteView(ClusterConfiguration.Cluster clusterConfig, CoreV1Api api, MainView mainView, PodGrid.Pod pod) {
        this.clusterConfig = clusterConfig;
        this.api = api;
        this.mainView = mainView;
        this.pod = pod;
        this.ui = UI.getCurrent();
    }

    @Override
    public void tabInit(XTab xTab) {
        this.tab = xTab;

        MenuBar menuBar = new MenuBar();
        menuBar.addItem("Autoscroll", e -> {
            System.out.println("Autoscroll");
        });
        menuBar.addItem("Wrap lines", e -> {
            System.out.println(".");
        });
        menuBar.addItem("Json", e -> {
            System.out.println("..");
        });
        add(menuBar);

        xterm = new XTerm();
        xterm.writeln("Start console\n\n");
        xterm.setCursorBlink(true);
        xterm.setCursorStyle(ITerminalOptions.CursorStyle.UNDERLINE);
//        xterm.addCustomKeyListener(e -> {
//            xterm.write(" ");
//        }, Key.SPACE);

        xterm.setSizeFull();
        xterm.setCopySelection(true);
        xterm.setUseSystemClipboard(ITerminalClipboard.UseSystemClipboard.READWRITE);
        xterm.setPasteWithRightClick(true);
        xterm.addLineListener(e -> {
            System.out.println("Line: " + e.getLine());
            try {
//                proc.outputWriter().write(e.getLine());
//                proc.outputWriter().newLine();
                proc.getOutputStream().write(e.getLine().getBytes());
                proc.getOutputStream().write('\n');
                LOGGER.info("Alive: {}", proc.isAlive());
            } catch (IOException ex) {
                LOGGER.error("Write error", ex);
            }
        });

        add(xterm);
        setSizeFull();

        try {
            Exec exec = new Exec(api.getApiClient());
            proc =
                    exec.exec(pod.getPod(), new String[]{"/bin/bash"}, true, true);

            threadInput = Thread.startVirtualThread(this::loopInput);
            threadError = Thread.startVirtualThread(this::loopError);
        } catch (Exception e) {
            LOGGER.error("Execute", e);
        }


    }

    private void loopError() {
        try {
            byte[] buffer = new byte[1024];
            InputStream is = proc.getErrorStream();
            while (true) {
                int len = is.read(buffer);
                if (len <= 0) {
                    MThread.sleep(100);
                    continue;
                }
                System.out.println("ERead: " + len);
                String line = new String(buffer, 0, len);
                ui.access(() -> xterm.write(line));
            }
        } catch (Exception e) {
            LOGGER.error("Loop", e);
        }
    }

    private void loopInput() {
        try {
            byte[] buffer = new byte[1024];
            InputStream is = proc.getInputStream();
            while (true) {
                int len = is.read(buffer);
                if (len <= 0) {
                    MThread.sleep(100);
                    continue;
                }
                System.out.println("IRead: " + len);
                String line = new String(buffer, 0, len);
                ui.access(() -> xterm.write(line));
            }
        } catch (Exception e) {
            LOGGER.error("Loop", e);
        }
    }


    @Override
    public void tabSelected() {

    }

    @Override
    public void tabDeselected() {
    }

    @Override
    public void tabDestroyed() {
        LOGGER.debug("Destroy xterm");
        if (proc != null) {
            proc.destroy();
        }
        if (threadInput != null) {
            threadInput.interrupt();
        }
        if (threadError != null) {
            threadError.interrupt();
        }
        threadInput = null;
        threadError = null;
        proc = null;
    }

    @Override
    public void tabRefresh() {
    }

    @Override
    public void tabShortcut(ShortcutEvent event) {

    }

}
