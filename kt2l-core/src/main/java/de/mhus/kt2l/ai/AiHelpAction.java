package de.mhus.kt2l.ai;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.help.HelpConfiguration;
import de.mhus.kt2l.core.MainView;
import de.mhus.kt2l.help.HelpAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiHelpAction implements HelpAction {

    @Autowired
    private AiConfiguration aiConfiguration;

    @Autowired
    private AiService aiService;

    @Override
    public boolean canHandle(HelpConfiguration.HelpLink link) {
        return "ai".equals(link.getAction()) && aiConfiguration.isEnabled();
    }

    @Override
    public void execute(MainView mainView, HelpConfiguration.HelpLink link) {
        AiHelpPanel panel = new AiHelpPanel(mainView, link, aiService);
        panel.setSizeFull();
        mainView.setHelpPanel(panel);
        panel.getPrompt().focus();

    }

    @Override
    public Icon getIcon(HelpConfiguration.HelpLink link) {
        return VaadinIcon.CROSSHAIRS.create();
    }
}
