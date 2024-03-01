package de.mhus.kt2l;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public class MainLayout extends AppLayout {

    private final transient AuthenticationContext authContext;

    public MainLayout(AuthenticationContext authContext) {
        this.authContext = authContext;
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

        SideNav nav = new SideNav();

        SideNavItem mainLink = new SideNavItem("Main", MainView.class, VaadinIcon.DASHBOARD.create());

        nav.addItem(mainLink);
        final var routes = ((List<RouteEntry>)UI.getCurrent().getSession().getAttribute("routes"));
        if (routes != null) {
            routes.forEach(route -> {
                    nav.addItem(new SideNavItem(
                            route.name(),
                            route.clazz(),
                            route.parameters(),
                            route.icon()
                            ));
                });
        }
        addToDrawer(nav);
        UI.getCurrent().getSession().setAttribute("nav", nav);
    }

    public static synchronized void addRoute(String name, Class<? extends Component> clazz, RouteParameters parameters, Component icon) {
        var routes = ((List<RouteEntry>)UI.getCurrent().getSession().getAttribute("routes"));
        if (routes == null) {
            routes = new java.util.ArrayList<RouteEntry>();
            UI.getCurrent().getSession().setAttribute("routes", routes);
        }
        if (routes != null) {
            routes.add(new RouteEntry(name, clazz, parameters, icon));
        }
        var nav = (SideNav)UI.getCurrent().getSession().getAttribute("nav");
        if (nav != null) {
            nav.addItem(new SideNavItem(name, clazz, parameters, icon));
        }
    }
}