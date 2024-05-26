package de.mhus.kt2l.portforward;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.cluster.ClusterAction;
import de.mhus.kt2l.cluster.ClusterOverviewPanel;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.core.WithRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@WithRole(UsersConfiguration.ROLE.WRITE)
public class PortForwardClusterAction implements ClusterAction {

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
        return "Port Forward";
    }

    @Override
    public void execute(Core core, ClusterOverviewPanel.ClusterItem cluster) {
        panelService.addPanel(
                core,
                cluster,
                "portforward",
                 "Port Forward",
                true,
                VaadinIcon.CLOUD_UPLOAD_O.create(),
                () -> new PortForwardingPanel(core, cluster.cluster())
        ).select();
    }

    @Override
    public Icon getIcon() {
        return VaadinIcon.CLOUD_UPLOAD_O.create();
    }

    @Override
    public int getPriority() {
        return 2045;
    }
}
