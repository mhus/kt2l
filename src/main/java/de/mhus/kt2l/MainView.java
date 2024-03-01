package de.mhus.kt2l;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.security.core.userdetails.UserDetails;

import javax.annotation.PostConstruct;

@PermitAll
@Route(value = "/")
public class MainView extends AppLayout {

    private @Autowired
            @Getter
    AutowireCapableBeanFactory beanFactory;

    private final transient AuthenticationContext authContext;
    private XTabViewer tabViewer;

    public MainView(AuthenticationContext authContext) {
        this.authContext = authContext;
    }

    @PostConstruct
    public void createUi() {

        createHeader();
        createDrawer();

    }

    private void createHeader() {
        H1 logo = new H1("kt2l");
        logo.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.Margin.MEDIUM);

        final var header =
                authContext.getAuthenticatedUser(UserDetails.class).map(userDetails -> {

                    var logout = new Button("Logout", click -> authContext.logout());
                    var user = new Span("Welcome " + userDetails.getUsername());
                    return new HorizontalLayout(new DrawerToggle(), logo, user, logout);

                }).orElse(
                        new HorizontalLayout(/*new DrawerToggle(),*/ logo)
                );

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames(
                LumoUtility.Padding.Vertical.NONE,
                LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header);

    }

    private void createDrawer() {

        tabViewer = new XTabViewer(this);
        addToDrawer(tabViewer);

        tabViewer.addTab(new XTab("main", "Main", false, VaadinIcon.DASHBOARD.create(), new MainPanel())).select();
    }

}