/*
 * kt2l-core - kt2l core implementation
 * Copyright © 2024 Mike Hummel (mh@mhus.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.kt2l.ai;

import de.mhus.kt2l.cfg.CPanelVerticalLayout;
import de.mhus.kt2l.form.YBoolean;
import de.mhus.kt2l.form.YText;
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
                .path("enabled")
                .label("Enabled")
                .defaultValue(false));
        add(new YText()
                .path("ollamaUrl")
                .label("ollama Url")
                .defaultValue("http://localhost:11434"));
        add(new YText()
                .path("openAiKey")
                .label("Open API Key")
                .defaultValue(""));
        add(new YText()
                .path("codingModel")
                .label("Model for coding")
                .defaultValue(""));
        add(new YText()
                .path("translateModel")
                .label("Model for translation")
                .defaultValue(""));

    }
}
