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

import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8s;
import io.kubernetes.client.common.KubernetesObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.vaadin.addons.visjs.network.main.Edge;
import org.vaadin.addons.visjs.network.main.NetworkDiagram;
import org.vaadin.addons.visjs.network.main.Node;
import org.vaadin.addons.visjs.network.options.Options;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Configurable
@Slf4j
public class VisPanel extends VerticalLayout implements DeskTabListener {

    @Autowired
    private List<VisHandler> visHandlers;
    @Autowired
    private List<HandlerK8s> k8sHandler;
    private Map<String, HandlerK8s> k8sMap;

    @Getter
    private final Core core;
    @Getter
    private final Cluster cluster;
    private Map<String, NodeStore> nodes = Collections.synchronizedMap(new HashMap<>());
    private Map<String, EdgeStore> deges = Collections.synchronizedMap(new HashMap<>());
    private NetworkDiagram nd;
    private ListDataProvider<Node> nodeProvider;
    private LinkedList<Node> nodeList;
    private LinkedList<Edge> edgeList;
    private ListDataProvider<Edge> edgeProvider;
//    private IRegistration registrationNs;
//    private IRegistration registrationPod;
//    private IRegistration registrationDeploy;
//    private IRegistration registrationReplicaSet;

    public VisPanel(Core core, Cluster cluster) {
        this.core = core;
        this.cluster = cluster;
    }

    public static String getKindOfNodId(String id) {
        var pos = id.indexOf('-');
        if (pos < 0) return null;
        var kind = id.substring(0, pos);
        return kind;
    }

    @Override
    public void tabInit(DeskTab deskTab) {
        nd = new NetworkDiagram(
                Options.builder()
                        .withWidth("100%")
                        .withHeight("100%")
                        .withAutoResize(true)
                        .build()
        );
        nodeList = new LinkedList<>();
        nodeProvider = new ListDataProvider<>(nodeList);
        nd.setNodesDataProvider(nodeProvider);
        edgeList = new LinkedList<>();
        edgeProvider = new ListDataProvider<>(edgeList);
        nd.setEdgesDataProvider(edgeProvider);

        nd.setSizeFull();
        add(nd);
        setSizeFull();
        setPadding(false);
        setMargin(false);

        k8sMap = Collections.synchronizedMap(new HashMap<>());
        k8sHandler.forEach(handler -> k8sMap.put(handler.getManagedResourceType().kind(), handler));

        visHandlers.forEach(handler -> handler.init(this));

    }

    public HandlerK8s getK8sHandler(K8s resourceType) {
        return k8sMap.get(resourceType.kind());
    }

    private synchronized void updateEdges() {

        // check for new edges from namespace
        nodes.forEach((k1,v1) -> {
                    v1.handler.updateEdges(k1, v1);
                });

        // check for non existing nodes
        deges.entrySet().removeIf(e -> {
            var ret = !nodes.containsValue(e.getValue().from()) || !nodes.containsValue(e.getValue().to());
            if (ret) {
                core.ui().access(() -> {
                    edgeList.remove(e.getValue());
                    edgeProvider.refreshItem(e.getValue().edge());
                });
            }
            return ret;
        } );


    }

    @Override
    public void tabSelected() {

    }

    @Override
    public void tabUnselected() {

    }

    @Override
    public void tabDestroyed() {
        visHandlers.forEach(handler -> handler.destroy());
    }

    @Override
    public void tabRefresh(long counter) {

    }

    @Override
    public void tabShortcut(ShortcutEvent event) {

    }

    public void processNode(VisHandler handler, KubernetesObject res) {
        var id = handler.getManagedResourceType().kind() + "-" + res.getMetadata().getUid();
        var no = nodes.computeIfAbsent(id, n -> {
            var node = new Node();
            node.setLabel(handler.getManagedResourceType().kind() + "\n" + res.getMetadata().getName());
            node.setTitle(handler.getManagedResourceType().kind());
            if (res.getMetadata().getNamespace() != null)
                node.setGroup(res.getMetadata().getNamespace());
            handler.prepareNode(node, res);
            node.setId(id);
            handler.postPrepareNode(node);

            core.ui().access(() -> {
                nodeList.add(node);
                nodeProvider.refreshItem(node);
            });
            return new NodeStore(node, handler, res);
        });
        updateEdges();
    }

    public void deleteNode(VisHandler handler, KubernetesObject object) {
        var uid = object.getMetadata().getUid();
        var nodeStore = nodes.values().stream().filter(n -> n.k8sObject.getMetadata().getUid().equals(uid)).findFirst().orElse(null);
        if (nodeStore == null) return;
        core.ui().access(() -> {
            nodeList.remove(nodeStore.node);
            nodeProvider.refreshItem(nodeStore.node);
        });
        updateEdges();
    }

    public Map<String, NodeStore> getNodes() {
        return nodes;
    }

    public void processEdge(NodeStore v1, NodeStore v2) {
        deges.computeIfAbsent(v1.node.getId() + "/" + v2.node.getId(), n -> {
            LOGGER.info("Add Edge {} -> {}", v1.node().getId(), v2.node().getId());
            var edge = new Edge(v1.node().getId(), v2.node().getId());
            v1.handler.createEdge(edge, v1, v2);
            core.ui().access(() -> {
                edgeList.add(edge);
                edgeProvider.refreshItem(edge);
            });
            return new EdgeStore(edge, v1, v2);
        });
    }

    public record NodeStore(Node node, VisHandler handler, KubernetesObject k8sObject) {
    }

    public record EdgeStore(Edge edge, NodeStore from, NodeStore to) {
    }

}
