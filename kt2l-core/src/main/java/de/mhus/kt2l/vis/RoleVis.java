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
import de.mhus.kt2l.resources.role.RoleWatch;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1RoleBinding;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.vaadin.addons.visjs.network.main.Edge;
import org.vaadin.addons.visjs.network.main.Node;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RoleVis extends AbstractVisHandler {
    @Override
    public K8s[] getConnectedTypes() {
        return new K8s[] {K8s.ROLE_BINDING};
    }

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return RoleWatch.class;
    }

    @Override
    public void prepareNode(Node node, KubernetesObject res) {
        node.setColor("#aaa");
        node.setMass(1);
    }

    @Override
    public K8s getType() {
        return K8s.ROLE;
    }

    @Override
    public void createEdge(Edge edge, VisPanel.NodeStore v1, VisPanel.NodeStore v2) {

    }

    protected void updateConnectedEdge(String k1, VisPanel.NodeStore v1, String k2, VisPanel.NodeStore v2) {
        if (v1.k8sObject().getMetadata().getName() == null || v2.k8sObject().getMetadata().getName() == null) return;
        if (!v1.k8sObject().getMetadata().getNamespace().equals(v2.k8sObject().getMetadata().getNamespace())) return;

        var roleBinding = (V1RoleBinding)v2.k8sObject();
        var role = v1.k8sObject().getMetadata().getName();

        if (roleBinding.getRoleRef().getName().equals(role)) {
            panel.processEdge(v1, v2);
        }

    }
}
