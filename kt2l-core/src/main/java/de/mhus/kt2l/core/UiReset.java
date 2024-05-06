package de.mhus.kt2l.core;

import com.vaadin.flow.component.button.Button;
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
        add(new Button("Go back", e -> getUI().ifPresent(ui -> ui.navigate("/"))));
    }

}
