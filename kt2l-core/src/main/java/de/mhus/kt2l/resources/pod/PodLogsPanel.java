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

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import de.mhus.commons.tools.MJson;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tools.MThread;
import de.mhus.kt2l.aaa.SecurityContext;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.config.ViewsConfiguration;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.storage.OutputFile;
import de.mhus.kt2l.storage.StorageFile;
import de.mhus.kt2l.storage.StorageService;
import de.mhus.kt2l.ui.ProgressDialog;
import de.mhus.kt2l.ui.Tail;
import de.mhus.kt2l.ui.TailRow;
import de.mhus.kt2l.ui.UiUtil;
import de.mhus.kt2l.ui.VaadinThread;
import io.kubernetes.client.PodLogs;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Configurable
@Slf4j
public class PodLogsPanel extends VerticalLayout implements DeskTabListener {

    private static final String CONFIG_VIEW_LOG = "log";
    @Autowired
    private ViewsConfiguration viewsConfiguration;
    @Autowired
    private StorageService storageService;

    private final Cluster cluster;
    private final ApiProvider apiProvider;
    private final Core core;
    private final List<ContainerResource> containers;
    private DeskTab tab;
    private Tail logs;
    private LinkedList<LogEntry> logsBuffer = new LinkedList<>();
    // private boolean showAllMode;
    // private MenuItem menuItemAll;
    private volatile MenuItem menuItemJson;
    private volatile MenuItem menuItemAnsiCleanup;
    private MenuItem menuItemWrapLines;
    private volatile MenuItem menuItemAutoScroll;
    private volatile MenuItem menuItemWatch;
    private int maxCachedEntries = 1000;
    private ArrayList<Thread> streamLoopThreads = new ArrayList<>();
//    private Thread showResultsThread;
    private volatile MenuItem menuItemShowSource;
    private volatile MenuItem menuItemShowTime;
    private volatile MenuItem menuItemShowColors;
    private TextField filterText;
    private String filter = null;
    private volatile int index;
    private MenuItem menuItemStore;
    private MenuItem menuItemCapture;
    private Div menuItemStoreIconDiv;
    private volatile StorageFile captureDirectory;
    private Icon menuItemStoreIconDivIcon;
    private String[] jsonFields;

    public PodLogsPanel(Core core, Cluster cluster, List<ContainerResource> containers) {
        this.cluster = cluster;
        this.apiProvider = cluster.getApiProvider();
        this.core = core;
        this.containers = containers;
    }

    @Override
    public void tabInit(DeskTab deskTab) {
        this.tab = deskTab;

        maxCachedEntries = viewsConfiguration.getConfig(CONFIG_VIEW_LOG).getInt("maxCachedEntries", 1000);
        jsonFields = viewsConfiguration.getConfig(CONFIG_VIEW_LOG).getString("jsonFields", "@timestamp,severity,message").split(",");

        logs = new Tail();
        logs.setMaxRows(maxCachedEntries);
        logs.addClassName("log-view");

        var menuBar = new MenuBar();
        var viewMenuItem = menuBar.addItem("View");
        var viewMenu = viewMenuItem.getSubMenu();

        menuItemWatch = viewMenu.addItem("Watch");
        menuItemWatch.setCheckable(true);
        menuItemWatch.setChecked(true);

        menuItemAutoScroll = viewMenu.addItem("Autoscroll", e ->
                logs.setAutoScroll(menuItemAutoScroll.isChecked())
        );
        menuItemAutoScroll.setCheckable(true);
        menuItemAutoScroll.setChecked(true);
        logs.setAutoScroll(true);

        menuItemWrapLines = viewMenu.addItem("Wrap lines", e -> {
            logs.setScrollDirection(menuItemWrapLines.isChecked() ? Tail.ScrollDirection.VERTICAL : Tail.ScrollDirection.HORIZONTAL);
        });
        menuItemWrapLines.setCheckable(true);
        menuItemJson = viewMenu.addItem("Json", e -> {
            synchronized (logsBuffer) {
                if (menuItemJson.isChecked()) {
                    menuItemAnsiCleanup.setChecked(false);
                    logsBuffer.forEach(ee -> ee.text = processJson(ee.raw));
                } else
                    logsBuffer.forEach(ee -> ee.text = ee.raw);
            }
            resetTail();
        });
        menuItemJson.setCheckable(true);
        menuItemJson.setChecked(true);

        menuItemAnsiCleanup = viewMenu.addItem("Ansi Cleanup", e -> {
            synchronized (logsBuffer) {
                if (menuItemAnsiCleanup.isChecked()) {
                    menuItemJson.setChecked(false);
                    logsBuffer.forEach(ee -> ee.text = processAnsiEscCleanup(ee.raw));
                } else
                    logsBuffer.forEach(ee -> ee.text = ee.raw);
            }
            resetTail();
        });
        menuItemAnsiCleanup.setCheckable(true);
        menuItemAnsiCleanup.setChecked(false);

        menuItemShowSource = viewMenu.addItem("Show Source", e -> {
            resetTail();
        });
        menuItemShowSource.setCheckable(true);
        menuItemShowSource.setChecked(containers.size() > 1);

        menuItemShowTime = viewMenu.addItem("Show Time", e -> {
            resetTail();
        });
        menuItemShowTime.setCheckable(true);
        menuItemShowTime.setChecked(false);

        menuItemShowColors = viewMenu.addItem("Show Colors");
        menuItemShowColors.setCheckable(true);
        menuItemShowColors.setChecked(true);

        if (storageService.isEnabled()) {

            menuItemStoreIconDivIcon = VaadinIcon.BULLSEYE.create();
            menuItemStoreIconDivIcon.setVisible(false);
            menuItemStoreIconDivIcon.addClassName("color-red");
            menuItemStoreIconDivIcon.setSize("var(--lumo-icon-size-s)");
            menuItemStoreIconDiv = new Div();
            menuItemStoreIconDiv.add(menuItemStoreIconDivIcon, new Text(" Store"));
            menuItemStore = menuBar.addItem(menuItemStoreIconDiv);
            var storeMenu = menuItemStore.getSubMenu();
            storeMenu.addItem("Download", e -> storeLogs());
            menuItemCapture = storeMenu.addItem("Capture", e -> captureLogs());
            menuItemCapture.setCheckable(true);

        }

        filterText = new TextField();
        filterText.setPlaceholder("Filter ...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> {
            filter = MString.isEmpty(e.getValue()) ? null : e.getValue();
            synchronized (logsBuffer) {
                logsBuffer.forEach(ee -> ee.checkFilter());
            }
            resetTail();
        });

        final var menuBarHorizontal = new HorizontalLayout();
        menuBarHorizontal.setWidthFull();
        menuBarHorizontal.add(menuBar);
        menuBarHorizontal.add(filterText);
        add(menuBarHorizontal);

        logs.setWidth("100%");
        logs.setHeight("90%");
        // logs.addClassNames("no-word-wrap");

        add(logs);
        setMargin(false);
        setPadding(false);
        setSizeFull();

        var sc = SecurityContext.create();
        containers.forEach(c -> {
            streamLoopThreads.add(Thread.startVirtualThread(() -> streamLoop(c, sc)));
        });
//        showResultsThread = Thread.startVirtualThread(this::showResults);
    }

    private void captureLogs() {
        try {
            if (captureDirectory == null) {
                captureDirectory = storageService.getStorage().createDirectory("logs_capture");
                menuItemStoreIconDivIcon.setVisible(true);
                menuItemCapture.setChecked(true);
            } else {
                storageService.showStoragePanel(core, captureDirectory);
                captureDirectory = null;
                menuItemStoreIconDivIcon.setVisible(false);
                menuItemCapture.setChecked(false);
            }
        } catch (Exception e) {
            LOGGER.error("Error capturing logs", e);
            UiUtil.showErrorNotification("Error capturing logs", e);
        }
    }

    @VaadinThread
    private void resetTail() {
        try {
            logs.clear();
            synchronized (logsBuffer) {
                logsBuffer.forEach(e -> {
                    appendToTail(e);
                });
            }
            core.ui().push();
        } catch (Exception e) {
            LOGGER.error("Error in tail", e);
        }
    }

    private void storeLogs() {
        ProgressDialog progress = new ProgressDialog();
        progress.setMax(containers.size());
        progress.open();
        try {
                var directory = storageService.getStorage().createDirectory("logs");
                var sc = SecurityContext.create();
                Thread.startVirtualThread(() -> {
                    try (var sce = sc.enter()){
                        containers.forEach(c -> {
                            core.ui().access(() -> progress.next(c.getPod().getMetadata().getNamespace() + "/" + c.getPod().getMetadata().getName()));
                            LOGGER.info("Store logs for {}/{}/{}", c.getPod().getMetadata().getNamespace(), c.getPod().getMetadata().getName(), c.getContainerName());
                            try {
                                var file = storageService.getStorage().createFileStream(directory, c.getPod().getMetadata().getNamespace() + "-" + c.getPod().getMetadata().getName() + "-" + c.getContainerName() + ".log");
                                try (OutputStream out = file.getStream()) {
                                    PodLogs podLogs = new PodLogs(apiProvider.getClient());
                                    var logStream = podLogs.streamNamespacedPodLog(
                                            c.getPod().getMetadata().getNamespace(),
                                            c.getPod().getMetadata().getName(),
                                            c.getContainerName(),
                                            null,
                                            null,
                                            true
                                    );
                                    copy(logStream, out, progress);
                                }
                            } catch (Exception e) {
                                LOGGER.error("Error storing logs", e);
                                UiUtil.showErrorNotification("Error storing logs", e);
                            }
                        });
                    } catch (Exception e) {
                        LOGGER.error("Error storing logs", e);
                        UiUtil.showErrorNotification("Error storing logs", e);
                    } finally {
                        core.ui().access(() -> {
                                    progress.close();
                                    storageService.showStoragePanel(core, directory);
                                }
                        );
                    }
                });
        } catch (Exception e) {
            LOGGER.error("Error storing logs", e);
            UiUtil.showErrorNotification("Error storing logs", e);
        }
    }

    public void copy(InputStream in, OutputStream out, ProgressDialog progress) throws IOException {

        DateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormater.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

        final var now = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        LOGGER.info("Copy stream until {}", now);

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        PrintStream ps = new PrintStream(out);

        String line = null;
        long cnt = 0;
        long cntLines = 0;
        AtomicLong lastLine = new AtomicLong(System.currentTimeMillis());
        Thread.startVirtualThread(() -> {
            try {
                while (lastLine.get() > 0){
                    if (System.currentTimeMillis() - lastLine.get() > 1000) {
                        LOGGER.debug("Timeout in copy stream");
                        in.close();
                        break;
                    }
                    MThread.sleep(1000);
                }
            } catch (Exception e) {
                LOGGER.error("Error in timeout", e);
            }
        });
        while ((line = reader.readLine()) != null) {
            ps.println(line);
            cnt+=line.length();
            cntLines++;
            lastLine.set(System.currentTimeMillis());
            var lineTimeStr = MString.beforeIndex(line, '.');
            try {
                var lineTime = dateFormater.parse(lineTimeStr);
                if (lineTime.after(now.getTime())) {
                    LOGGER.info("End of stream reached after {} characters", cnt);
                    break;
                }
            } catch (ParseException pe) {
                LOGGER.error("Error parsing line time", pe);
                break;
            }
            if (cntLines % 10000 == 0) {
                var timestamp = MString.beforeIndex(line, ' ');
                final var finalCntLines = cntLines;
                if (progress != null)
                    core.ui().access(() -> progress.setProgressDetails("Lines " + finalCntLines + " " + timestamp) );
                LOGGER.debug("Copy {} lines with {} characters and timestamp {}", cntLines, cnt, timestamp);
            }
        }
        lastLine.set(-1);
        ps.flush();
    }

    private void streamLoop(ContainerResource container, SecurityContext sc) {
        int index = nextIndex();
        var color = UiUtil.LIGHT_COLORS.get(index % UiUtil.LIGHT_COLORS.size());
        OutputFile captureFile = null;

        InputStream logStream = null;
        try (var sce = sc.enter()) {
            V1Pod pod = container.getPod();
            String source = pod.getMetadata().getName();

            PodLogs podLogs = new PodLogs(apiProvider.getClient());
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
                addLogEntry(new LogEntry(source, color, line));
                if (captureDirectory != null) {
                    try {
                        if (captureFile == null) {
                            captureFile = storageService.getStorage().createFileStream(captureDirectory, pod.getMetadata().getNamespace() + "-" + source + ".log");
                        }
                        captureFile.getStream().write(line.getBytes());
                        captureFile.getStream().write('\n');
                        captureFile.getStream().flush();
                    } catch (Exception e) {
                        LOGGER.error("Error capturing logs", e);
                    }
                } else {
                    if (captureFile != null) {
                        try {
                            captureFile.getStream().close();
                        } catch (Exception e) {
                            LOGGER.error("Error capturing logs", e);
                        }
                        captureFile = null;
                    }
                }
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

    private synchronized int nextIndex() {
        return index++;
    }

    private void addLogEntry(LogEntry logEntry) {
        synchronized (logsBuffer) {
            logsBuffer.add(logEntry);
            while (logsBuffer.size() > maxCachedEntries) {
                logsBuffer.removeFirst();
            }
        }
        core.ui().access(() -> {
            appendToTail(logEntry);
        });

    }

    @VaadinThread
    private void appendToTail(LogEntry e) {
        if (!e.visible || !menuItemWatch.isChecked()) return;
        var out = new StringBuilder();
        if (menuItemShowSource.isChecked())
            out.append(e.source).append(": ");
        if (menuItemShowTime.isChecked())
            out.append(e.time).append(" ");
        out.append(e.text);
        var row = TailRow.builder().text(out.toString());
        if (menuItemShowColors.isChecked())
            row.color(e.color);
        logs.addRow(row.build());
    }

    private String processAnsiEscCleanup(String text) {
        return text.replaceAll("\\e\\[[\\d;]*[^\\d;]", "");
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
                    for (var fieldName : jsonFields) {
                        var value = json.get(fieldName);
                        if (value != null) {
                            if (out.length() > 0)
                                out.append(" ");
                            out.append(value.asText());
                            MString.fillUntil(out, out.length() / 10 * 10 + 10, ' ');
                        }
                    }
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

//        if (showResultsThread != null)
//            showResultsThread.interrupt();
        streamLoopThreads = null;
    }

    @Override
    public void tabRefresh(long counter) {
    }

    @Getter
    private class LogEntry {
        String time;
        String source;
        String raw;
        @Setter
        String text;
        boolean visible = true;
        UiUtil.COLOR color;

        public LogEntry(String source, UiUtil.COLOR color, String raw) {
            this.color = color;
            if (raw == null) raw = "";
            int pos = raw.indexOf(' ');
            if (pos >= 0) {
                this.time = raw.substring(0, pos);
                this.raw = raw.substring(pos + 1);
            } else {
                this.time = "xxxx-xx-xxTxx:xx:xx.xxxxxxxxxZ";
            }
            this.source = source;
            if (menuItemJson.isChecked())
                this.text = processJson(this.raw);
            else if (menuItemAnsiCleanup.isChecked())
                this.text = processAnsiEscCleanup(this.raw);
            else
                this.text = this.raw;
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
