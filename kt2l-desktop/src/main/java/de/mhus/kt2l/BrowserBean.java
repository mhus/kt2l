/*
 * kt2l-desktop - kt2l desktop app
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
/**
 * This file is part of kt2l-desktop.
 *
 * kt2l-desktop is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * kt2l-desktop is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with kt2l-desktop.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.kt2l;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BrowserBean {

    @Autowired
    private ServletWebServerApplicationContext webServerAppCtxt;
    private String baseUrl;

//    public static void setShutdownMessage() {
//        var htmlStr = "<html><body>Shutting down ...</body></html>";
//        Kt2lDesktopApplication.getBrowserInstances().forEach(b -> b.getBrowser().setText(htmlStr));
//    }

    // https://github.com/jcefmaven/jcefmaven
    @PostConstruct
    public void init() {
        LOGGER.info("BrowserBean initialized");
    }

    public void loadURL(String url) {
        baseUrl = url;
        LOGGER.info("Loading URL: {}", url);
        Kt2lDesktopApplication.getDisplay().syncExec(() -> {
            Kt2lDesktopApplication.getBrowserInstances().forEach(b -> b.getBrowser().setUrl(url));
        });
    }

    public void openNewWindow() {
        Kt2lDesktopApplication.getDisplay().syncExec(() -> {
            new BrowserInstance().getBrowser().setUrl(baseUrl);
        });
    }

    public void loadLocalURL() {
        loadURL("http://localhost:" + webServerAppCtxt.getWebServer().getPort());
    }

}
