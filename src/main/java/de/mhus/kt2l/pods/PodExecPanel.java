package de.mhus.kt2l.pods;

import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import de.mhus.commons.tools.MThread;
import de.mhus.kt2l.cluster.ClusterConfiguration;
import de.mhus.kt2l.config.ConfigUtil;
import de.mhus.kt2l.config.Configuration;
import de.mhus.kt2l.ui.MainView;
import de.mhus.kt2l.ui.XTab;
import de.mhus.kt2l.ui.XTabListener;
import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStreamReader;
import java.util.List;

@Slf4j
public class PodExecPanel extends VerticalLayout implements XTabListener  {

    @Autowired
    private Configuration configuration;

    private final ClusterConfiguration.Cluster clusterConfig;
    private final CoreV1Api api;
    private final MainView mainView;
    private final List<ContainerResource> containers;
    private final UI ui;
    private XTab tab;
    private AceEditor editor;
    private VerticalLayout results;
    private MenuItem menuItemRun;

    public PodExecPanel(ClusterConfiguration.Cluster clusterConfig, CoreV1Api api, MainView mainView, List<ContainerResource> containers) {
        this.clusterConfig = clusterConfig;
        this.api = api;
        this.mainView = mainView;
        this.containers = containers;
        this.ui = UI.getCurrent();
    }

    @Override
    public void tabInit(XTab xTab) {
        this.tab = xTab;

        var menuBar = new MenuBar();
        menuItemRun = menuBar.addItem("Run", e -> {
            menuItemRun.setEnabled(false);
            editor.setReadOnly(true);
            runCommand();
        });

        editor = new AceEditor();
        editor.setTheme(AceTheme.terminal);
        editor.setMode(AceMode.sh);
        editor.setValue("/bin/bash,-c,ls -la");
        editor.setWidthFull();
        editor.setHeight("200px"); // TODO setSizeFull()

        var editorLayout = new VerticalLayout();
        editorLayout.add(editor);
        editorLayout.setWidthFull();

        results = new VerticalLayout();
        results.setWidthFull();

        SplitLayout splitLayout = new SplitLayout(editorLayout, results);
        splitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
        splitLayout.setSplitterPosition(50);
        splitLayout.setSizeFull();

        add(menuBar);
        add(splitLayout);

    }

    private void runCommand() {
        String command = editor.getValue();
        for (ContainerResource container : containers) {

            var text = new TextArea();
            text.setLabel(container.getContainerName());
            text.addClassName("bgcolor-yellow");
            text.setWidthFull();
            text.setValue("Running ...");
            results.add(text);

            Thread.startVirtualThread(() -> {
                runCommandInContainer(container, command, text);
            });
        }
        menuItemRun.setEnabled(true);
        editor.setReadOnly(false);
    }

    private void runCommandInContainer(ContainerResource container, String command, TextArea text) {
        try {

            Exec exec = new Exec(api.getApiClient());
            var proc = exec.exec(
                    container.getPod(),
                    command.split(","),
                    container.getContainerName(),
                    true, true);

            StringBuffer sb = new StringBuffer();
            var is = proc.getInputStream();
            var reader = new InputStreamReader(is);
            char[] buffer = new char[1024];
            while (true) {
                var size = reader.read(buffer);
                if (size <= 0) break;
                if (size == 0) {
                    MThread.sleep(100);
                } else {
                    sb.append(new String(buffer, 0, size));
                    ui.access(() -> {
                        text.setValue(sb.toString());
                    });
                }
            }
            ui.access(() -> {
                text.removeClassNames("bgcolor-yellow");
            });
        } catch (Exception e) {
            LOGGER.error("Execute", e);
            ui.access(() -> {
                text.removeClassNames("bgcolor-yellow");
                text.addClassName("bgcolor-red");
                text.setValue(text.getValue() + "\n" + e.getMessage());
            });
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

    }

    @Override
    public void tabRefresh(long counter) {

    }

    @Override
    public void tabShortcut(ShortcutEvent event) {

    }
}
