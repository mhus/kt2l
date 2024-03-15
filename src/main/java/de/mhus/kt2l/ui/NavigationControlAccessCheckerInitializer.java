package de.mhus.kt2l.ui;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.auth.NavigationAccessControl;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NavigationControlAccessCheckerInitializer implements VaadinServiceInitListener {

    private NavigationAccessControl accessControl;

    public NavigationControlAccessCheckerInitializer() {
        LOGGER.info("START");
        accessControl = new NavigationAccessControl();
        accessControl.setLoginView(LoginView.class);
    }

    @Override
    public void serviceInit(ServiceInitEvent serviceInitEvent) {
        serviceInitEvent.getSource().addUIInitListener(uiInitEvent -> {
            uiInitEvent.getUI().addBeforeEnterListener(accessControl);
        });
    }
}