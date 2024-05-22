package de.mhus.kt2l.vis;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.cluster.ClusterAction;
import de.mhus.kt2l.cluster.ClusterOverviewPanel;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.PanelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VisClusterAction implements ClusterAction {

    @Autowired
    private PanelService panelService;

    @Override
    public boolean canHandle(Core core) {
        return true;
    }

    @Override
    public boolean canHandle(Core core, ClusterOverviewPanel.ClusterItem cluster) {
        return true;
    }

    @Override
    public String getTitle() {
        return "Visualize";
    }

    @Override
    public void execute(Core core, ClusterOverviewPanel.ClusterItem cluster) {
        panelService.addPanel(
                core,
                cluster,
                cluster.name() + ":vis",
                cluster.name(),
                false,
                VaadinIcon.CLUSTER.create(),
                () ->
                        new VisPanel(
                                core,
                                cluster.config()
                        )).setHelpContext("vis").select();

    }

    @Override
    public Icon getIcon() {
        return VaadinIcon.CLUSTER.create();
    }

    @Override
    public int getPriority() {
        return 2000;
    }
}
