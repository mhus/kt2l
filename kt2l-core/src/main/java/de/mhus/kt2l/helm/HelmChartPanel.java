package de.mhus.kt2l.helm;

import com.marcnuri.helm.Helm;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.k8s.K8sService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class HelmChartPanel extends VerticalLayout implements DeskTabListener {

    @Autowired
    private K8sService k8sService;

    private final Core core;
    private final Cluster cluster;
    private Grid<HelmResource> grid;

    public HelmChartPanel(Core core, Cluster cluster) {
        this.core = core;
        this.cluster = cluster;
    }

    @Override
    public void tabInit(DeskTab deskTab) {

        var menuBar = new MenuBar();
        menuBar.addItem("Refresh", e -> refresh());
        add(menuBar);

        grid = new Grid<HelmResource>();
        grid.addColumn(HelmResource::getName).setHeader("Name");
        grid.setSizeFull();
        add(grid);

        refresh();
        setSizeFull();
    }

    private void refresh() {

        var kubeConfig = k8sService.getKubeConfigPath(cluster);
        if (kubeConfig == null) {
            grid.setItems(new ArrayList<>());
            return;
        }
        final List<HelmResource> list = new ArrayList<>();
        Helm.list()
                .withKubeConfig(kubeConfig)
                .all()
                .call().forEach(r -> list.add(new HelmResource(r.getName())));
        grid.setItems(list);
    }

    @Override
    public void tabSelected() {

    }

    @Override
    public void tabUnselected() {

    }

    @Override
    public void tabDestroyed() {
        
    }

    @Override
    public void tabRefresh(long counter) {

    }

    @Getter
    private class HelmResource {

        private String name;

        public HelmResource(String name) {
            this.name = name;
        }

    }

}
