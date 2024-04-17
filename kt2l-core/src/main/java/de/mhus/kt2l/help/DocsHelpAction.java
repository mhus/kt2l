package de.mhus.kt2l.help;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.core.MainView;
import org.springframework.stereotype.Component;

@Component
public class DocsHelpAction implements HelpAction {

    @Override
    public boolean canHandle(HelpConfiguration.HelpLink link) {
        return "docs".equals(link.getAction());
    }

    @Override
    public void execute(MainView mainView, HelpConfiguration.HelpLink link) {
        link.getNode().getString("document").ifPresent(href ->
                mainView.setHelpUrl("/public/docs/" + href + ".html")
        );
    }

    @Override
    public Icon getIcon(HelpConfiguration.HelpLink link) {
        return VaadinIcon.FILE_O.create();
    }
}
