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

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import org.vaadin.addons.visjs.network.main.Edge;
import org.vaadin.addons.visjs.network.main.Node;

public interface VisHandler {
    void init(VisPanel visPanel);

    void updateAll();

    void postPrepareNode(Node node);

    void prepareNode(Node node, KubernetesObject res);

    V1APIResource getType();

    void updateEdges(String k1, VisPanel.NodeStore v1);

    void createEdge(Edge edge, VisPanel.NodeStore v1, VisPanel.NodeStore v2);

    void destroy();

    void setAutoUpdate(Boolean value);

    void setEnabled(boolean value);

    void setNamespace(String value);

    boolean isEnabled();
}
