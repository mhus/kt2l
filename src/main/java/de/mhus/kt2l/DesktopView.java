package de.mhus.kt2l;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@PermitAll
@Route(value = "/", layout = MainLayout.class)
public class DesktopView extends VerticalLayout {

    public DesktopView() {
        add(new Text("Welcome to MainView."));
    }

}