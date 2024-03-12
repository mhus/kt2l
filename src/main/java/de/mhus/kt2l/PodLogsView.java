package de.mhus.kt2l;

import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import de.mhus.commons.tools.MJson;
import de.mhus.commons.tools.MThread;
import io.kubernetes.client.PodLogs;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Streams;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Slf4j
public class PodLogsView extends VerticalLayout implements XTabListener {


    private static final int MAX = 300000;
    private final ClusterConfiguration.Cluster clusterConfig;
    private final CoreV1Api api;
    private final MainView mainView;
    private final PodGrid.Pod pod;
    private final UI ui;
    private XTab tab;
    private TextArea logs;
    private InputStream logStream;
    private StringBuilder logsBuffer = new StringBuilder();
    private Thread streamLoopThread;
    private PodLogs podLogs;
    private boolean showAllMode;
    private MenuItem menuItemAll;
    private MenuItem menuItemJson;
    private MenuItem menuItemWrapLines;
    private MenuItem menuItemAutoScroll;


    public PodLogsView(ClusterConfiguration.Cluster clusterConfig, CoreV1Api api, MainView mainView, PodGrid.Pod pod) {
        this.clusterConfig = clusterConfig;
        this.api = api;
        this.mainView = mainView;
        this.pod = pod;
        this.ui = UI.getCurrent();
    }

    @Override
    public void tabInit(XTab xTab) {
        this.tab = xTab;

        var menuBar = new MenuBar();
        var viewMenuItem = menuBar.addItem("View");
        var viewMenu = viewMenuItem.getSubMenu();
        menuItemAutoScroll = viewMenu.addItem("Autoscroll");
        menuItemAutoScroll.setCheckable(true);
        menuItemAutoScroll.setChecked(true);
        menuItemWrapLines = viewMenu.addItem("Wrap lines", e -> {
            if (menuItemWrapLines.isChecked())
                logs.removeClassNames("no-word-wrap");
            else
                logs.addClassNames("no-word-wrap");
        });
        menuItemWrapLines.setCheckable(true);
        menuItemJson = viewMenu.addItem("Json");
        menuItemJson.setCheckable(true);
        menuItemAll = viewMenu.addItem("All", e -> {
            if (showAllMode)
                tailMode();
            else
                showAllMode();
            menuItemAll.setChecked(showAllMode);
        });
        menuItemAll.setCheckable(true);
        menuBar.addItem("Download", e -> {
            System.out.println("..");
        });
        add(menuBar);

        logs = new TextArea();
        logs.setReadOnly(true);
        logs.setMaxLength(MAX);
        logs.addClassName("log-view");

        logs.setWidth("100%");
        logs.setHeight("90%");
        logs.addClassNames("no-word-wrap");

        add(logs);
        setSizeFull();

        tailMode();
    }

    private void tailMode() {
        showAllMode = false;
        registerLog();
        if (streamLoopThread != null) streamLoopThread.interrupt();
        streamLoopThread = Thread.startVirtualThread(() -> streamLoop());
    }

    private void showAllMode() {
        showAllMode = true;
        // stop log stream
        streamLoopThread.interrupt();
        try {
            logStream.close();
            logStream = null;
        } catch (IOException e) {
        }
        logsBuffer.setLength(0);

        logs.setValue("Loading ...");
        ui.push();
        try {
            InputStream is = podLogs.streamNamespacedPodLog(pod.pod());
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            copy(is, os);
            String content = new String(os.toByteArray());
            LOGGER.debug("Content length: {}", content.length());
            logs.setMaxLength(content.length() + 1000);
            logs.setValue(content);
        } catch (ApiException | IOException e) {
            LOGGER.error("Error reading log stream", e);
            logs.setValue("Error: " + e.getMessage());
        }

    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024 * 10];
        int bytesRead;
        long total = 0;
        long lastTime = System.currentTimeMillis();
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            long thisTime = System.currentTimeMillis();
            if (thisTime - lastTime > 2000) {
                LOGGER.debug("Read timeout {}", (thisTime - lastTime));
                break;
            }
            System.out.println("Read: " + bytesRead);
            total += bytesRead;
            if (total % 1000000 == 0)
                LOGGER.debug("Read: {}", total);
            if (total > 1024 * 1024 * 20) {
                LOGGER.debug("Too much - Break");
                break;
            }
        }
        out.flush();
    }

    private void streamLoop() {
        try {
            while (true) {

                if (logStream != null) {
                    try {
                        byte[] buffer = new byte[1024 * 3];
                        int bytesRead;
                        while ((bytesRead = logStream.read(buffer)) != -1) {
                            // LOGGER.debug("Read: {}", bytesRead);
                            if (bytesRead != 0) {
                                logsBuffer.append(new String(buffer, 0, bytesRead));
                                if (logsBuffer.length() > MAX) {
                                    logsBuffer.delete(0, logsBuffer.length() - MAX);
                                }
                                String text = logsBuffer.toString();
                                if (menuItemJson.isChecked()) {
                                    text = processJson(text);
                                }
                                final String finalText = text;
                                ui.access(() -> {
                                    logs.setValue(finalText);
                                    if (menuItemAutoScroll.isChecked()) {
                                            ui.getPage().executeJs(
                                    "document.querySelector('.log-view').shadowRoot.querySelector(\"vaadin-input-container\").scrollTop = Number.MAX_SAFE_INTEGER");
                                    }
                                    ui.push();

                                });
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.error("Error reading log stream", e);
                    }
                }
                MThread.sleep(200);
            }
        } catch (Exception e) {
            LOGGER.debug("Interrupted", e);
        }
    }

    private String processJson(String text) {
        StringBuilder out = new StringBuilder();
        BufferedReader bf = new BufferedReader(new java.io.StringReader(text));
        String line = "";
        try {
            while ((line = bf.readLine()) != null) {
                if (line.startsWith("{") && line.endsWith("}")) {
                    var json = MJson.load(line);
                    var message = json.get("message");
                    var severity = json.get("severity");
                    var timestamp = json.get("@timestamp");
                    out.append(timestamp).append(" ").append(severity).append(" ").append(message).append("\n");
                } else
                    out.append(line).append("\n");
            }
        } catch (IOException e) {
            LOGGER.error("Error processing json", e);
        }
        return out.toString();
    }

    private void registerLog() {
        try {
            podLogs = new PodLogs(api.getApiClient());
            logStream = podLogs.streamNamespacedPodLog(
                    pod.pod().getMetadata().getNamespace(),
                    pod.pod().getMetadata().getName(),
                    null,
                    null,
                    10,
                    false
            );

        } catch (Exception e) {
            LOGGER.error("Error starting log stream", e);
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
        LOGGER.debug("Destroy logger");
        streamLoopThread.interrupt();
        try {
            logStream.close();
        } catch (IOException e) {
            LOGGER.error("Error closing log stream", e);
        }
    }

    @Override
    public void tabRefresh() {
    }

    @Override
    public void tabShortcut(ShortcutEvent event) {

    }
}
