package de.mhus.kt2l.core;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.cluster.ClusterConfiguration;
import de.mhus.kt2l.resources.ResourceDetailsPanel;
import de.mhus.kt2l.resources.ResourcesGridPanel;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class PanelService {

    public XTab addPanel(
            XTab parentTab,
            String id, String title, boolean unique, Icon icon, Supplier<com.vaadin.flow.component.Component> panelCreator) {
        return parentTab.getViewer().addTab(
                id,
                title,
                true,
                unique,
                icon,
                panelCreator)
        .setColor(parentTab.getColor()).setParentTab(parentTab);
    }

    public XTab addDetailsPanel(XTab parentTab, ClusterConfiguration.Cluster cluster, CoreV1Api api, String resourceType, KubernetesObject resource) {
        return parentTab.getViewer().addTab(
                cluster.name() + ":" + resourceType + ":" + resource.getMetadata().getName() + ":details",
                resource.getMetadata().getName(),
                true,
                true,
                VaadinIcon.FILE_TEXT_O.create(),
                () ->
                        new ResourceDetailsPanel(
                                cluster,
                                api,
                                parentTab.getViewer().getMainView(),
                                resourceType,
                                resource
                        )).setColor(parentTab.getColor()).setParentTab(parentTab).setHelpContext("details");

    }

    public XTab addResourcesGrid(MainView mainView, ClusterOverviewPanel.Cluster cluster) {
        return mainView.getTabBar().addTab(
                        "test/" + cluster.name(),
                        cluster.title(),
                        true,
                        false,
                        VaadinIcon.OPEN_BOOK.create(),
                        () -> new ResourcesGridPanel(cluster.name(), mainView))
                .setColor(cluster.config().color()).setHelpContext("resources");

    }
}
