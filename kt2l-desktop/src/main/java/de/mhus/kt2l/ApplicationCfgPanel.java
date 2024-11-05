package de.mhus.kt2l;

import de.mhus.kt2l.cfg.CPanelVerticalLayout;
import de.mhus.kt2l.form.YBoolean;
import de.mhus.kt2l.form.YText;
import org.springframework.beans.factory.annotation.Configurable;

@Configurable
public class ApplicationCfgPanel extends CPanelVerticalLayout {
    @Override
    public String getTitle() {
        return "Application";
    }

    @Override
    public void initUi() {
        add(new YBoolean()
                .path("enableDebugLog")
                .label("Enable Debug Log")
                .defaultValue(false));
        add(new YText()
                .path("path")
                .label("Path (all)")
                .defaultValue(""));
        add(new YText()
                .path("pathAdditional")
                .label("Path (additional)")
                .defaultValue(""));
    }
}
