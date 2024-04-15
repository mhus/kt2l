package de.mhus.kt2l.core;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ResourcesClusterAction implements ClusterAction {

    @Autowired
    private PanelService panelService;

    @Override
    public String getTitle() {
        return "Resources";
    }

    @Override
    public void execute(MainView mainView, ClusterOverviewPanel.Cluster cluster) {
        panelService.addResourcesGrid(mainView, cluster).select();
    }

    @Override
    public Icon getIcon() {
        return VaadinIcon.OPEN_BOOK.create();
    }

    @Override
    public int getPriority() {
        return 1000;
    }
}
