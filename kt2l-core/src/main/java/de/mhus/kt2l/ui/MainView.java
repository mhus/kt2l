package de.mhus.kt2l.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.mhus.commons.tools.MSystem;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.Kt2lApplication;
import de.mhus.kt2l.pods.ContainerShellPanel;
import jakarta.annotation.security.PermitAll;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.security.core.userdetails.UserDetails;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;

import static de.mhus.commons.tools.MCollection.detached;

@PermitAll
@Route(value = "/")
@CssImport("./styles/custom.css")
@Uses(ContainerShellPanel.class)
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
    private Map<String, Map<String, ClusterBackgroundJob>> backgroundJobs = new HashMap<>();
    private long refreshCounter;
    private Button helpToggel;
    private VerticalLayout helpContent;
    private VerticalLayout contentContainer;
    private Component contentContent;
    private IFrame helpBrowser;

    public MainView(AuthenticationContext authContext) {
        this.authContext = authContext;
    }

    @PostConstruct
    public void createUi() {

        createContent();
        createHeader();
        createDrawer();

        LOGGER.info("Start Refresh Scheduler");
        closeScheduler = scheduler.scheduleAtFixedRate(this::fireRefresh, 1, 1, java.util.concurrent.TimeUnit.SECONDS);

    }

    private void createContent() {
        var content = new HorizontalLayout();
        content.setSizeFull();
        super.setContent(content);
        helpContent = new VerticalLayout();
        helpContent.setVisible(false);
        helpContent.setWidth("300px");
        helpContent.setHeightFull();

        var helpMenu = new MenuBar();
        helpMenu.addItem("Help", e -> helpBrowser.setSrc("https://google.de"));
        helpContent.add(helpMenu);

        helpBrowser = new IFrame();
        helpBrowser.setSizeFull();
        helpBrowser.getElement().setAttribute("frameborder", "0");
        helpBrowser.setSrc("/public/test.html");
        helpContent.add(helpBrowser);

        contentContainer = new VerticalLayout();
        content.add(contentContainer, helpContent);
    }

    @Override
    public Component getContent() {
        return contentContent;
    }

    @Override
    public void setContent(Component content) {
        contentContent = content;
        contentContainer.removeAll();
        if (content != null)
            contentContainer.add(content);
    }

    public void removeContent() {
        contentContent = null;
        contentContainer.removeAll();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        LOGGER.debug("UI on attach {}", MSystem.getObjectId(getUI().get()));
        heartbeatRegistration = getUI().get().addHeartbeatListener(event -> {
            LOGGER.debug("Heartbeat");
        });

    }

    protected void onDetach(DetachEvent detachEvent) {
        LOGGER.debug("UI on detach {}", MSystem.getObjectId(getUI().get()));
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

        final var header =
                authContext.getAuthenticatedUser(UserDetails.class).map(userDetails -> {

                    tabTitle = new Span("");
                    tabTitle.setWidthFull();
                    tabTitle.addClassName("ktool-title");

                    helpToggel = new Button(VaadinIcon.QUESTION_CIRCLE_O.create());
                    helpToggel.addClickListener(e -> {
                        helpContent.setVisible(!helpContent.isVisible());
                    });

                    var space = new Span(" ");


                    if (userDetails != null) {
                        UI.getCurrent().getSession().setAttribute(Kt2lApplication.UI_USERNAME, userDetails.getUsername());
                    }
                    if (userDetails != null && !userDetails.getUsername().equals("autologin")) { //XXX config
                        var logout = new Button("Logout", click -> authContext.logout());
                        return new HorizontalLayout(new DrawerToggle(), createLogo(), tabTitle, logout, helpToggel,space);
                    }
                    return new HorizontalLayout(new DrawerToggle(), createLogo(), tabTitle, helpToggel, space);

                }).orElse(
                        new HorizontalLayout(createLogo())
                );

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames(
                LumoUtility.Padding.Vertical.NONE,
                LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header);

    }

    private Component createLogo() {
        var span = new Span("[KT2L]");
        span.addClassNames("logo");
        return span;
    }

    private void createDrawer() {

        tabBar = new XTabBar(this);
        addToDrawer(tabBar);

        tabBar.addTab(new XTab("main", "Main", false, VaadinIcon.HOME_O.create(), new ClusterOverviewPanel(this))).select();
    }

    private void fireRefresh() {
        refreshCounter++;
        try {
            final var selected = tabBar.getSelectedTab();
            if (selected != null) {
                final var panel = selected.getPanel();
                if (panel != null && panel instanceof XTabListener) {
                    LOGGER.trace("Refresh selected panel {}", panel.getClass());
                    ((XTabListener) panel).tabRefresh(refreshCounter);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error refreshing", e);
        }
    }

    public void setWindowTitle(String title, UiUtil.COLOR color) {
        if (title == null)
            tabTitle.setText("");
        else
            tabTitle.setText(title);

        Arrays.stream(UiUtil.COLOR.values()).forEach(c -> tabTitle.removeClassNames("bgcolor-" + c.name().toLowerCase()));
        if (color != null && color != UiUtil.COLOR.NONE)
            tabTitle.addClassNames("bgcolor-" + color.name().toLowerCase());
    }

    public XTabBar getTabBar() {
        return tabBar;
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