package de.mhus.kt2l.help;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.commons.tools.MSystem;
import de.mhus.kt2l.core.MainView;
import org.springframework.stereotype.Component;

@Component
public class LinkHelpAction implements HelpAction {
    @Override
    public boolean canHandle(HelpConfiguration.HelpLink link) {
        return "link".equals(link.getAction());
    }

    @Override
    public void execute(MainView mainView, HelpConfiguration.HelpLink link) {
        link.getNode().getString("href").ifPresent(href ->
                MSystem.openBrowserUrl(href)
        );
    }

    @Override
    public Icon getIcon(HelpConfiguration.HelpLink link) {
        return VaadinIcon.EXTERNAL_LINK.create();
    }
}
