package de.mhus.kt2l;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.security.core.userdetails.UserDetails;

public class MainLayout extends AppLayout {

    private final transient AuthenticationContext authContext;

    public MainLayout(AuthenticationContext authContext) {
        this.authContext = authContext;

        H1 logo = new H1("kt2l");
        logo.addClassName("logo");
        HorizontalLayout
                header =
                authContext.getAuthenticatedUser(UserDetails.class)
                        .map(user -> {
                            Button logout = new Button("Logout", click ->
                                    this.authContext.logout());
                            Span loggedUser = new Span("Welcome " + user.getUsername());
                            return new HorizontalLayout(logo, loggedUser, logout);
                        }).orElseGet(() -> new HorizontalLayout(logo));

        // Other page components omitted.
        header.setWidthFull();
        addToNavbar(header);
    }
}