package de.mhus.kt2l.pods;

import com.flowingcode.vaadin.addons.xterm.ITerminalClipboard;
import com.flowingcode.vaadin.addons.xterm.ITerminalOptions;
import com.flowingcode.vaadin.addons.xterm.XTerm;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.mhus.commons.tools.MJson;
import de.mhus.commons.tools.MThread;
import de.mhus.kt2l.config.ConfigUtil;
import de.mhus.kt2l.config.Configuration;
import de.mhus.kt2l.cluster.ClusterConfiguration;
import de.mhus.kt2l.ui.XTab;
import de.mhus.kt2l.ui.XTabListener;
import de.mhus.kt2l.ui.MainView;
import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Uses(XTerm.class)
public class ContainerShellPanel extends VerticalLayout implements XTabListener {


    @Autowired
    private Configuration configuration;

    private static final int MAX = 300000;
    private final ClusterConfiguration.Cluster clusterConfig;
    private final CoreV1Api api;
    private final MainView mainView;
    private final V1Pod pod;
    private final UI ui;
    private XTab tab;
    private XTerm xterm;
    private Thread threadInput;
    private Process proc;
    private Thread threadError;
    private MenuItem menuItemEsc;


    public ContainerShellPanel(ClusterConfiguration.Cluster clusterConfig, CoreV1Api api, MainView mainView, V1Pod pod) {
        this.clusterConfig = clusterConfig;
        this.api = api;
        this.mainView = mainView;
        this.pod = pod;
        this.ui = UI.getCurrent();
    }

    @Override
    public void tabInit(XTab xTab) {
        this.tab = xTab;

        xterm = new XTerm();
        xterm.writeln("Start console\n\n");
        xterm.setCursorBlink(true);
        xterm.setCursorStyle(ITerminalOptions.CursorStyle.UNDERLINE);

        xterm.setSizeFull();
        xterm.setCopySelection(true);
        xterm.setUseSystemClipboard(ITerminalClipboard.UseSystemClipboard.READWRITE);
        xterm.setPasteWithRightClick(true);
        xterm.addAnyKeyListener(e -> {
            System.out.println("Key: " + e.getKey());
            try {
                /*
Key: {"key":"A","code":"KeyA","ctrlKey":false,"altKey":false,"metaKey":false,"shiftKey":true}
Key: {"key":"Meta","code":"MetaLeft","ctrlKey":false,"altKey":false,"metaKey":true,"shiftKey":false}
Key: {"key":"ArrowUp","code":"ArrowUp","ctrlKey":false,"altKey":false,"metaKey":false,"shiftKey":false}
Key: {"key":"Meta","code":"MetaLeft","ctrlKey":false,"altKey":false,"metaKey":true,"shiftKey":false}
Key: {"key":"Meta","code":"MetaLeft","ctrlKey":false,"altKey":false,"metaKey":true,"shiftKey":false}
Key: {"key":"Escape","code":"Escape","ctrlKey":false,"altKey":false,"metaKey":false,"shiftKey":false}
Key: {"key":"Meta","code":"MetaLeft","ctrlKey":false,"altKey":false,"metaKey":true,"shiftKey":false}
                 */
                var json = MJson.load(e.getKey());
                var key = json.get("key").asText();
                if (key.length() == 1) {
                    try {
                        proc.getOutputStream().write(key.getBytes());
                        LOGGER.info("Alive: {}", proc.isAlive());
                    } catch (IOException ex) {
                        LOGGER.error("Write error", ex);
                        closeTerminal();
                    }
                } else
                    if (key.equals("Escape")) {
                        proc.getOutputStream().write("\u001b".getBytes());
                } else
                    if (key.equals("ArrowUp")) {
                        proc.getOutputStream().write("\u001b[A".getBytes());
                    }
                    else if (key.equals("ArrowDown")) {
                        proc.getOutputStream().write("\u001b[B".getBytes()
                        );
                    }
                    else if (key.equals("ArrowRight")) {
                        proc.getOutputStream().write("\u001b[C".getBytes());
                    }
                    else if (key.equals("ArrowLeft")) {
                        proc.getOutputStream().write("\u001b[D".getBytes());
                    }
                    else if (key.equals("Backspace")) {
                        proc.getOutputStream().write("\u007f".getBytes());
                    }
                    else if (key.equals("Delete")) {
                        proc.getOutputStream().write("\u001b[3~".getBytes());
                    }
                    else if (key.equals("Enter")) {
                        proc.getOutputStream().write("\n".getBytes());
                    } else if (key.equals("Tab")) {
                        proc.getOutputStream().write("\t".getBytes());
                    }
            } catch (Exception ex) {
                LOGGER.error("Key", ex);
            }
        });
//        xterm.addLineListener(e -> {
//            if (proc == null) return;
//            var line = e.getLine();
//            System.out.println("Line: " + line);
//            var pos = line.indexOf('#'); // TODO this is a hack
//            if (pos >= 0) {
//                line = line.substring(pos + 1);
//            }
//
//            try {
//                proc.getOutputStream().write(line.getBytes());
//                proc.getOutputStream().write('\n');
//                LOGGER.info("Alive: {}", proc.isAlive());
//            } catch (IOException ex) {
//                LOGGER.error("Write error", ex);
//                closeTerminal();
//            }
//        });

//        for (Key key : new Key[]{Key.ENTER, Key.BACKSPACE, Key.DELETE, Key.ESCAPE, Key.TAB, Key.SPACE, Key.ARROW_DOWN, Key.ARROW_LEFT, Key.ARROW_RIGHT, Key.ARROW_UP, Key.PAGE_DOWN, Key.PAGE_UP, Key.END, Key.HOME, Key.INSERT, Key.F1, Key.F2, Key.F3, Key.F4, Key.F5, Key.F6, Key.F7, Key.F8, Key.F9, Key.F10, Key.F11, Key.F12}) {
//            UI.getCurrent().addShortcutListener(
//                    () -> handleKey(key),
//                    key).listenOn(xterm);
//        }

        var xTermMenuBar = new MenuBar();
        menuItemEsc = xTermMenuBar.addItem("ESC", e -> {
            xterm.write("\u001b");
        });

        add(xTermMenuBar);
        add(xterm);

        xterm.focus();

        setSizeFull();

        try {
            Exec exec = new Exec(api.getApiClient());
            proc = exec.exec(pod, new String[]{ConfigUtil.getShellFor(configuration, clusterConfig, pod )}, true, true);

            threadInput = Thread.startVirtualThread(this::loopInput);
            threadError = Thread.startVirtualThread(this::loopError);
        } catch (Exception e) {
            LOGGER.error("Execute", e);
        }


    }

    private void closeTerminal() {
        proc = null;
        if (threadError != null)
            threadError.interrupt();
        if (threadInput != null)
            threadInput.interrupt();
        menuItemEsc.setEnabled(false);
        xterm.setEnabled(false);
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
