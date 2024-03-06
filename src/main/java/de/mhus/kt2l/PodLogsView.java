package de.mhus.kt2l;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import io.kubernetes.client.openapi.apis.CoreV1Api;

public class PodLogsView extends VerticalLayout implements XTabListener {


    private final ClusterConfiguration.Cluster clusterConfig;
    private final CoreV1Api api;
    private final MainView mainView;
    private XTab tab;
    private TextArea logs;

    public PodLogsView(ClusterConfiguration.Cluster clusterConfig, CoreV1Api api, MainView mainView) {
        this.clusterConfig = clusterConfig;
        this.api = api;
        this.mainView = mainView;
    }

    @Override
    public void tabInit(XTab xTab) {
        this.tab = xTab;

        MenuBar menuBar = new MenuBar();
        menuBar.addItem("Item 1", e -> {
            System.out.println("Item 1");
        });
        add(menuBar);

        logs = new TextArea();
        logs.setReadOnly(true);
        logs.setSizeFull();
        add(logs);



    }

    @Override
    public void tabSelected() {

    }

    @Override
    public void tabClosed() {

    }

    @Override
    public void tabDestroyed() {

    }

    @Override
    public void tabRefresh() {

    }
}
