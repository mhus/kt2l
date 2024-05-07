package de.mhus.kt2l.help;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.core.Core;
import org.springframework.stereotype.Component;

@Component
public class YamlSnippetsHelperAction implements HelpAction {

    @Override
    public boolean canHandle(HelpConfiguration.HelpLink link) {
        return "yaml-snippets".equals(link.getAction());
    }

    @Override
    public void execute(Core core, HelpConfiguration.HelpLink link) {
        YamlSnippetsHelpPanel panel = new YamlSnippetsHelpPanel(core, link);
        panel.setSizeFull();
        core.setHelpPanel(panel);
        panel.init();
    }

    @Override
    public Icon getIcon(HelpConfiguration.HelpLink link) {
        return VaadinIcon.FILE_ADD.create();
    }
}
