package de.mhus.kt2l;

import de.mhus.commons.tools.MFile;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BrowserBean {

    @Autowired
    private ServletWebServerApplicationContext webServerAppCtxt;

    public static void setStartupMessage() {
        var html = new StringBuffer();
        html.append("<html><body>Booting [KT2L] ...<br><br><br><center>");
        try {
            html.append( MFile.readFile(Kt2lApplication.class.getResourceAsStream("/images/kt2l-logo.svg") ) );
        } catch (Exception e) {
            LOGGER.warn("Logo not found", e);
        }
        html.append("</center></body></html>");
        Kt2lDesktopApplication.getBrowser().setText(html.toString());
    }

    public static void setShutdownMessage() {
        Kt2lDesktopApplication.getBrowser().setText("<html><body>Shutting down ...</body></html>");
    }

    // https://github.com/jcefmaven/jcefmaven
    @PostConstruct
    public void init() {
        LOGGER.info("BrowserBean initialized");
    }

    public void loadURL(String url) {
        LOGGER.info("Loading URL: {}", url);
        Kt2lDesktopApplication.getDisplay().syncExec(() -> {
            Kt2lDesktopApplication.getBrowser().setUrl(url);
        });
    }

    public void loadLocalURL() {
        loadURL("http://localhost:" + webServerAppCtxt.getWebServer().getPort());
    }

}
