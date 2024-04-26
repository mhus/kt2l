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

package de.mhus.kt2l.resources.pods;

import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import de.mhus.commons.tools.MJson;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tools.MThread;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.config.ViewsConfiguration;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.XTab;
import de.mhus.kt2l.core.XTabListener;
import io.kubernetes.client.PodLogs;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public class PodLogsPanel extends VerticalLayout implements XTabListener {

    private static final String CONFIG_VIEW_LOG = "log";
    @Autowired
    private ViewsConfiguration viewsConfiguration;

    private static int maxCachedCharacters = 300000;
    private final Cluster clusterConfig;
    private final CoreV1Api api;
    private final Core core;
    private final UI ui;
    private final List<ContainerResource> containers;
    private XTab tab;
    private TextArea logs;
    private LinkedList<LogEntry> logsBuffer = new LinkedList<>();
    // private boolean showAllMode;
    // private MenuItem menuItemAll;
    private volatile MenuItem menuItemJson;
    private MenuItem menuItemWrapLines;
    private volatile MenuItem menuItemAutoScroll;
    private volatile MenuItem menuItemWatch;
    private int maxCachedEntries = 1000;
    private long logsCount = 0;
    private ArrayList<Thread> streamLoopThreads = new ArrayList<>();
    private Thread showResultsThread;
    private volatile MenuItem menuItemShowSource;
    private volatile MenuItem menuItemShowTime;
    private TextField filterText;
    private String filter = null;

    public PodLogsPanel(Cluster clusterConfig, CoreV1Api api, Core core, List<ContainerResource> containers) {
        this.clusterConfig = clusterConfig;
        this.api = api;
        this.core = core;
        this.containers = containers;
        this.ui = UI.getCurrent();
    }

    @Override
    public void tabInit(XTab xTab) {
        this.tab = xTab;

        maxCachedEntries = viewsConfiguration.getConfig(CONFIG_VIEW_LOG).getInt("maxCachedEntries", 1000);
        maxCachedCharacters = viewsConfiguration.getConfig(CONFIG_VIEW_LOG).getInt("maxCachedCharacters", 300000);

        var menuBar = new MenuBar();
        var viewMenuItem = menuBar.addItem("View");
        var viewMenu = viewMenuItem.getSubMenu();

        menuItemWatch = viewMenu.addItem("Watch");
        menuItemWatch.setCheckable(true);
        menuItemWatch.setChecked(true);

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
        menuItemJson = viewMenu.addItem("Json", e -> {
            synchronized (logsBuffer) {
                if (menuItemJson.isChecked())
                    logsBuffer.forEach(ee -> ee.text = processJson(ee.raw));
                else
                    logsBuffer.forEach(ee -> ee.text = ee.raw);
                logsCount = 0;
            }
        });
        menuItemJson.setCheckable(true);
        menuItemJson.setChecked(true);

        menuItemShowSource = viewMenu.addItem("Show Source", e -> {
            logsCount = 0;
        });
        menuItemShowSource.setCheckable(true);
        menuItemShowSource.setChecked(containers.size() > 1);

        menuItemShowTime = viewMenu.addItem("Show Time", e -> {
            logsCount = 0;
        });
        menuItemShowTime.setCheckable(true);
        menuItemShowTime.setChecked(false);


//        menuItemAll = viewMenu.addItem("All", e -> {
//            if (showAllMode)
//                tailMode();
//            else
//                showAllMode();
//            menuItemAll.setChecked(showAllMode);
//        });
//        menuItemAll.setCheckable(true);
//        menuBar.addItem("Download", e -> {
//            System.out.println("..");
//        });


        filterText = new TextField();
        filterText.setPlaceholder("Filter ...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> {
            filter = MString.isEmpty(e.getValue()) ? null : e.getValue();
            synchronized (logsBuffer) {
                logsBuffer.forEach(ee -> ee.checkFilter());
                logsCount = 0;
            }
        });


        final var menuBarHorizontal = new HorizontalLayout();
        menuBarHorizontal.setWidthFull();
        menuBarHorizontal.add(menuBar);
        menuBarHorizontal.add(filterText);
        add(menuBarHorizontal);

        logs = new TextArea();
        logs.setReadOnly(true);
        logs.setMaxLength(maxCachedCharacters);
        logs.addClassName("log-view");

        logs.setWidth("100%");
        logs.setHeight("90%");
        logs.addClassNames("no-word-wrap");

        add(logs);
        setSizeFull();

        containers.forEach(c -> {
            streamLoopThreads.add(Thread.startVirtualThread(() -> streamLoop(c)));
        });
        showResultsThread = Thread.startVirtualThread(this::showResults);
    }

    private void showResults() {
        try {
            long lastCount = 0;
            while(true) {
                if (menuItemWatch.isChecked() && lastCount != logsCount) {
                    lastCount = logsCount;
                    StringBuilder out = new StringBuilder();
                    synchronized (logsBuffer) {
                        logsBuffer.forEach(e -> {
                            if (!e.visible) return;
                            if (menuItemShowSource.isChecked())
                                out.append(e.source).append(": ");
                            if (menuItemShowTime.isChecked())
                                out.append(e.time).append(" ");
                            out.append(e.text).append("\n");
                        });
                    }
                    ui.access(() -> {
                        logs.setValue(out.toString());
                        if (menuItemAutoScroll.isChecked()) {
                            ui.getPage().executeJs(
                                    "document.querySelector('.log-view').shadowRoot.querySelector(\"vaadin-input-container\").scrollTop = Number.MAX_SAFE_INTEGER");
                        }
                        ui.push();
                    });
                }
                MThread.sleep(100);
            }
        } catch (Exception e) {
            LOGGER.error("Interrupted", e);
        }
    }

//    private void tailMode() {
//        showAllMode = false;
//        registerLog();
//        if (streamLoopThread != null) streamLoopThread.interrupt();
//        streamLoopThread = Thread.startVirtualThread(() -> streamLoop());
//    }
//
//    private void showAllMode() {
//        showAllMode = true;
//        // stop log stream
//        streamLoopThread.interrupt();
//        try {
//            logStream.close();
//            logStream = null;
//        } catch (IOException e) {
//        }
//        logsBuffer.setLength(0);
//
//        logs.setValue("Loading ...");
//        ui.push();
//        try {
//            InputStream is = podLogs.streamNamespacedPodLog(pod.getPod());
//            ByteArrayOutputStream os = new ByteArrayOutputStream();
//            copy(is, os);
//            String content = new String(os.toByteArray());
//            LOGGER.debug("Content length: {}", content.length());
//            if (menuItemJson.isChecked()) {
//                content = processJson(content);
//            }
//            logs.setMaxLength(content.length() + 1000);
//            logs.setValue(content);
//        } catch (ApiException | IOException e) {
//            LOGGER.error("Error reading log stream", e);
//            logs.setValue("Error: " + e.getMessage());
//        }
//
//    }

//    public static void copy(InputStream in, OutputStream out) throws IOException {
//        byte[] buffer = new byte[1024 * 10];
//        int bytesRead;
//        long total = 0;
//        long lastTime = System.currentTimeMillis();
//        while ((bytesRead = in.read(buffer)) != -1) {
//            out.write(buffer, 0, bytesRead);
//            long thisTime = System.currentTimeMillis();
//            if (thisTime - lastTime > 2000) {
//                LOGGER.debug("Read timeout {}", (thisTime - lastTime));
//                break;
//            }
//            System.out.println("Read: " + bytesRead);
//            total += bytesRead;
//            if (total % 1000000 == 0)
//                LOGGER.debug("Read: {}", total);
//            if (total > 1024 * 1024 * 20) {
//                LOGGER.debug("Too much - Break");
//                break;
//            }
//        }
//        out.flush();
//    }

    private void streamLoop(ContainerResource container) {
        InputStream logStream = null;
        try {
            V1Pod pod = container.getPod();
            String source = pod.getMetadata().getName();

            PodLogs podLogs = new PodLogs(api.getApiClient());
            logStream = podLogs.streamNamespacedPodLog(
                    pod.getMetadata().getNamespace(),
                    pod.getMetadata().getName(),
                    null,
                    null,
                    10,
                    true
            );

            BufferedReader reader = new BufferedReader(new InputStreamReader(logStream));

            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                addLogEntry(new LogEntry(source, line));
            }

        } catch (Exception e) {
            LOGGER.debug("Interrupted", e);
        }

        LOGGER.info("Close log stream");
        if (logStream != null) {
            try {
                logStream.close();
            } catch (Exception e) {
                LOGGER.error("Error closing log stream", e);
            }
        }

    }

    private void addLogEntry(LogEntry logEntry) {
        synchronized (logsBuffer) {
            logsBuffer.add(logEntry);
            while (logsBuffer.size() > maxCachedEntries) {
                logsBuffer.removeFirst();
            }
            logsCount++;
        }

    }

    private String processJson(String text) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder();
        BufferedReader bf = new BufferedReader(new java.io.StringReader(text));
        String line = "";
        try {
            while ((line = bf.readLine()) != null) {
                if (line.startsWith("{") && line.endsWith("}")) {
                    var json = MJson.load(line);
                    var messageJson = json.get("message");
                    var message = messageJson == null ? "" :messageJson.asText();
                    var severity = json.get("severity");
                    var timestamp = json.get("@timestamp");
                    out.append(timestamp).append(" ").append(severity).append(" ").append(message);
                } else
                    out.append(line);
            }
        } catch (IOException e) {
            LOGGER.error("Error processing json", e);
        }
        return out.toString();
    }

    @Override
    public void tabSelected() {

    }

    @Override
    public void tabUnselected() {
    }

    @Override
    public void tabDestroyed() {
        LOGGER.debug("Destroy logger tab");

        if (streamLoopThreads != null)
            streamLoopThreads.forEach(Thread::interrupt);
        streamLoopThreads = null;

        if (showResultsThread != null)
            showResultsThread.interrupt();
        streamLoopThreads = null;
    }

    @Override
    public void tabRefresh(long counter) {
    }

    @Override
    public void tabShortcut(ShortcutEvent event) {

    }

    @Getter
    private class LogEntry {
        String time;
        String source;
        String raw;
        @Setter
        String text;
        boolean visible = true;

        public LogEntry(String source, String raw) {
            if (raw == null) raw = "";
            int pos = raw.indexOf(' ');
            if (pos >= 0) {
                this.time = raw.substring(0, pos);
                this.raw = raw.substring(pos + 1);
            } else {
                this.time = "xxxx-xx-xxTxx:xx:xx.xxxxxxxxxZ";
            }
            this.source = source;
            this.text = menuItemJson.isChecked() ? processJson(this.raw) : this.raw;
            checkFilter();
        }

        public void checkFilter() {
            if (filter == null) {
                visible = true;
            } else {
                if (filter.startsWith("/"))
                    visible = source.matches(filter.substring(1));
                else
                    visible = text.contains(filter);
            }
        }
    }
}
