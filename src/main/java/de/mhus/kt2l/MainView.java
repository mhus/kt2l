package de.mhus.kt2l;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.mhus.commons.tools.MSystem;
import jakarta.annotation.security.PermitAll;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.security.core.userdetails.UserDetails;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;

import static de.mhus.commons.tools.MCollection.detached;

@PermitAll
@Route(value = "/")
@CssImport("./styles/custom.css")
@Slf4j
public class MainView extends AppLayout {

    private @Autowired
            @Getter
    AutowireCapableBeanFactory beanFactory;

    @Autowired
    ScheduledExecutorService scheduler;

    private final transient AuthenticationContext authContext;
    private XTabBar tabBar;
    private ScheduledFuture<?> closeScheduler;
    private Span tabTitle;
    private Registration heartbeatRegistration;
    private Set<String> registeredKeyShortcuts = new HashSet<>();
    private Map<String, Map<String,ClusterBackgroundJob>> backgroundJobs = new HashMap<>();

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

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        LOGGER.error("UI on attach {}", MSystem.getObjectId(getUI().get()));
        heartbeatRegistration = getUI().get().addHeartbeatListener(event -> {
            LOGGER.debug("Heartbeat");
        });

    }

    protected void onDetach(DetachEvent detachEvent) {
        LOGGER.error("UI on detach {}", MSystem.getObjectId(getUI().get()));
        closeScheduler.cancel(false);
        detached(tabBar.getTabs()).forEach(XTab::closeTab);
        clusteredJobsCleanup();
        if (heartbeatRegistration != null)
            heartbeatRegistration.remove();
    }

    private void clusteredJobsCleanup() {
        synchronized (backgroundJobs) {
            backgroundJobs.values().forEach(m -> m.values().forEach(ClusterBackgroundJob::close));
            backgroundJobs.clear();
        }
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
                    if (userDetails != null) {
                        UI.getCurrent().getSession().setAttribute(Kt2lApplication.UI_USERNAME, userDetails.getUsername());
                    }
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

    private void fireRefresh() {
        try {
            final var selected = tabBar.getSelectedTab();
            if (selected != null) {
                final var panel = selected.getPanel();
                if (panel != null && panel instanceof XTabListener) {
                    LOGGER.debug("Refresh selected panel {}", panel.getClass());
                    ((XTabListener) panel).tabRefresh();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error refreshing", e);
        }
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

    public void registerKeyShortcut(Key key) {
        if (registeredKeyShortcuts.contains(key.toString()))
            return;
        registeredKeyShortcuts.add(key.toString());
        getUI().get().addShortcutListener(this::handleKeyShortcut, key);

    }

    private void handleKeyShortcut(ShortcutEvent shortcutEvent) {
        LOGGER.debug("Shortcut: {}", shortcutEvent.getKey().getKeys().get(0));
        final var selected = tabBar.getSelectedTab();
        if (selected != null) {
            final var panel = selected.getPanel();
            if (panel != null && panel instanceof XTabListener) {
                LOGGER.debug("Shortcut to panel {}", panel.getClass());
                ((XTabListener) panel).tabShortcut(shortcutEvent);
            }
        }
    }

    public <T extends ClusterBackgroundJob> T getBackgroundJob(String clusterId, Class<? extends T> jobId, Supplier<T> create) {
        return (T) getBackgroundJob(clusterId, jobId.getName(), () -> create.get());
    }

    public <T extends ClusterBackgroundJob> Optional<T> getBackgroundJob(String clusterId, Class<? extends T> jobId) {
        return (Optional<T>)getBackgroundJob(clusterId, jobId.getName());
    }

    public ClusterBackgroundJob getBackgroundJob(String clusterId, String jobId, Supplier<ClusterBackgroundJob> create) {
        synchronized (backgroundJobs) {
            return backgroundJobs.computeIfAbsent(clusterId, k -> new HashMap<>()).computeIfAbsent(jobId, k -> {
                try {
                    final var job = create.get();
                    beanFactory.autowireBean(job);
                    job.init(this, clusterId, jobId);
                    return job;
                } catch (Exception e) {
                    LOGGER.error("Create Job {}", k, e);
                    return null;
                }
            });
        }
    }

    public Optional<ClusterBackgroundJob> getBackgroundJob(String clusterId, String jobId) {
        synchronized (backgroundJobs) {
            return Optional.ofNullable(backgroundJobs.computeIfAbsent(clusterId, k -> new HashMap<>()).get(jobId));
        }
    }

}