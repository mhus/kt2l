package de.mhus.kt2l.development;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.commons.tools.MSystem;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.CoreAction;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.core.WithRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@WithRole(UsersConfiguration.ROLE.ADMIN)
public class SystemInfoAction implements CoreAction {

    @Autowired
    private PanelService panelService;

    @Override
    public boolean canHandle(Core core) {
        return MSystem.isVmDebug();
    }

    @Override
    public String getTitle() {
        return "System Info";
    }

    @Override
    public void execute(Core core) {
        panelService.showSystemInfoPanel(core).select();
    }

    @Override
    public Icon getIcon() {
        return VaadinIcon.INFO_CIRCLE_O.create();
    }

    @Override
    public int getPriority() {
        return 10000;
    }
}
