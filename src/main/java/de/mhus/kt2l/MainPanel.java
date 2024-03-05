package de.mhus.kt2l;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.springframework.beans.factory.annotation.Autowired;

public class MainPanel extends VerticalLayout implements XTabListener {

    @Autowired
    private K8sService k8s;

    @Autowired
    Configuration configuration;

    private XTab tab;

    public MainPanel() {
        add(new Text("Hello World"));
    }

    public void createUi() {
        final var clustersConfig = configuration.getClusterConfiguration();

        add(new Text(" "));
        ComboBox<Cluster> clusterBox = new ComboBox<>("Select a cluster");
        clusterBox.setItems(
                k8s.availableContexts().stream()
                        .map(name -> {
                            final var clusterConfig = clustersConfig.getClusterOrDefault(name);
                            return new Cluster(name, clusterConfig.title(), clusterConfig);
                        })
                        .filter(cluster -> cluster.config().enabled())
                        .toList()
        );
        clusterBox.setItemLabelGenerator(Cluster::title);
        clusterBox.setWidthFull();
        add(clusterBox);

        Button resourcesButton = new Button("Resources");
        resourcesButton.addClickListener(click -> {
            if (clusterBox.getValue() != null) {
                tab.getViewer().addTab(
                        "test/" + clusterBox.getValue().name(),
                        "Resources " + clusterBox.getValue().title(),
                        true,
                        true,
                        VaadinIcon.PANEL.create(),
                        () -> new ResourcesView(clusterBox.getValue().name())).select();
            }
        });
        add(resourcesButton);

        setWidthFull();
    }

    @Override
    public void tabInit(XTab xTab) {
        this.tab = xTab;
        createUi();
    }

    @Override
    public void tabSelected() {

    }

    @Override
    public void tabClosed() {

    }

    @Override
    public void tabDestroyed() {

    }

    private record Cluster(String name, String title, ClusterConfiguration.Cluster config) {
    }

}
