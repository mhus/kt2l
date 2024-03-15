package de.mhus.kt2l.ui;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.auth.NavigationAccessControl;

public class ViewAccessCheckerInitializer implements VaadinServiceInitListener {

    private  NavigationAccessControl navigationAccessControl;

    public ViewAccessCheckerInitializer() {
        navigationAccessControl = new  NavigationAccessControl();
        navigationAccessControl.setLoginView(LoginView.class);
    }

    @Override
    public void serviceInit(ServiceInitEvent serviceInitEvent) {
        serviceInitEvent.getSource().addUIInitListener(uiInitEvent -> {
            uiInitEvent.getUI().addBeforeEnterListener(navigationAccessControl);
        });
    }
}