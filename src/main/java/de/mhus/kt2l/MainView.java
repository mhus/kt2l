package de.mhus.kt2l;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveListener;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.security.core.userdetails.UserDetails;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

@PermitAll
@Route(value = "/")
@CssImport("./styles/custom.css")
@Slf4j
public class MainView extends AppLayout implements BeforeLeaveListener, BeforeEnterListener {

    private @Autowired
            @Getter
    AutowireCapableBeanFactory beanFactory;

    @Autowired
    ScheduledExecutorService scheduler;

    private final transient AuthenticationContext authContext;
    private XTabBar tabBar;
    private ScheduledFuture<?> closeScheduler;
    private Span tabTitle;

    public MainView(AuthenticationContext authContext) {
        this.authContext = authContext;
    }

    @PostConstruct
    public void createUi() {

        createHeader();
        createDrawer();

        LOGGER.info("Start Refresh Scheduler");
        closeScheduler = scheduler.scheduleAtFixedRate(this::fireRefresh, 10, 10, java.util.concurrent.TimeUnit.SECONDS);

    }

    private void createHeader() {
        H1 logo = new H1("kt2l");
        logo.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.Margin.MEDIUM);

        final var header =
                authContext.getAuthenticatedUser(UserDetails.class).map(userDetails -> {

                    tabTitle = new Span("");
                    tabTitle.setWidthFull();
                    if (userDetails != null && !userDetails.getUsername().equals("autologin")) { //XXX config
                        var space = new Span(" ");
                        var logout = new Button("Logout", click -> authContext.logout());
                        return new HorizontalLayout(new DrawerToggle(), logo, tabTitle, logout, space);
                    }
                    return new HorizontalLayout(new DrawerToggle(), logo, tabTitle);

                }).orElse(
                        new HorizontalLayout(logo)
                );

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames(
                LumoUtility.Padding.Vertical.NONE,
                LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header);

    }

    private void createDrawer() {

        tabBar = new XTabBar(this);
        addToDrawer(tabBar);

        tabBar.addTab(new XTab("main", "Main", false, VaadinIcon.DASHBOARD.create(), new MainPanel(this))).select();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
    }

    private void fireRefresh() {
        LOGGER.info("Refresh");
        try {
            final var selected = tabBar.getSelectedTab();
            if (selected != null) {
                final var panel = selected.getPanel();
                if (panel != null && panel instanceof XTabListener) {
                    ((XTabListener) panel).tabRefresh();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error refreshing", e);
        }
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        LOGGER.info("Cancel Refresh Scheduler");
        closeScheduler.cancel(false);
    }

    public void setWindowTitle(String title, XUi.COLOR color) {
        if (title == null)
            tabTitle.setText("");
        else
            tabTitle.setText(title);

        Arrays.stream(XUi.COLOR.values()).forEach(c -> tabTitle.removeClassNames("color-" + c.name().toLowerCase()));
        if (color != null && color != XUi.COLOR.NONE)
            tabTitle.addClassNames("color-" + color.name().toLowerCase());
    }

    public XTabBar getTabBar() {
        return tabBar;
    }
}