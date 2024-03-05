package de.mhus.kt2l;

import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;

@PWA(
        name = "Vaadin Application-Theme Demo",
        shortName = "App-Theme Demo"
)
@Theme("custom")
public class AppShellConfigurator implements com.vaadin.flow.component.page.AppShellConfigurator {
}
