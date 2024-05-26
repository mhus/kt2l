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
import de.mhus.commons.lang.IRegistration;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.resources.deployment.DeploymentWatch;
import de.mhus.kt2l.resources.namespace.NamespaceWatch;
import de.mhus.kt2l.resources.pod.PodWatch;
import de.mhus.kt2l.resources.replicaset.ReplicaSetWatch;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.util.Watch;
import lombok.extern.slf4j.Slf4j;
import org.vaadin.addons.visjs.network.main.Edge;
import org.vaadin.addons.visjs.network.main.NetworkDiagram;
import org.vaadin.addons.visjs.network.main.Node;
import org.vaadin.addons.visjs.network.options.Options;
import org.vaadin.addons.visjs.network.options.physics.Physics;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

@Slf4j
public class VisPanel extends VerticalLayout implements DeskTabListener {
    private final Core core;
    private final Cluster cluster;
    private IRegistration registrationPod;
    private Map<String, NodeStore> nodes = Collections.synchronizedMap(new HashMap<>());
    private Map<String, EdgeStore> deges = Collections.synchronizedMap(new HashMap<>());
    private NetworkDiagram nd;
    private ListDataProvider<Node> nodeProvider;
    private LinkedList<Node> nodeList;
    private IRegistration registrationNs;
    private LinkedList<Edge> edgeList;
    private ListDataProvider<Edge> edgeProvider;
    private IRegistration registrationDeploy;
    private IRegistration registrationReplicaSet;

    public VisPanel(Core core, Cluster cluster) {
        this.core = core;
        this.cluster = cluster;
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

        registrationPod = core.backgroundJobInstance(cluster, PodWatch.class).getEventHandler().registerWeak(this::updatePod);
        registrationNs = core.backgroundJobInstance(cluster, NamespaceWatch.class).getEventHandler().registerWeak(this::updateNamespace);
        registrationReplicaSet = core.backgroundJobInstance(cluster, ReplicaSetWatch.class).getEventHandler().registerWeak(this::updateReplicaSet);
        registrationDeploy = core.backgroundJobInstance(cluster, DeploymentWatch.class).getEventHandler().registerWeak(this::updateDeployment);
    }

    private void updateReplicaSet(Watch.Response<V1ReplicaSet> res) {
        var no = nodes.computeIfAbsent("replicaset-" + res.object.getMetadata().getName(), n -> {
            var node = new Node();
            node.setId("replicaset-" + res.object.getMetadata().getName());
            LOGGER.info("Add Node {}", node.getId());
            node.setLabel(res.object.getMetadata().getName());
            node.setTitle("Replicas");
            node.setColor("#dedeff");
            node.setMass(4);
            node.setGroup(res.object.getMetadata().getNamespace());
            core.ui().access(() -> {
                nodeList.add(node);
                nodeProvider.refreshItem(node);
            });
            return new NodeStore(node, res.object);
        });

        if (res.type == "DELETED") {
            nodes.remove(no.node.getId());
            core.ui().access(() -> {
                nodeList.remove(no.node);
                nodeProvider.refreshItem(no.node);
            });
        }
        updateEdges();
    }


    private void updateDeployment(Watch.Response<V1Deployment> res) {
        var no = nodes.computeIfAbsent("deploy-" + res.object.getMetadata().getName(), n -> {
            var node = new Node();
            node.setId("deploy-" + res.object.getMetadata().getName());
            LOGGER.info("Add Node {}", node.getId());
            node.setLabel(res.object.getMetadata().getName());
            node.setTitle("Deployment");
            node.setColor("#deffff");
            node.setMass(5);
            node.setGroup(res.object.getMetadata().getNamespace());
            core.ui().access(() -> {
                nodeList.add(node);
                nodeProvider.refreshItem(node);
            });
            return new NodeStore(node, res.object);
        });

        if (res.type == "DELETED") {
            nodes.remove(no.node.getId());
            core.ui().access(() -> {
                nodeList.remove(no.node);
                nodeProvider.refreshItem(no.node);
            });
        }
        updateEdges();
    }

    private void updateNamespace(Watch.Response<V1Namespace> res) {
        var no = nodes.computeIfAbsent("ns-" + res.object.getMetadata().getName(), n -> {
            var node = new Node();
            node.setId("ns-" + res.object.getMetadata().getName());
            LOGGER.info("Add Node {}", node.getId());
            node.setLabel(res.object.getMetadata().getName());
            node.setTitle("Namespace");
            node.setColor("#ffdede");
            node.setMass(10);
            node.setGroup(res.object.getMetadata().getName());
            core.ui().access(() -> {
                nodeList.add(node);
                nodeProvider.refreshItem(node);
            });
            return new NodeStore(node, res.object);
        });

        if (res.type == "DELETED") {
            nodes.remove(no.node.getId());
            core.ui().access(() -> {
                nodeList.remove(no.node);
                nodeProvider.refreshItem(no.node);
            });
        }
        updateEdges();
    }

    private synchronized void updatePod(Watch.Response<V1Pod> res) {
        var no = nodes.computeIfAbsent("pod-" + res.object.getMetadata().getNamespace() + "-" + res.object.getMetadata().getName(), n -> {
            var node = new Node();
            node.setId("pod-" + res.object.getMetadata().getNamespace() + "-" + res.object.getMetadata().getName());
            LOGGER.info("Add Node {}", node.getId());
            node.setLabel(res.object.getMetadata().getName());
            node.setTitle("Pod");
            node.setColor("#ffffde");
            node.setMass(1);
            node.setGroup(res.object.getMetadata().getNamespace());
            core.ui().access(() -> {
                nodeList.add(node);
                nodeProvider.refreshItem(node);
            });
            return new NodeStore(node, res.object);
        });

        if (res.type == "DELETED") {
            nodes.remove(no.node.getId());
            core.ui().access(() -> {
                nodeList.remove(no.node);
                nodeProvider.refreshItem(no.node);
            });
        }
        updateEdges();
    }

    private synchronized void updateEdges() {

        // check for new edges from namespace
        nodes.forEach((k1,v1) -> {
            if (k1.startsWith("replicaset-")) {
                var uid = v1.k8sObject().getMetadata().getUid();
                nodes.forEach((k2,v2) -> {
                    if (k2.startsWith("pod-")) {
                        var ownerReferences = v2.k8sObject().getMetadata().getOwnerReferences();
                        if (ownerReferences != null) {
                            ownerReferences.forEach(ref -> {
                                if (ref.getUid().equals(uid)) {
                                    deges.computeIfAbsent(v1.node.getId() + "/" + v2.node.getId(), n -> {
                                        LOGGER.info("Add Edge {} -> {}", v1.node().getId(), v2.node().getId());
                                        var edge = new Edge(v1.node().getId(), v2.node().getId());
                                        core.ui().access(() -> {
                                            edgeList.add(edge);
                                            edgeProvider.refreshItem(edge);
                                        });
                                        return new EdgeStore(edge, v1, v2);
                                    });
                                }
                            });
                        }
                    }
                });
            } else
            if (k1.startsWith("deploy-")) {
                var uid = v1.k8sObject().getMetadata().getUid();
                nodes.forEach((k2,v2) -> {
                    if (k2.startsWith("replicaset-")) {
                        var ownerReferences = v2.k8sObject().getMetadata().getOwnerReferences();
                        if (ownerReferences != null) {
                            ownerReferences.forEach(ref -> {
                                if (ref.getUid().equals(uid)) {
                                    deges.computeIfAbsent(v1.node.getId() + "/" + v2.node.getId(), n -> {
                                        LOGGER.info("Add Edge {} -> {}", v1.node().getId(), v2.node().getId());
                                        var edge = new Edge(v1.node().getId(), v2.node().getId());
                                        core.ui().access(() -> {
                                            edgeList.add(edge);
                                            edgeProvider.refreshItem(edge);
                                        });
                                        return new EdgeStore(edge, v1, v2);
                                    });
                                }
                            });
                        }
                    }
                });
            } else
            if (k1.startsWith("ns-")) {

                nodes.forEach((k2,v2) -> {
                    if (k2.startsWith("deploy-")) {
                        var ns = v2.k8sObject().getMetadata().getNamespace();
                        if (ns != null && ns.equals(v1.k8sObject().getMetadata().getName())) {
                            deges.computeIfAbsent(v1.node.getId() + "/" + v2.node.getId(), n -> {
                                LOGGER.info("Add Edge {} -> {}", v1.node().getId(), v2.node().getId());
                                var edge = new Edge(v1.node().getId(), v2.node().getId());
                                core.ui().access(() -> {
                                    edgeList.add(edge);
                                    edgeProvider.refreshItem(edge);
                                });
                                return new EdgeStore(edge, v1, v2);
                            });
                        }
                    }
                });

            }
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

//    final List<Node> nodes = new LinkedList<>();
//    AtomicInteger idCounter = new AtomicInteger();
//        for (int i=1 ; i <= 5 ; i++)
//    {
//        int nodeId = idCounter.incrementAndGet();
//        var node = new Node(nodeId+"", "Node "+nodeId);
//        node.setColor(i % 2 == 0 ? "red" : "blue");
//        nodes.add(node);
//    }
//    final ListDataProvider<Node> dataProvider = new ListDataProvider<>(nodes);
//        nd.setNodesDataProvider(dataProvider);
//        nd.setEdges(new Edge("1", "3"), new Edge("1", "2"), new Edge("2", "4"), new Edge("2", "5"), new Edge("3", "3"));
//    final Registration registrationSelect = nd.addSelectNodeListener(
//            ls -> Notification.show("NodeId selected " + ls.getParams().getArray("nodes").toJson()));
//    final Registration registrationDeselect =  nd.addDeselectNodeListener(
//            ls -> Notification.show("NodeId deselected " + ls.getParams().getObject("previousSelection").getArray("nodes").toJson()));
//    //
//    add(nd);

    @Override
    public void tabSelected() {

    }

    @Override
    public void tabUnselected() {

    }

    @Override
    public void tabDestroyed() {
        registrationPod.unregister();
        registrationNs.unregister();
        registrationDeploy.unregister();
        registrationReplicaSet.unregister();
    }

    @Override
    public void tabRefresh(long counter) {

    }

    @Override
    public void tabShortcut(ShortcutEvent event) {

    }

    private record NodeStore(Node node, KubernetesObject k8sObject) {
    }

    private record EdgeStore(Edge edge, NodeStore from, NodeStore to) {
    }

}
