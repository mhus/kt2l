package de.mhus.kt2l.resources.all;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.cluster.ClusterAction;
import de.mhus.kt2l.cluster.ClusterOverviewPanel;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.PanelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ClusterCreateAction implements ClusterAction {

    @Autowired
    private PanelService panelService;

    @Override
    public String getTitle() {
        return "Create";
    }

    @Override
    public void execute(Core core, ClusterOverviewPanel.ClusterItem cluster) {
        panelService.addPanel(
                core.getMainTab(),
                cluster.name() + ":"+cluster.config().getDefaultNamespace()+":create",
                cluster.config().getDefaultNamespace(),
                false,
                VaadinIcon.FILE_ADD.create(),
                () ->
                        new ResourceCreatePanel(
                                cluster.config(),
                                core,
                                cluster.config().getDefaultNamespace()
                        )).setHelpContext("create").select();

    }

    @Override
    public Icon getIcon() {
        return VaadinIcon.FILE_ADD.create();
    }

    @Override
    public int getPriority() {
        return 5000;
    }
}
