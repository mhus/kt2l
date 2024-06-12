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
package de.mhus.kt2l.vis;

import de.mhus.commons.lang.IRegistration;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.util.Watch;
import lombok.extern.slf4j.Slf4j;
import org.vaadin.addons.visjs.network.main.Node;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractVisHandler implements VisHandler {

    protected VisPanel panel;
    private HandlerK8s k8sHandler;
    private IRegistration eventRegistration;
    private Boolean autoUpdate = false;
    private boolean enabled = true;
    protected String namespace = null;

    public void init(VisPanel visPanel) {
        this.panel = visPanel;
        k8sHandler = visPanel.getK8sHandler(getManagedResourceType());

        updateAll();
    }

    @Override
    public void updateAll() {
        if (!enabled) return;
        try {
            var allResList = k8sHandler.createResourceListWithoutNamespace(panel.getCluster().getApiProvider());
            allResList.getItems().forEach(res -> {
                if (isInNamespace(res)) return;
                panel.processNode(this, res);
            });
        } catch (Exception e) {
            LOGGER.warn("Error", e);
        }
    }

    protected boolean isInNamespace(KubernetesObject res) {
        return getManagedResourceType().isNamespaced() && namespace != null && !namespace.equals(res.getMetadata().getNamespace());
    }

    public void destroy() {
        if (eventRegistration != null)
            eventRegistration.unregister();
    }

    protected void changeEvent(Watch.Response<KubernetesObject> event) {
        if (!autoUpdate || !enabled) return;
        if (getManagedResourceType().isNamespaced() && namespace != null && !namespace.equals(event.object.getMetadata().getNamespace())) return;

        if (K8sUtil.WATCH_EVENT_DELETED.equals(event.type))
            panel.deleteNode(this, event.object);
        else
            panel.processNode(this, event.object);
    }

    public void updateEdges(String k1, VisPanel.NodeStore v1) {

        var connectedKinds = getConnectedResourceTypes() == null ? null : Arrays.stream(getConnectedResourceTypes()).map(t -> t.kind() ).collect(Collectors.toSet());
        panel.getNodes().forEach((k2, v2) -> {
            var kind = VisPanel.getKindOfNodId(k2);
            if (kind == null) return;
            if (connectedKinds == null || connectedKinds.contains(kind)) {
                try {
                    updateConnectedEdge(k1, v1, k2, v2);
                } catch (Exception e) {
                    LOGGER.debug("Error", e);
                }
            }
        });
    }

    protected void updateConnectedEdge(String k1, VisPanel.NodeStore v1, String k2, VisPanel.NodeStore v2) {
        var uid = v1.k8sObject().getMetadata().getUid();
        var ownerReferences = v2.k8sObject().getMetadata().getOwnerReferences();
        if (ownerReferences != null) {
            ownerReferences.forEach(ref -> {
                if (ref.getUid().equals(uid)) {
                    try {
                        panel.processEdge(v1, v2);
                    } catch (Exception e) {
                        LOGGER.debug("Error", e);
                    }
                }
            });
        }
    }

    public abstract K8s[] getConnectedResourceTypes();

    protected abstract Class<? extends ClusterBackgroundJob> getManagedWatchClass();

    public void postPrepareNode(Node node) {
    }

    public synchronized void setAutoUpdate(Boolean value) {
        this.autoUpdate = value;
        if (autoUpdate) {
            if (getManagedWatchClass() != null)
                eventRegistration = panel.getCore().backgroundJobInstance(panel.getCluster(), getManagedWatchClass()).getEventHandler().registerWeak(this::changeEvent);
        } else {
            if (eventRegistration != null)
                eventRegistration.unregister();
            eventRegistration = null;
        }
    }

    public void setEnabled(boolean value) {
        this.enabled = value;
    }

    public void setNamespace(String value) {
        this.namespace = value;
    }

    public boolean isEnabled() {
        return enabled;
    }


}
