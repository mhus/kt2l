package de.mhus.kt2l.core;

import com.vaadin.flow.component.icon.Icon;

public interface ClusterAction {

    String getTitle();

    void execute(MainView mainView, ClusterOverviewPanel.Cluster cluster);

    Icon getIcon();

    int getPriority();
}
