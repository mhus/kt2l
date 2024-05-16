package de.mhus.kt2l.events;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.cluster.ClusterAction;
import de.mhus.kt2l.cluster.ClusterOverviewPanel;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.PanelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventClusterAction implements ClusterAction {

    @Autowired
    private PanelService panelService;

    @Override
    public String getTitle() {
        return "Events";
    }

    @Override
    public void execute(Core core, ClusterOverviewPanel.ClusterItem cluster) {
        var name = cluster.name();
        panelService.addPanel(
                core, cluster,
                name + ":events",
                cluster.title(),
                false,
                VaadinIcon.CALENDAR_CLOCK.create(),
                () -> new EventPanel(core, cluster.config())
        ).setHelpContext("events").setWindowTitle(cluster.title() + " Events").select();

    }

    @Override
    public Icon getIcon() {
        return VaadinIcon.CALENDAR_CLOCK.create();
    }

    @Override
    public int getPriority() {
        return 1000;
    }
}
