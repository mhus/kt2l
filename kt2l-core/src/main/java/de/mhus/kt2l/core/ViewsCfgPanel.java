package de.mhus.kt2l.core;

import de.mhus.kt2l.cfg.CPanelVerticalLayout;
import de.mhus.kt2l.form.YBoolean;
import de.mhus.kt2l.form.YText;
import org.springframework.beans.factory.annotation.Configurable;

@Configurable
public class ViewsCfgPanel extends CPanelVerticalLayout {

    @Override
    public String getTitle() {
        return "Views";
    }

    @Override
    public void initUi() {
        add(new YBoolean()
                .path("core/darkMode")
                .label("Dark Mode as default")
                .defaultValue(true));
        add(new YBoolean()
                .path("core/autoDarkMode")
                .label("Automatic Dark Mode")
                .defaultValue(true));
        add(new YText()
                .path("localBash/bash")
                .label("Local bash")
                .defaultValue("/bin/bash"));
        add(new YText()
                .path("localBash/argument")
                .label("Local bash argument")
                .defaultValue("-i"));

    }
}
