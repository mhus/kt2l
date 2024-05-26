package de.mhus.kt2l.core;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.config.UsersConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@WithRole(UsersConfiguration.ROLE.ADMIN)
public class LocalBashCoreAction implements CoreAction {

    @Autowired
    private PanelService panelService;

    @Override
    public boolean canHandle(Core core) {
        return true;
    }

    @Override
    public String getTitle() {
        return "Local Bash";
    }

    @Override
    public void execute(Core core) {
        panelService.addPanel(
                core,
                null,
                "localbash",
                "Local bash",
                false,
                VaadinIcon.MODAL.create(),
                () -> new LocalBashPanel(core)
                ).select();
    }

    @Override
    public Icon getIcon() {
        return VaadinIcon.MODAL.create();
    }

    @Override
    public int getPriority() {
        return 2050;
    }
}
