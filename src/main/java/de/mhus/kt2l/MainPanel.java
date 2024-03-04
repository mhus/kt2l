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
                        .map(id -> {
                            final var clusterConfig = clustersConfig.getClusterOrDefault(id);
                            return new Cluster(id, clusterConfig.name(), clusterConfig);
                        })
                        .filter(cluster -> cluster.config().enabled())
                        .toList()
        );
        clusterBox.setItemLabelGenerator(Cluster::name);
        clusterBox.setWidthFull();
        add(clusterBox);

        Button resourcesButton = new Button("Resources");
        resourcesButton.addClickListener(click -> {
            if (clusterBox.getValue() != null) {
//                MainLayout.addRoute(
//                        clusterBox.getValue().name(),
//                        TestView.class,
//                        new RouteParameters(new RouteParam("clusterId", clusterBox.getValue().id())),
//                        VaadinIcon.FILE.create());
//                getUI().ifPresent(ui -> ui.navigate("/test/" + clusterBox.getValue().id()));
                tab.getViewer().addTab(
                        "test/" + clusterBox.getValue().id(),
                        "Resources clusterBox.getValue().id()",
                        true,
                        true,
                        VaadinIcon.PANEL.create(),
                        () -> new TestView(),
                        "clusterId", clusterBox.getValue().id()).select();
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

    private record Cluster(String id, String name, ClusterConfiguration.Cluster config) {
    }

}
