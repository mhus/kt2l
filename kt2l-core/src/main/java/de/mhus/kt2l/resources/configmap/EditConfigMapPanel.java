package de.mhus.kt2l.resources.configmap;

import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.mhus.commons.lang.IRegistration;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.util.Watch;

public class EditConfigMapPanel extends VerticalLayout implements DeskTabListener {
    private final Core core;
    private final Cluster cluster;
    private final V1ConfigMap selected;
    private IRegistration registration;

    public EditConfigMapPanel(Core core, Cluster cluster, V1ConfigMap selected) {
        this.core = core;
        this.cluster = cluster;
        this.selected = selected;
    }

    @Override
    public void tabInit(DeskTab deskTab) {
        registration = core.backgroundJobInstance(cluster, ConfigMapWatch.class).getEventHandler().registerWeak(this::changedEvent);
    }

    private void changedEvent(Watch.Response<V1DaemonSet> event) {
    }

    @Override
    public void tabSelected() {

    }

    @Override
    public void tabUnselected() {

    }

    @Override
    public void tabDestroyed() {
        registration.unregister();
    }

    @Override
    public void tabRefresh(long counter) {

    }

    @Override
    public void tabShortcut(ShortcutEvent event) {

    }
}
