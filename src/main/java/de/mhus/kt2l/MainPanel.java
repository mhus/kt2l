package de.mhus.kt2l;

import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
public class MainPanel extends VerticalLayout implements XTabListener {

    @Autowired
    private K8sService k8s;

    @Autowired
    Configuration configuration;

    private XTab tab;

    @Getter
    private MainView mainView;
    private ComboBox<Cluster> clusterBox;
    private List<Cluster> clusterList;

    public MainPanel(MainView mainView) {
        this.mainView = mainView;
    }

    public void createUi() {
        final var clustersConfig = configuration.getClusterConfiguration();

        add(new Text(" "));
        clusterBox = new ComboBox<>("Select a cluster");
        clusterList = k8s.availableContexts().stream()
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
                tab.getViewer().addTab(
                        "test/" + clusterBox.getValue().name(),
                        clusterBox.getValue().title(),
                        true,
                        false,
                        VaadinIcon.OPEN_BOOK.create(),
                        () -> new ResourcesGridPanel(clusterBox.getValue().name(), mainView))
                        .setColor(clusterBox.getValue().config().color()).select();
            }
        });
        add(resourcesButton);

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

    private record Cluster(String name, String title, ClusterConfiguration.Cluster config) {
    }

}
