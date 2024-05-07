package de.mhus.kt2l.resources;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.help.HelpAction;
import de.mhus.kt2l.help.HelpConfiguration;
import org.springframework.stereotype.Component;

@Component
public class ResourceCreateHelperAction implements HelpAction {

    @Override
    public boolean canHandle(HelpConfiguration.HelpLink link) {
        return "create".equals(link.getAction());
    }

    @Override
    public void execute(Core core, HelpConfiguration.HelpLink link) {
        ResourceCreateHelpPanel panel = new ResourceCreateHelpPanel(core, link);
        panel.setSizeFull();
        core.setHelpPanel(panel);
        panel.init();
    }

    @Override
    public Icon getIcon(HelpConfiguration.HelpLink link) {
        return VaadinIcon.FILE_ADD.create();
    }
}
