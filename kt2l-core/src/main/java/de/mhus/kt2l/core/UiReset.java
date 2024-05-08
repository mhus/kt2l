package de.mhus.kt2l.core;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.PermitAll;

@AnonymousAllowed
@Route(value = "/reset")
@Slf4j
public class UiReset extends VerticalLayout {

    public UiReset() {
        LOGGER.info("UiReset");
        var backButton = new Button("KT2L", e -> getUI().ifPresent(ui -> ui.navigate("/")));
        backButton.setIcon(VaadinIcon.BACKSPACE_A.create());
        backButton.setAutofocus(true);
        backButton.setWidthFull();
        add(backButton);
    }

}
