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

import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.secret.SecretWatch;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1Pod;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.vaadin.addons.visjs.network.main.Edge;
import org.vaadin.addons.visjs.network.main.Node;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SecretVis extends  AbstractVisHandler {
    @Override
    public V1APIResource[] getConnectedTypes() {
        return new V1APIResource[] {K8s.POD};
    }

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return SecretWatch.class;
    }

    @Override
    public void prepareNode(Node node, KubernetesObject res) {
        node.setColor("#eefeff");
        node.setMass(2);
    }

    @Override
    public V1APIResource getType() {
        return K8s.SECRET;
    }

    @Override
    public void createEdge(Edge edge, VisPanel.NodeStore v1, VisPanel.NodeStore v2) {

    }

    protected void updateConnectedEdge(String k1, VisPanel.NodeStore v1, String k2, VisPanel.NodeStore v2) {
        if (v1.k8sObject().getMetadata().getName() == null || !(v2.k8sObject() instanceof V1Pod))
            return;
        if (!v1.k8sObject().getMetadata().getNamespace().equals(v2.k8sObject().getMetadata().getNamespace()))
            return;

        var pod = (V1Pod) v2.k8sObject();
        if (pod.getSpec().getContainers() == null) return;
        var resName = v1.k8sObject().getMetadata().getName();

        pod.getSpec().getContainers().forEach(container -> {
            if (container.getEnv() != null) {
                container.getEnv().forEach(env -> {
                    if (env.getValueFrom() != null && env.getValueFrom().getSecretKeyRef() != null) {
                        if (env.getValueFrom().getSecretKeyRef().getName().equals(resName)) {
                            panel.processEdge(v1, v2);
                        }
                    }
                });
            }
            if (container.getEnvFrom() != null) {
                container.getEnvFrom().forEach(envFrom -> {
                    if (envFrom.getSecretRef() != null) {
                        if (envFrom.getSecretRef().getName().equals(resName)) {
                            panel.processEdge(v1, v2);
                        }
                    }
                });
            }
        });
    }
}
