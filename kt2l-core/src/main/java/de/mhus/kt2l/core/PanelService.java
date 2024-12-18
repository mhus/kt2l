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

package de.mhus.kt2l.core;

import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.commons.lang.Function0;
import de.mhus.kt2l.ai.AiResourcePanel;
import de.mhus.kt2l.cfg.CfgFactory;
import de.mhus.kt2l.cfg.GlobalCfgPanel;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.events.EventPanel;
import de.mhus.kt2l.form.FormPanel;
import de.mhus.kt2l.helm.HelmChartDetailsPanel;
import de.mhus.kt2l.helm.HelmClusterAction;
import de.mhus.kt2l.helm.HelmInstalledChartsPanel;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.portforward.PortForwardingPanel;
import de.mhus.kt2l.resources.ResourcesGridPanel;
import de.mhus.kt2l.resources.common.ResourceCreatePanel;
import de.mhus.kt2l.resources.common.ResourceEditFormPanel;
import de.mhus.kt2l.resources.common.ResourcePatchPanel;
import de.mhus.kt2l.resources.common.ResourceYamlEditorPanel;
import de.mhus.kt2l.resources.configmap.EditConfigMapPanel;
import de.mhus.kt2l.resources.pod.ContainerResource;
import de.mhus.kt2l.resources.pod.ContainerShellPanel;
import de.mhus.kt2l.resources.pod.PodExecPanel;
import de.mhus.kt2l.resources.pod.PodGrid;
import de.mhus.kt2l.resources.pod.PodLogsPanel;
import de.mhus.kt2l.resources.secret.EditSecretPanel;
import de.mhus.kt2l.storage.StorageFile;
import de.mhus.kt2l.storage.StoragePanel;
import de.mhus.kt2l.system.SystemInfoPanel;
import de.mhus.kt2l.system.SystemLogPanel;
import de.mhus.kt2l.ui.UiUtil;
import de.mhus.kt2l.vis.VisPanel;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Secret;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class PanelService {

    private DeskTab addPanel(
            DeskTab parentTab,
            String id, String title, boolean unique, AbstractIcon icon, Function0<com.vaadin.flow.component.Component> panelCreator) {
        return parentTab.getTabBar().addTab(
                id,
                title,
                true,
                unique,
                icon,
                panelCreator)
        .setColor(parentTab.getColor())
        .setParentTab(parentTab);
    }

    public DeskTab addPanel(
            Core core, Cluster cluster,
            String id, String title, boolean unique, AbstractIcon icon, Function0<com.vaadin.flow.component.Component> panelCreator) {
        return core.getTabBar().addTab(
                        id,
                        title,
                        true,
                        unique,
                        icon,
                        panelCreator)
                .setColor(cluster == null ? UiUtil.COLOR.NONE : cluster.getColor());
    }

    public DeskTab showYamlPanel(DeskTab parentTab, Cluster cluster, V1APIResource type, KubernetesObject resource) {
        return parentTab.getTabBar().addTab(
                cluster.getName() + ":" + K8s.displayName(type) + ":" + resource.getMetadata().getName() + ":details",
                resource.getMetadata().getName(),
                true,
                true,
                VaadinIcon.FILE_TEXT_O.create(),
                () ->
                        new ResourceYamlEditorPanel(
                                cluster,
                                parentTab.getTabBar().getCore(),
                                type,
                                resource
                        ))
                .setReproducable(true)
                .setColor(parentTab.getColor())
                .setParentTab(parentTab)
                .setHelpContext("yaml")
                .setWindowTitle(cluster.getTitle() + " - " + K8s.displayName(type) + " - " + resource.getMetadata().getName() + " - Details");
    }

    public DeskTab addResourcesGrid(Core core, Cluster cluster) {
        return core.getTabBar().addTab(
                        cluster.getName() + ":resources",
                        cluster.getTitle(),
                        true,
                        false,
                        VaadinIcon.OPEN_BOOK.create(),
                        () -> new ResourcesGridPanel(cluster.getName(), core))
                .setColor(cluster.getColor())
                .setHelpContext("resources")
                .setReproducable(true)
                .setWindowTitle(cluster.getTitle() + " - Resources");
    }

    public DeskTab addResourcesGrid(DeskTab parentTab, Core core, Cluster cluster) {
        return addPanel(
                        parentTab,
                        cluster.getName() + ":resources",
                        cluster.getTitle(),
                        false,
                        VaadinIcon.OPEN_BOOK.create(),
                        () -> new ResourcesGridPanel(cluster.getName(), core))
                .setHelpContext("resources")
                .setReproducable(true)
                .setWindowTitle(cluster.getTitle() + " - Resources");
    }

    public DeskTab addLocalBashPanel(Core core) {
        return addPanel(
                core,
                null,
                "localbash",
                "Local bash",
                false,
                VaadinIcon.MODAL.create(),
                () -> new LocalBashPanel(core)
        )
                .setReproducable(false)
                .setHelpContext("localbash")
                .setWindowTitle("Local Bash");
    }

    public DeskTab showContainerShellPanel(DeskTab parentTab, Cluster cluster, Core core, V1Pod selected, String containerName, boolean attach) {
        return addPanel(
                parentTab,
                cluster.getName() + ":" + selected.getMetadata().getNamespace() + "." + selected.getMetadata().getName() + (attach ? ":attach" : ":shell"),
                selected.getMetadata().getName() + (containerName == null ? "" : "." + containerName),
                true,
                attach ? VaadinIcon.DESKTOP.create() : VaadinIcon.TERMINAL.create(),
                () -> new ContainerShellPanel(
                        cluster,
                        core,
                        selected,
                        containerName,
                        attach
                ))
                .setReproducable(false)
                .setHelpContext("shell")
                .setWindowTitle(cluster.getTitle() + " - " + selected.getMetadata().getNamespace() + "." + selected.getMetadata().getName() + (containerName == null ? "" : "." + containerName) + (attach ? " - Attach" : " - Shell"));
    }

    public DeskTab showStoragePanel(Core core, StorageFile selected) {
        final var tab = addPanel(
                core,
                null,
                "storage",
                "Storage",
                true,
                VaadinIcon.STORAGE.create(),
                () -> new StoragePanel())
                .setReproducable(true)
                .setHelpContext("storage")
                .setWindowTitle("Storage");
        if (selected != null) {
            ((StoragePanel)tab.getPanel()).showFile(selected);
        }
        return tab;
    }

    public DeskTab addVisPanel(Core core, Cluster cluster) {
        return addPanel(
                core,
                cluster,
                cluster.getName() + ":vis",
                cluster.getTitle(),
                false,
                VaadinIcon.CLUSTER.create(),
                () ->
                        new VisPanel(
                                core,
                                cluster
                        )).setHelpContext("vis")
                .setReproducable(false)
                .setColor(cluster.getColor())
                .setWindowTitle(cluster.getTitle() + " - Visulization");
    }

    public DeskTab addEventPanel(DeskTab parentTab, Core core, Cluster cluster, Set<? extends KubernetesObject> selected) {
        var name = selected.iterator().next().getMetadata().getName();
        return addPanel(
                parentTab,
                cluster.getName() + ":" + name + ":events",
                name,
                false,
                VaadinIcon.CALENDAR_CLOCK.create(),
                () -> new EventPanel(core, cluster, selected)
        )
                .setReproducable(true)
                .setHelpContext("events")
                .setWindowTitle(cluster.getName() + " - " + name + " - Events");
    }

    public DeskTab addEventPanel(Core core, Cluster cluster) {
        return addPanel(
                core,
                cluster,
                cluster.getName() + ":events",
                cluster.getTitle(),
                false,
                VaadinIcon.CALENDAR_CLOCK.create(),
                () -> new EventPanel(core, cluster)
        )
                .setReproducable(true)
                .setColor(cluster.getColor())
                .setHelpContext("events")
                .setWindowTitle(cluster.getTitle() + " Events");
    }

    public DeskTab showEditConfigMapPanel(DeskTab parentTab, Core core, Cluster cluster, V1ConfigMap configMap) {
        return addPanel(
                parentTab,
                "edit-configmap-" + configMap.getMetadata().getNamespace() + "-" + configMap.getMetadata().getName(),
                "Edit " + configMap.getMetadata().getName(),
                true,
                VaadinIcon.INPUT.create(),
                () -> new EditConfigMapPanel(core, cluster, configMap)
        )
                .setReproducable(true)
                .setHelpContext("edit_configmap")
                .setWindowTitle(cluster.getTitle() + " - Edit ConfigMap " + configMap.getMetadata().getName());
    }

    public DeskTab addAiPanel(DeskTab parentTab, Core core, Cluster cluster, List<KubernetesObject> resources) {
        var name = resources.getFirst().getMetadata().getName();
        return addPanel(
                parentTab,
                cluster.getName() + ":" + name + ":ai",
                name,
                false,
                VaadinIcon.ACADEMY_CAP.create(),
                () -> new AiResourcePanel(resources, core)
        )
                .setHelpContext("ai")
                .setReproducable(true)
                .setWindowTitle(cluster.getTitle() + " - " + name + " - AI");
    }

    public DeskTab showEditSecretPanel(DeskTab parentTab, Core core, Cluster cluster, V1Secret secret) {
        return addPanel(
                parentTab,
                "edit-secret-" + secret.getMetadata().getNamespace() + "-" + secret.getMetadata().getName(),
                "Edit " + secret.getMetadata().getName(),
                true,
                VaadinIcon.PASSWORD.create(),
                () -> new EditSecretPanel(core, cluster, secret)
        )
                .setReproducable(true)
                .setHelpContext("edit_secret")
                .setWindowTitle(cluster.getTitle() + " - Edit Secret " + secret.getMetadata().getName());
    }

    public DeskTab showGlobalCfgPanel(Core core, List<CfgFactory> globalFactories, File configDir, File ... fallbackDirs) {
        return addPanel(
                core,
                null,
                "global-cfg",
                "Global Settings",
                true,
                VaadinIcon.COGS.create(),
                () -> new GlobalCfgPanel(
                        core,
                        true,
                        globalFactories,
                        configDir,
                        fallbackDirs))
                .setReproducable(true)
                .setHelpContext("global_cfg")
                .setWindowTitle("Global Settings");
    }

    public DeskTab showUserCfgPanel(Core core, List<CfgFactory> factories, File configDir, File ... fallbackDirs) {
        return addPanel(
                core,
                null,
                "user-cfg",
                "User Settings",
                true,
                VaadinIcon.COG.create(),
                () -> new GlobalCfgPanel(
                        core,
                        false,
                        factories,
                        configDir,
                        fallbackDirs))
                .setReproducable(true)
                .setHelpContext("user_cfg")
                .setWindowTitle("User Settings");
    }

    public DeskTab addPodExecPanel(DeskTab parentTab, Core core, Cluster cluster, Set<? extends KubernetesObject> selected) {

        List<ContainerResource> containers = new ArrayList<>();

        selected.forEach(p -> {
            if (p instanceof V1Pod pod) {
                var cs = K8sUtil.findDefaultContainer(pod);
                if (cs != null) {
                    containers.add(new ContainerResource(new PodGrid.Container(
                            PodGrid.CONTAINER_TYPE.DEFAULT,
                            cs,
                            pod)));
                }
            }
            else if (p instanceof ContainerResource container) {
                containers.add(container);
            }
        });
        if (containers.size() == 0) return null;

        final var firstSelectedPod = containers.get(0).getPod();

        return addPanel(
                parentTab,
                cluster.getName() + ":exec",
                containers.size() == 1 ? firstSelectedPod.getMetadata().getName() : containers.size() + " Items",
                false,
                VaadinIcon.FORWARD.create(),
                () ->
                        new PodExecPanel(
                                cluster,
                                core,
                                containers
                        ))
                .setReproducable(true)
                .setHelpContext("exec")
                .setWindowTitle(cluster.getTitle() + " - " + (containers.size() == 1 ? firstSelectedPod.getMetadata().getName() : containers.size() + " Items") + " - Exec");
    }

    public DeskTab addResourceCreatePanel(DeskTab parentTab, Core core, Cluster cluster, String namespace) {
        return addPanel(
                parentTab == null ? core.getHomeTab() : parentTab,
                cluster.getName() + ":" + namespace + ":create",
                namespace,
                false,
                VaadinIcon.FILE_ADD.create(),
                () ->
                        new ResourceCreatePanel(
                                cluster,
                                core,
                                namespace
                        ))
                .setReproducable(true)
                .setHelpContext("create")
                .setWindowTitle(cluster.getTitle() + " - " + namespace + " - Create");
    }

    public DeskTab addResourcePatchPanel(DeskTab parentTab, Core core, Cluster cluster, Set<? extends KubernetesObject> selected) {
        return addPanel(
                parentTab == null ? core.getHomeTab() : parentTab,
                cluster.getName() + ":" + selected + ":patch",
                "Patch " + (selected.size() == 1 ? selected.iterator().next().getMetadata().getName() : selected.size() + " Items"),
                false,
                VaadinIcon.FILE_REFRESH.create(),
                () ->
                        new ResourcePatchPanel(
                                cluster,
                                core,
                                selected
                        ))
                .setReproducable(true)
                .setHelpContext("patch")
                .setWindowTitle(cluster.getTitle() + " - " + selected.size() + " Items - Patch");
    }

    public DeskTab addPodLogsPanel(DeskTab parentTab, Core core, Cluster cluster, Set<? extends KubernetesObject> selected) {

        List<ContainerResource> containers = new ArrayList<>();

        selected.forEach(p -> {
            if (p instanceof V1Pod pod) {
                pod.getStatus().getContainerStatuses().forEach(cs -> {
                    if (cs.getState().getTerminated() == null)
                        containers.add(new ContainerResource(new PodGrid.Container(
                                PodGrid.CONTAINER_TYPE.DEFAULT,
                                cs,
                                pod)));
                });
                if (pod.getStatus().getEphemeralContainerStatuses() != null)
                    pod.getStatus().getEphemeralContainerStatuses().forEach(cs -> {
                        if (cs.getState().getTerminated() == null)
                            containers.add(new ContainerResource(new PodGrid.Container(
                                    PodGrid.CONTAINER_TYPE.EPHEMERAL,
                                    cs,
                                    pod)));
                    });
                if (pod.getStatus().getInitContainerStatuses() != null)
                    pod.getStatus().getInitContainerStatuses().forEach(cs -> {
                        if (cs.getState().getTerminated() == null)
                            containers.add(new ContainerResource(new PodGrid.Container(
                                    PodGrid.CONTAINER_TYPE.INIT,
                                    cs,
                                    pod)));
                    });
            }
            else if (p instanceof ContainerResource container) {
                containers.add(container);
            }
        });
        if (containers.size() == 0) {
            // try to get also terminated containers
            selected.forEach(p -> {
                if (p instanceof V1Pod pod) {
                    pod.getStatus().getContainerStatuses().forEach(cs -> {
                        containers.add(new ContainerResource(new PodGrid.Container(
                                PodGrid.CONTAINER_TYPE.DEFAULT,
                                cs,
                                pod)));
                    });
                }
            });
        }
        if (containers.size() == 0) {
            LOGGER.debug("No containers found for logs");
            UiUtil.showErrorNotification("No containers found to show logs, select a single container");
            return null;
        }

        final var firstSelectedPod = containers.get(0).getPod();

        return addPanel(
                parentTab,
                cluster.getName() + ":logs",
                containers.size() == 1 ? firstSelectedPod.getMetadata().getName() : containers.size() + " Items",
                false,
                VaadinIcon.MODAL_LIST.create(),
                () ->
                        new PodLogsPanel(
                                core,
                                cluster,
                                containers
                        ))
                .setReproducable(true)
                .setHelpContext("logs")
                .setWindowTitle(cluster.getTitle() + " - " + (containers.size() == 1 ? firstSelectedPod.getMetadata().getName() : containers.size() + " Items" ) + " - Logs");
    }

    public DeskTab showPortForwardingPanel(Core core, Cluster cluster) {
        return addPanel(
                core,
                cluster,
                cluster.getName() + "-portforward",
                "Port Forward",
                true,
                VaadinIcon.CLOUD_UPLOAD_O.create(),
                () -> new PortForwardingPanel(core, cluster)
        )
                .setReproducable(true)
                .setColor(cluster.getColor())
                .setHelpContext("portforward")
                .setWindowTitle(cluster.getTitle() + " - Port Forward");
    }

    public DeskTab showHelmInstalledChartsPanel(Core core, Cluster cluster) {
        return addPanel(
                core,
                cluster,
                cluster.getName() + "-helm-chart",
                "Helm Chart",
                true,
                HelmClusterAction.createIcon(),
                () -> new HelmInstalledChartsPanel(core, cluster)
        )
                .setReproducable(true)
                .setColor(cluster.getColor())
                .setHelpContext("helm_installed_charts")
                .setWindowTitle(cluster.getTitle() + " - Helm Charts");

    }

    public DeskTab showHelmChartDetailsPanel(DeskTab parentTab, Cluster cluster, V1Secret resource) {
        return addPanel(
                parentTab,
                cluster.getName() + ":" + resource.getMetadata().getNamespace() + "." + resource.getMetadata().getName() + ":helm-details",
                resource.getMetadata().getName(),
                true,
                HelmClusterAction.createIcon(),
                () -> new HelmChartDetailsPanel(parentTab.getTabBar().getCore(), cluster, resource)
        )
                .setReproducable(true)
                .setHelpContext("helm_details")
                .setWindowTitle(cluster.getTitle() + " - " + resource.getMetadata().getName() + " - Helm Details");
    }

    public DeskTab showSystemInfoPanel(Core core) {
        return addPanel(
                core,
                null,
                "system-info",
                "System Info",
                true,
                VaadinIcon.INFO_CIRCLE_O.create(),
                () -> new SystemInfoPanel()
        )
                .setReproducable(true)
                .setHelpContext("system_info");
    }

    public DeskTab showSystemLogPanel(Core core) {
        return addPanel(
                core,
                null,
                "system-log",
                "System Logs",
                true,
                VaadinIcon.INFO_CIRCLE_O.create(),
                () -> new SystemLogPanel()
        )
                .setReproducable(true)
                .setHelpContext("system_logs");
    }

    public DeskTab showEditFormPanel(DeskTab parentTab, FormPanel form, Cluster cluster, KubernetesObject resource, V1APIResource type) {
        return addPanel(
                parentTab,
                cluster.getName() + ":" + resource.getMetadata().getNamespace() + "." + resource.getMetadata().getName() + ":edit-resource",
                resource.getMetadata().getName(),
                true,
                VaadinIcon.EDIT.create(),
                () -> new ResourceEditFormPanel(parentTab.getTabBar().getCore(), cluster, resource, form, type)
        )
                .setReproducable(true)
                .setHelpContext("edit_form")
                .setWindowTitle(cluster.getTitle() + " - " + K8s.displayName(type) + " - " + resource.getMetadata().getName() + " - Edit");

    }
}
