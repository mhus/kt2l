package de.mhus.kt2l;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

@PermitAll
@Route(value = "/", layout = MainLayout.class)
public class MainView extends VerticalLayout {

    @Autowired
    private K8sService k8s;

    @PostConstruct
    public void createUi() {

        add(new Text(" "));
        ComboBox<Cluster> clusterBox = new ComboBox<>("Select a cluster");
        clusterBox.setItems(
                k8s.availableContexts().stream()
                        .map(id -> new Cluster(id, id))
                        .toList()
        );
        clusterBox.setItemLabelGenerator(Cluster::name);
        clusterBox.setWidthFull();
        add(clusterBox);

        Button resourcesButton = new Button("Resources");
        resourcesButton.addClickListener(click -> {
            if (clusterBox.getValue() != null) {
                MainLayout.addRoute(
                        clusterBox.getValue().name(),
                        TestView.class,
                        new RouteParameters(new RouteParam("clusterId", clusterBox.getValue().id())),
                        VaadinIcon.FILE.create());
                getUI().ifPresent(ui -> ui.navigate("/test/" + clusterBox.getValue().id()));
            }
        });
        add(resourcesButton);

        setWidthFull();
    }

    private record Cluster(String id, String name) {
    }
}