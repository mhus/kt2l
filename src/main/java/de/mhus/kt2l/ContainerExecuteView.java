package de.mhus.kt2l;

import com.flowingcode.vaadin.addons.xterm.ITerminalClipboard;
import com.flowingcode.vaadin.addons.xterm.ITerminalOptions;
import com.flowingcode.vaadin.addons.xterm.XTerm;
import com.vaadin.flow.component.ScrollOptions;
import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import de.mhus.commons.tools.MThread;
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
        xterm.writeln("Hello world.\n\n");
        xterm.setCursorBlink(true);
        xterm.setCursorStyle(ITerminalOptions.CursorStyle.UNDERLINE);

        xterm.setSizeFull();
        xterm.setCopySelection(true);
        xterm.setUseSystemClipboard(ITerminalClipboard.UseSystemClipboard.READWRITE);
        xterm.setPasteWithRightClick(true);

        add(xterm);
        setSizeFull();

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
    }

    @Override
    public void tabRefresh() {
    }

    @Override
    public void tabShortcut(ShortcutEvent event) {

    }

}
