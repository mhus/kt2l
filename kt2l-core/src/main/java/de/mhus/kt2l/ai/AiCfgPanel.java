package de.mhus.kt2l.ai;

import de.mhus.kt2l.cfg.panel.CPanelVerticalLayout;
import de.mhus.kt2l.cfg.panel.YBoolean;
import de.mhus.kt2l.cfg.panel.YText;
import org.springframework.beans.factory.annotation.Configurable;

@Configurable
public class AiCfgPanel extends CPanelVerticalLayout {
    @Override
    public String getTitle() {
        return "AI";
    }

    @Override
    public void initUi() {
        add(new YBoolean()
                .name("enabled")
                .label("Enabled")
                .defaultValue(false));
        add(new YText()
                .name("ollamaUrl")
                .label("ollama Url")
                .defaultValue("http://localhost:11434"));
        add(new YText()
                .name("openAiKey")
                .label("Open API Key")
                .defaultValue(""));

    }
}
