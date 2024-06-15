package de.mhus.kt2l.development;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.commons.tools.MSystem;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.CoreAction;
import de.mhus.kt2l.core.PanelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class DevelopmentAction implements CoreAction {

    @Autowired
    private PanelService panelService;

    @Override
    public boolean canHandle(Core core) {
        return MSystem.isVmDebug();
    }

    @Override
    public String getTitle() {
        return "Development";
    }

    @Override
    public void execute(Core core) {
        execute(panelService, core);
    }

    public void execute(PanelService panelService, Core core) {
        panelService.addPanel(core, null, "development", "Development", true, VaadinIcon.HAMMER.create(), () ->
                new DevelopmentPanel()
        ).setReproducable(true).select();
    }

    @Override
    public Icon getIcon() {
        return VaadinIcon.HAMMER.create();
    }

    @Override
    public int getPriority() {
        return 10000;
    }
}
