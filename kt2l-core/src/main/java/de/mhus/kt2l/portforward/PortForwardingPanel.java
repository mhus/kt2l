package de.mhus.kt2l.portforward;

import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;

public class PortForwardingPanel extends VerticalLayout implements DeskTabListener {
    private final Core core;
    private final Cluster cluster;
    private PortForwardBackgroundJob portForwarder;

    public PortForwardingPanel(Core core, Cluster cluster) {
        this.core = core;
        this.cluster = cluster;
    }

    @Override
    public void tabInit(DeskTab deskTab) {
        portForwarder = core.backgroundJobInstance(cluster, PortForwardBackgroundJob.class);
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
