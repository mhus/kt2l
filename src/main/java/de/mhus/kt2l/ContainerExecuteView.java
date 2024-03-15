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
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class ContainerExecuteView extends VerticalLayout implements XTabListener {


    @Autowired
    private Configuration configuration;

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

        xterm.setSizeFull();
        xterm.setCopySelection(true);
        xterm.setUseSystemClipboard(ITerminalClipboard.UseSystemClipboard.READWRITE);
        xterm.setPasteWithRightClick(true);
        xterm.addLineListener(e -> {
            var line = e.getLine();
            System.out.println("Line: " + line);
            var pos = line.indexOf('#'); // TODO this is a hack
            if (pos >= 0) {
                line = line.substring(pos + 1);
            }

            try {
                proc.getOutputStream().write(line.getBytes());
                proc.getOutputStream().write('\n');
                LOGGER.info("Alive: {}", proc.isAlive());
            } catch (IOException ex) {
                LOGGER.error("Write error", ex);
            }
        });

//        for (Key key : new Key[]{Key.ENTER, Key.BACKSPACE, Key.DELETE, Key.ESCAPE, Key.TAB, Key.SPACE, Key.ARROW_DOWN, Key.ARROW_LEFT, Key.ARROW_RIGHT, Key.ARROW_UP, Key.PAGE_DOWN, Key.PAGE_UP, Key.END, Key.HOME, Key.INSERT, Key.F1, Key.F2, Key.F3, Key.F4, Key.F5, Key.F6, Key.F7, Key.F8, Key.F9, Key.F10, Key.F11, Key.F12}) {
//            UI.getCurrent().addShortcutListener(
//                    () -> handleKey(key),
//                    key).listenOn(xterm);
//        }

        var xTermMenuBar = new MenuBar();
        xTermMenuBar.addItem("ESC", e -> {
            xterm.write("\u001b");
        });

        var xTermLayout = new VerticalLayout(xTermMenuBar, xterm);
        xTermLayout.setSizeFull();

        add(xTermLayout);
        setSizeFull();

        try {
            Exec exec = new Exec(api.getApiClient());
            proc = exec.exec(pod.getPod(), new String[]{ConfigUtil.getShellFor(configuration, clusterConfig, pod.getPod() )}, true, true);

            threadInput = Thread.startVirtualThread(this::loopInput);
            threadError = Thread.startVirtualThread(this::loopError);
        } catch (Exception e) {
            LOGGER.error("Execute", e);
        }


    }

    private void handleKey(Key key) {
        System.out.println("Key: " + key);
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
    public void tabUnselected() {
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
    public void tabRefresh(long counter) {
    }

    @Override
    public void tabShortcut(ShortcutEvent event) {

    }

}
