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

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.config.ViewsConfiguration;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.common.DescribeAction;
import de.mhus.kt2l.ui.ProgressDialog;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.vaadin.addons.visjs.network.main.Edge;
import org.vaadin.addons.visjs.network.main.NetworkDiagram;
import org.vaadin.addons.visjs.network.main.Node;
import org.vaadin.addons.visjs.network.options.Options;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static de.mhus.commons.tools.MLang.tryThis;
import static de.mhus.commons.tools.MString.isSet;

@Configurable
@Slf4j
public class VisPanel extends SplitLayout implements DeskTabListener {


    @Autowired
    private List<VisHandler> visHandlers;
    @Autowired
    private List<HandlerK8s> k8sHandler;
    private Map<String, HandlerK8s> k8sMap;
    @Autowired
    private K8sService k8sService;
    @Autowired
    private PanelService panelService;
    @Autowired
    private DescribeAction describeAction;
    @Autowired
    private ViewsConfiguration viewsConfig;

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
    private VerticalLayout content;
    private VerticalLayout settings;
    private ComboBox<String> namespaceSelector;
    private String selectedNode;

    private volatile boolean needEdgeUpdate = false;
    private volatile boolean isUpdaing = false;
    private Map<String, Checkbox> useSwitches = new HashMap<>();

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

        k8sService.fillTypes(cluster); // prepare cluster

        content = new VerticalLayout();
        content.setSizeFull();
        addToPrimary(content);

        settings = new VerticalLayout();
        settings.setSizeFull();
        settings.setMargin(false);
        settings.setSpacing(false);
        addToSecondary(settings);

        setOrientation(Orientation.HORIZONTAL);
        setSplitterPosition(90);

        nodeList = new LinkedList<>();
        nodeProvider = new ListDataProvider<>(nodeList);
        nd.setNodesDataProvider(nodeProvider);
        edgeList = new LinkedList<>();
        edgeProvider = new ListDataProvider<>(edgeList);
        nd.setEdgesDataProvider(edgeProvider);

        nd.setSizeFull();

        content.add(nd);
        setSizeFull();
        content.setPadding(false);
        content.setMargin(false);

        k8sMap = Collections.synchronizedMap(new HashMap<>());
        k8sHandler.forEach(handler -> k8sMap.put(handler.getManagedType().getKind(), handler));

        var contextMenu = new ContextMenu(nd);
        contextMenu.addItem("Yaml", e -> {
            if (selectedNode == null) return;
            var nodeStore = nodes.get(selectedNode);
            if (nodeStore == null) return;
            panelService.showYamlPanel(deskTab, cluster, nodeStore.handler.getType(), nodeStore.k8sObject).select();
        });
        contextMenu.addItem("Refresh", e -> updateAll());

        nd.setVisible(false);

        // ---

        viewsConfig.getConfig("vis").getArray("presets").ifPresent(presets -> {
            Button presetBtn = new Button("Presets");
            presetBtn.setWidthFull();
            ContextMenu presetMenu = new ContextMenu(presetBtn);
            presetMenu.setOpenOnClick(true);
            presets.forEach(preset -> {
                presetMenu.addItem(preset.getString("name", "?"), e -> {
                    var ns = preset.getString("namespace");
                    if (ns.isPresent()) {
                        namespaceSelector.setValue(ns.get());
                    }
                    var regex = preset.getString("regex").orElse(null);
                    if (isSet(regex)) {
                        useSwitches.forEach((k,v) -> {
                            v.setValue(k.matches(regex));
                        });
                    } else {
                        Set<String> enabledSwitches = new HashSet(MTree.getArrayValueStringList(preset.getArray("types").orElse(MTree.EMPTY_LIST)));
                        useSwitches.forEach((k, v) -> {
                            v.setValue(enabledSwitches.contains(k));
                        });
                    }
                });
            });

            settings.add(presetBtn);
        });

        var updateSwitch = new Checkbox("Auto Update");
        updateSwitch.addValueChangeListener(e -> {
            visHandlers.forEach(handler -> handler.setAutoUpdate(e.getValue()));
        });
        settings.add(updateSwitch);

        visHandlers.forEach(handler -> {
            handler.setNamespace(K8sUtil.NAMESPACE_DEFAULT);
            handler.setAutoUpdate(false);
            handler.setEnabled(false);
        });
        visHandlers.stream().filter(h -> K8s.NAMESPACE.equals(h.getType())).findFirst().ifPresent(h -> h.setEnabled(true));

        namespaceSelector = new ComboBox<String>();
        namespaceSelector.setItems(k8sService.getNamespaces(true,cluster.getApiProvider()));
        namespaceSelector.setValue(K8sUtil.NAMESPACE_DEFAULT);
        namespaceSelector.setWidthFull();
        namespaceSelector.addValueChangeListener(e -> {
            var ns = e.getValue().equals(K8sUtil.NAMESPACE_ALL_LABEL) ? null : e.getValue();
            visHandlers.forEach(handler -> handler.setNamespace(ns));
            updateAll();
        });
        settings.add(namespaceSelector);


        final AtomicBoolean isNamespacedHandler = new AtomicBoolean(true);
        for (VisHandler handler : visHandlers.stream().sorted((a,b) -> {
            if (a.getType().getNamespaced() != b.getType().getNamespaced())
                return a.getType().getNamespaced() ? -1 : 1;
            return a.getType().getKind().compareTo(b.getType().getKind());
        } ).toList()) {
            if (isNamespacedHandler.get() && !handler.getType().getNamespaced()) {
                isNamespacedHandler.set(false);
                settings.add(new Hr());
            }
            var useSwitch = new Checkbox(handler.getType().getKind());
            useSwitch.setValue(handler.isEnabled());
            useSwitch.setTooltipText(K8s.displayName(handler.getType()));
            useSwitch.addValueChangeListener(e -> {
                if (e.getValue()) {
                    enableHandler(handler);
                } else {
                    disableHandler(handler);
                }
            });
            settings.add(useSwitch);
            useSwitches.put(K8s.displayName(handler.getType()) , useSwitch);
        }

        // ---

        updateAll();

        addAttachListener(event -> {
            setListeners();
        });

    }

    private void disableHandler(VisHandler handler) {
        handler.setEnabled(false);
        new ArrayList<>(nodes.values()).forEach(n -> {
            if (n.handler == handler) {
                deleteNode(handler, n.k8sObject());
            }
        });
    }

    private void enableHandler(VisHandler handler) {
        ProgressDialog progress = new ProgressDialog();
        progress.setHeaderTitle("Update Visualization");
        progress.setIndeterminate(true);
        progress.open();
        nd.setVisible(false);
        Thread.startVirtualThread(() -> {
            handler.setEnabled(true);
            handler.updateAll();
            core.ui().access(() -> {
                updateEdges();
                progress.setProgressDetails("nodes: " + nodes.size() + ", edges: " + deges.size());
            } );
            core.ui().access(() -> {
                nd.setVisible(true);
                progress.close();
            } );
        });
    }

    private void setListeners() {
        nd.addSelectListener(e -> {
            var nodeId = tryThis(() -> e.getParams().getArray("nodes").get(0).asString()).orElse(null);
            if (nodeId == null) {
                selectedNode = null;
            } else {
                selectedNode = nodeId;
            }
            LOGGER.debug("Selected Node: {} ", selectedNode);
        });

        nd.addDoubleClickListener(e -> {
            if (selectedNode == null) return;
            var nodeStore = nodes.get(selectedNode);
            if (nodeStore == null) return;
            describeAction.showPreview(core, cluster, nodeStore.handler.getType(), Set.of(nodeStore.k8sObject));
        });
//        if (nd.isVisible())
//            nd.diagamRedraw();
    }

    private void updateAll() {

        ProgressDialog progress = new ProgressDialog();
        progress.setHeaderTitle("Start Visualization");
        progress.setMax(visHandlers.size());
        progress.open();

        nd.setVisible(false);
        isUpdaing = true;
        clearAll();

        Thread.startVirtualThread(() -> {
            try {
                visHandlers.forEach(handler -> {
                    core.ui().access(() -> progress.next(handler.getType().getName()));
                    handler.init(this);
                });
            } catch (Exception e) {
                LOGGER.warn("Error", e);
            }
            core.ui().access(() -> {
                progress.setIndeterminate(true);
                progress.setProgress(0, "edges ...");
                updateEdges();
                needEdgeUpdate = false;
            });
            core.ui().access(() -> {
                progress.setIndeterminate(true);
                progress.setProgress(0, "visualize ...");
                progress.setProgressDetails("nodes: " + nodes.size() + ", edges: " + deges.size());
            } );
            core.ui().access(() -> {
                nd.setVisible(true);
                progress.close();
                isUpdaing = false;
            } );
        });

    }

    private void clearAll() {
        nodes.clear();
        nodeList.clear();
        deges.clear();
        edgeList.clear();
        edgeProvider.refreshAll();
        nodeProvider.refreshAll();
    }

    public HandlerK8s getK8sHandler(V1APIResource type) {
        return k8sMap.get(type.getKind());
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
        if ( counter % 5 == 0 && !isUpdaing && needEdgeUpdate) {
            needEdgeUpdate = false;
            updateEdges();
        }
    }

    public void processNode(VisHandler handler, KubernetesObject res) {
        var id = handler.getType().getKind() + "-" + res.getMetadata().getUid();
        var no = nodes.computeIfAbsent(id, n -> {
            var node = new Node();
            node.setLabel(handler.getType().getKind() + "\n" + res.getMetadata().getName());
            node.setTitle(handler.getType().getKind());
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
        needEdgeUpdate = true;
    }

    public void deleteNode(VisHandler handler, KubernetesObject object) {
        var uid = object.getMetadata().getUid();
        var nodeStore = nodes.values().stream().filter(n -> n.k8sObject.getMetadata().getUid().equals(uid)).findFirst().orElse(null);
        if (nodeStore == null) return;
        core.ui().access(() -> {
            nodes.remove(nodeStore.node.getId());
            nodeList.remove(nodeStore.node);
            nodeProvider.refreshItem(nodeStore.node);
            edgeList.removeIf(e -> {
                var remove = e.getFrom().equals(nodeStore.node().getId()) || e.getTo().equals(nodeStore.node().getId());
                if (remove) {
                    edgeProvider.getItems().remove(e.getValue());
                }
                return remove;
            });
        });
        needEdgeUpdate = true;
    }

    public Map<String, NodeStore> getNodes() {
        return nodes;
    }

    public void processEdge(NodeStore v1, NodeStore v2) {
        deges.computeIfAbsent(v1.node.getId() + "/" + v2.node.getId(), n -> {
            LOGGER.debug("Add Edge {} -> {}", v1.node().getId(), v2.node().getId());
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
