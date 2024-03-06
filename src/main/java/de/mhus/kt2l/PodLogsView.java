package de.mhus.kt2l;

import com.vaadin.flow.component.ScrollOptions;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import de.mhus.commons.tools.MThread;
import io.kubernetes.client.PodLogs;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;

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

        logs = new TextArea();
        logs.setReadOnly(true);
        logs.setMaxLength(MAX);
        logs.scrollIntoView(new ScrollOptions(ScrollOptions.Behavior.SMOOTH, ScrollOptions.Alignment.END, ScrollOptions.Alignment.START));

        logs.setSizeFull();
        add(logs);
        setSizeFull();

        registerLog();
        if (streamLoopThread != null) streamLoopThread.interrupt();
        streamLoopThread = Thread.startVirtualThread(() -> streamLoop());
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
                                ui.access(() -> {
                                    logs.setValue(text);
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
}
