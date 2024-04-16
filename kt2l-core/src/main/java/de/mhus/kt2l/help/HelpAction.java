package de.mhus.kt2l.help;

import com.vaadin.flow.component.icon.Icon;
import de.mhus.commons.tools.MSystem;
import de.mhus.kt2l.config.HelpConfiguration;
import de.mhus.kt2l.core.MainView;

public interface HelpAction {

    boolean canHandle(HelpConfiguration.HelpLink link);

    void execute(MainView mainView, HelpConfiguration.HelpLink link);

    Icon getIcon(HelpConfiguration.HelpLink link);
}
