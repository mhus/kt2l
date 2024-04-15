package de.mhus.kt2l.core;

import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;
import de.mhus.kt2l.cluster.ClusterConfiguration;
import de.mhus.kt2l.config.Configuration;
import de.mhus.kt2l.k8s.K8sService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
public class ClusterOverviewPanel extends VerticalLayout implements XTabListener {

    @Autowired
    private K8sService k8s;

    @Autowired
    private Configuration configuration;

    @Autowired
    private PanelService panelService;
    private XTab tab;

    @Getter
    private MainView mainView;
    private ComboBox<Cluster> clusterBox;
    private List<Cluster> clusterList;

    public ClusterOverviewPanel(MainView mainView) {
        this.mainView = mainView;
    }

    public void createUi() {
        final var clustersConfig = configuration.getClusterConfiguration();

        add(new Text(" "));
        clusterBox = new ComboBox<>("Select a cluster");
        clusterList = k8s.getAvailableContexts().stream()
                .map(name -> {
                    final var clusterConfig = clustersConfig.getClusterOrDefault(name);
                    return new Cluster(name, clusterConfig.title(), clusterConfig);
                })
                .filter(cluster -> cluster.config().enabled())
                .toList();

        clusterBox.setItems(clusterList);
        clusterBox.setItemLabelGenerator(Cluster::title);
        clusterBox.setWidthFull();
        if (clustersConfig.defaultClusterName() != null) {
            clusterList.stream().filter(c -> c.name().equals(clustersConfig.defaultClusterName())).findFirst().ifPresent(clusterBox::setValue);
        }
        add(clusterBox);

        Button resourcesButton = new Button("Resources");
        resourcesButton.addClickListener(click -> {
            if (clusterBox.getValue() != null) {
                panelService.addResourcesGrid(mainView, clusterBox.getValue()).select();
            }
        });
        add(resourcesButton);

        StreamResource imageResource = new StreamResource("kt2l-logo.svg",
                () -> getClass().getResourceAsStream("/images/kt2l-logo.svg"));

        Image image = new Image(imageResource, "Logo");
        image.setWidthFull();
        image.setMaxWidth("800px");
        add(image);

        setWidthFull();
    }

    @Override
    public void tabInit(XTab xTab) {
        LOGGER.debug("Main Init");
        this.tab = xTab;
        createUi();
    }

    @Override
    public void tabSelected() {
        LOGGER.debug("Main Selected");
        if (clusterBox != null)
            clusterBox.focus();
    }

    @Override
    public void tabUnselected() {
        LOGGER.debug("Main DeSelected");
    }

    @Override
    public void tabDestroyed() {
        LOGGER.debug("Main Destroyed");
    }

    @Override
    public void tabRefresh(long counter) {
        LOGGER.trace("Main Refreshed");
    }

    @Override
    public void tabShortcut(ShortcutEvent event) {

    }

    public record Cluster(String name, String title, ClusterConfiguration.Cluster config) {
    }

}
