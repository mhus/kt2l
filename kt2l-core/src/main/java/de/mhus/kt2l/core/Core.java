/*
 * kt2l-core - kt2l core implementation
 * Copyright © 2024 Mike Hummel (mh@mhus.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.mhus.kt2l.core;

import com.vaadin.componentfactory.IdleNotification;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.VaadinSessionState;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.mhus.commons.tools.MSystem;
import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.Kt2lApplication;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.cluster.ClusterOverviewPanel;
import de.mhus.kt2l.config.ViewsConfiguration;
import de.mhus.kt2l.help.HelpAction;
import de.mhus.kt2l.help.HelpConfiguration;
import de.mhus.kt2l.help.LinkHelpAction;
import de.mhus.kt2l.resources.ResourceDetailsPanel;
import de.mhus.kt2l.resources.ResourcesGridPanel;
import de.mhus.kt2l.resources.pod.ContainerShellPanel;
import de.mhus.kt2l.resources.pod.PodLogsPanel;
import jakarta.annotation.security.PermitAll;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.security.core.userdetails.UserDetails;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;

import static de.mhus.commons.tools.MCollection.detached;
import static de.mhus.commons.tools.MCollection.notNull;

@PermitAll
@Route(value = "/")
@PreserveOnRefresh
@CssImport("./styles/custom.css")
@Slf4j
// add Used to include js files in the build, only once per vaadin component is needed
@Uses(ContainerShellPanel.class)
@Uses(ResourcesGridPanel.class)
@Uses(ResourceDetailsPanel.class)
@Uses(PodLogsPanel.class)

public class Core extends AppLayout {

    private @Autowired
            @Getter
    AutowireCapableBeanFactory beanFactory;

    @Autowired
    private ScheduledExecutorService scheduler;

    @Autowired
    private HelpConfiguration helpConfiguration;

    @Autowired
    private List<HelpAction> helpActions;

    @Autowired(required=false)
    private List<CoreListener> coreListeners;

    @Autowired
    private ViewsConfiguration viewsConfiguration;

    private final transient AuthenticationContext authContext;
    private DeskTabBar tabBar;
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
    private ContextMenu helpMenu;
    private UI ui;
    private VaadinSession session;

    public Core(AuthenticationContext authContext) {
        this.authContext = authContext;
    }

    @PostConstruct
    public void createUi() {
        if (closeScheduler != null) {
            LOGGER.debug("Session already created");
            return;
        }

        createContent();
        createHeader();
        createDrawer();

        LOGGER.info("Start Refresh Scheduler");
        closeScheduler = scheduler.scheduleAtFixedRate(this::fireRefresh, 1, 1, java.util.concurrent.TimeUnit.SECONDS);

        if (coreListeners != null)
            coreListeners.forEach(l -> l.onCoreCreated(this));
    }

    private void createContent() {
        var content = new HorizontalLayout();
        content.setSizeFull();
        super.setContent(content);
        contentContainer = new VerticalLayout();

        if (helpConfiguration.isEnabled()) {
            helpContent = new VerticalLayout();
            helpContent.setVisible(false);
            helpContent.setWidth(helpConfiguration.getWindowWidth());
            helpContent.setHeightFull();

            helpBrowser = new IFrame();
            helpBrowser.setSizeFull();
            helpBrowser.getElement().setAttribute("frameborder", "0");
            helpBrowser.setSrc("/public/docs/index.html");
            helpContent.add(helpBrowser);
            helpContent.setClassName("helpcontent");
        }
        content.add(notNull(contentContainer, helpContent));

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
        ui = attachEvent.getUI();
        LOGGER.debug("UI on attach {}", MSystem.getObjectId(ui));

        var idleConf = viewsConfiguration.getConfig("core").getObject("idle").orElse(MTree.EMPTY_MAP);
        if (idleConf.getBoolean("enabled", true)) {
            IdleNotification idleNotification = new IdleNotification();

            idleNotification.setSecondsBeforeNotification( idleConf.getInt("notifyBeforeSeconds", 90) );
            idleNotification.setMessage("Your session will expire in " +
                    IdleNotification.MessageFormatting.SECS_TO_TIMEOUT
                    + " seconds.");
            idleNotification.addExtendSessionButton("Extend session");
            idleNotification.addRedirectButton("Logout now", "/reset");
            idleNotification.addCloseButton();
            idleNotification.setExtendSessionOnOutsideClick(true);
            idleNotification.addOpenListener(event -> {
                LOGGER.debug("Idle Notification Opened");
                if (idleConf.getBoolean("autoExtend", true))
                    idleNotification.getElement().executeJs(
                            "var self=this;setTimeout(() => { try {self.click(); }" +
                                    " catch (error) {console.log(error);} }, " +
                                    idleConf.getInt("autoExtendWaitSeconds", 5) * 1000 +
                                    ");");
            });
            ui.add(idleNotification);
        }

        ui.getPage().setTitle("KT2L");
        session = ui.getSession();
        heartbeatRegistration = getUI().get().addHeartbeatListener(event -> {
            LOGGER.debug("Heartbeat");
        });
    }

    protected synchronized void closeSession() {
        if (session == null) return;
        LOGGER.debug("Close Session");
        closeScheduler.cancel(false);
        detached(tabBar.getTabs()).forEach(DeskTab::closeTab);
        clusteredJobsCleanup();
        if (heartbeatRegistration != null)
            heartbeatRegistration.remove();
        if (coreListeners != null)
            coreListeners.forEach(l -> l.onCoreDestroyed(this));
        session = null;
    }
    protected void onDetach(DetachEvent detachEvent) {
        LOGGER.debug("UI on detach {} on session {}", MSystem.getObjectId(detachEvent.getUI()), session == null ? "?" : session.getState());
        checkSession();
        ui = null;
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

                    if (helpConfiguration.isEnabled()) {
                        helpToggel = new Button(VaadinIcon.QUESTION_CIRCLE_O.create());
                        helpMenu = new ContextMenu();
                        helpMenu.setTarget(helpToggel);
                        helpMenu.setOpenOnClick(true);
                    }
                    var space = new Span(" ");


                    if (userDetails != null) {
                        UI.getCurrent().getSession().setAttribute(Kt2lApplication.UI_USERNAME, userDetails.getUsername());
                    }
                    if (userDetails != null && !userDetails.getUsername().equals("autologin")) { //XXX config
                        var logout = new Button("Logout", click -> authContext.logout());
                        return new HorizontalLayout(notNull(new DrawerToggle(), createLogo(), tabTitle, logout, helpToggel,space));
                    }
                    return new HorizontalLayout(notNull(new DrawerToggle(), createLogo(), tabTitle, helpToggel, space));

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
    
    protected void updateHelpMenu(boolean setDefaultDocu) {
        if (!helpConfiguration.isEnabled()) return;

        String helpContext = getTabBar().getSelectedTab().getHelpContext();
        var ctx = helpConfiguration.getContext(helpContext == null ? "default" : helpContext);
        helpMenu.removeAll();
        var links = ctx.getLinks();
        links.forEach(
            link -> {
                if (!link.isEnabled()) return;
                var action = getHelpAction(link);
                if (action == null) return;
                link.setHelpAction(action);
                var item = helpMenu.addItem(link.getName(), event -> {
                    action.execute(this, link);
                });
                var icon = action.getIcon(link);
                if (icon != null) {
                    icon.getStyle().set("width", "var(--lumo-icon-size-s)");
                    icon.getStyle().set("height", "var(--lumo-icon-size-s)");
                    icon.getStyle().set("marginRight", "var(--lumo-space-s)");
                    item.addComponentAsFirst(icon);
                }
            }
        );
        if (helpContent.isVisible()) {
            if (!links.isEmpty())
                helpMenu.add(new Hr());
            helpMenu.addItem("Close Help", event -> {
                if (!helpContent.isVisible()) return;
                helpContent.setVisible(false);
                updateHelpMenu(false);
            });
            if (setDefaultDocu) {
                links.stream().filter(link ->
                            link.getHelpAction() != null &&
                            !(link.getHelpAction() instanceof LinkHelpAction) &&
                            link.isDefault())
                        .findFirst().ifPresent(
                        link -> {
                            link.getHelpAction().execute(this, link);
                        }
                );
            }
        }
    }

    private HelpAction getHelpAction(HelpConfiguration.HelpLink link) {
        return helpActions.stream().filter(a -> a.canHandle(link)).findFirst().orElse(null);
    }

    public void showHelp() {
        if (!helpConfiguration.isEnabled()) return;
        if (helpContent.isVisible()) return;
        helpContent.setVisible(true);
        updateHelpMenu(true);
    }

    private Component createLogo() {
        var span = new Span("[KT2L]");
        span.addClassNames("logo");
        return span;
    }

    private void createDrawer() {

        tabBar = new DeskTabBar(this);
        addToDrawer(tabBar);

        tabBar.addTab(new DeskTab("main", "Main", false, VaadinIcon.HOME_O.create(), new ClusterOverviewPanel(this))).select();
    }

    private void fireRefresh() {
        if (session == null) return;
        try {
//            LOGGER.debug("Refresh for session {} and ui {}", session, ui == null ? "?" : Objects.toIdentityString(ui));
            refreshCounter++;


//            if (ui != null) {
//                ui.access(() -> {
//                    checkSession();
//                });
//            }

            final var selected = tabBar.getSelectedTab();
            if (selected != null) {
                final var panel = selected.getPanel();
                if (panel != null && panel instanceof DeskTabListener) {
                    LOGGER.trace("Refresh selected panel {}", panel.getClass());
                    ((DeskTabListener) panel).tabRefresh(refreshCounter);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error refreshing", e);
        }
    }

    private void checkSession() {
        try {
            if (session == null) return;
            if (session.getState() != VaadinSessionState.OPEN) {
                closeSession();
            }
        } catch (Exception e) {
            LOGGER.error("Error checking session", e);
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

    public DeskTabBar getTabBar() {
        return tabBar;
    }

    private void handleKeyShortcut(ShortcutEvent shortcutEvent) {
        LOGGER.debug("Shortcut: {}", shortcutEvent.getKey().getKeys().get(0));
        final var selected = tabBar.getSelectedTab();
        if (selected != null) {
            final var panel = selected.getPanel();
            if (panel != null && panel instanceof DeskTabListener) {
                LOGGER.debug("Shortcut to panel {}", panel.getClass());
                ((DeskTabListener) panel).tabShortcut(shortcutEvent);
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

    public void setHelpUrl(String url) {
        if (!helpConfiguration.isEnabled()) return;
        helpContent.removeAll();
        helpContent.add(helpBrowser);
        helpBrowser.setSrc(url);
        showHelp();
        helpBrowser.reload();
    }
    public void setHelpPanel(Component helpComponent) {
        if (!helpConfiguration.isEnabled()) return;
        helpContent.removeAll();
        helpContent.add(helpComponent);
        showHelp();
    }

    public UI ui() {
        return ui;
    }
}