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
import com.vaadin.componentfactory.ToggleButton;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.VaadinSessionState;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.mhus.commons.tools.MCast;
import de.mhus.commons.tools.MLang;
import de.mhus.commons.tools.MObject;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tools.MSystem;
import de.mhus.commons.tools.MThread;
import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.Kt2lApplication;
import de.mhus.kt2l.aaa.AaaUser;
import de.mhus.kt2l.aaa.LoginConfiguration;
import de.mhus.kt2l.aaa.SecurityContext;
import de.mhus.kt2l.aaa.SecurityService;
import de.mhus.kt2l.aaa.SecurityUtils;
import de.mhus.kt2l.aaa.UsersConfiguration;
import de.mhus.kt2l.aaa.oauth2.AuthProvider;
import de.mhus.kt2l.cfg.CfgService;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.cluster.ClusterOverviewPanel;
import de.mhus.kt2l.config.AbstractUserRelatedConfig;
import de.mhus.kt2l.config.Configuration;
import de.mhus.kt2l.config.ViewsConfiguration;
import de.mhus.kt2l.help.HelpAction;
import de.mhus.kt2l.help.HelpConfiguration;
import de.mhus.kt2l.help.LinkHelpAction;
import de.mhus.kt2l.resources.ResourcesGridPanel;
import de.mhus.kt2l.resources.common.ResourceYamlEditorPanel;
import de.mhus.kt2l.resources.pod.ContainerShellPanel;
import de.mhus.kt2l.resources.pod.PodLogsPanel;
import de.mhus.kt2l.resources.util.AbstractClusterWatch;
import de.mhus.kt2l.ui.UiUtil;
import jakarta.annotation.security.PermitAll;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;
import org.vaadin.addons.visjs.network.main.NetworkDiagram;
import org.vaadin.olli.FileDownloadWrapper;

import javax.annotation.PostConstruct;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;

import static de.mhus.commons.tools.MCollection.detached;
import static de.mhus.commons.tools.MCollection.notNull;
import static org.apache.logging.log4j.util.Strings.isBlank;

@PermitAll
@Route(value = "/")
@PreserveOnRefresh
@CssImport("./styles/custom.css")
@CssImport(
        themeFor = "vaadin-grid",
        value = "./styles/grid.css"
)
@JavaScript("./scripts/jquery/3.4.1/jquery.min.js")
@Slf4j
// add Used to include js files in the build, only once per vaadin component is needed
@Uses(ContainerShellPanel.class)
@Uses(ResourcesGridPanel.class)
@Uses(ResourceYamlEditorPanel.class)
@Uses(PodLogsPanel.class)
@Uses(FileDownloadWrapper.class)
@Uses(NetworkDiagram.class)
@Uses(ToggleButton.class)
@Uses(IntegerField.class)
public class Core extends AppLayout {

    private long uiTemeoutSeconds = 60;

    private @Autowired
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

    @Autowired
    private CfgService cfgService;

    @Autowired
    private LoginConfiguration loginConfiguration;

    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private AuthProvider authProvider;

    @Autowired
    private transient AuthenticationContext authContext;
    @Getter
    private DeskTabBar tabBar;
    private ScheduledFuture<?> closeScheduler;
    private Span tabTitle;
    private final Map<String, Map<String, ClusterBackgroundJob>> backgroundJobs = new HashMap<>();
    private long refreshCounter;
    private Button helpToggle;
    private VerticalLayout helpContent;
    private VerticalLayout contentContainer;
    private Component contentContent;
    private IFrame helpBrowser;
    private ContextMenu helpMenu;
    private UI ui;
    private VaadinSession session;
    private String sessionId;
    @Getter
    private DeskTab homeTab;
    @Autowired
    private SecurityService securityService;
    private long uiLost = 0;
    private boolean helpSticky;
    private boolean trackBrowserMemoryUsage = true;
    @Getter
    private String browserMemoryUsage;
    @Value("${kt2l.deskTabPreserveMode:true}")
    private boolean deskTabPreserveMode;
    @Getter
    private ContextMenu generalContextMenu;
    private boolean uiLostEnabled = false;
    private volatile boolean darkMode = false;
    private volatile boolean autoDarkMode = false;
    private MenuItem darkModeToggle;
    private MenuItem autoDarkModeToggle;

    @PostConstruct
    public void createUi() {

        ui = UI.getCurrent();
        session = UI.getCurrent().getSession();
        sessionId = session.getSession().getId();

        var maybeUser = authProvider.fetchUserFromContext();
        if (maybeUser.isEmpty()) {
            LOGGER.error("㋡ {} No user found (createUi)", sessionId);
            SecurityUtils.exitToLogin("No User Found");
            return;
        }
        var user = maybeUser.get();
        LOGGER.info("### UI with user '{}' and session '{}'", user.getUserId(), sessionId);
        UI.getCurrent().getSession().setAttribute(SecurityService.UI_USER, user);
        UI.getCurrent().getSession().setAttribute("autologin", "true");

        if (!MSystem.isVmDebug()) {
            generalContextMenu = new ContextMenu();
            generalContextMenu.setTarget(this);
            generalContextMenu.addItem("Reload", e -> ui().getPage().reload());
        }

        uiLostEnabled = viewsConfiguration.getConfig("core").getBoolean("uiLostEnabled", uiLostEnabled);
        uiTemeoutSeconds = viewsConfiguration.getConfig("core").getLong("uiTimeoutSeconds", uiTemeoutSeconds);
        trackBrowserMemoryUsage = viewsConfiguration.getConfig("core").getBoolean("trackBrowserMemoryUsage", trackBrowserMemoryUsage);
        autoDarkMode = viewsConfiguration.getConfig("core").getBoolean("autoDarkMode", false);
        if (autoDarkMode) {
            darkMode = false;
            checkAutoDarkMode();
        } else
            //noinspection NonAtomicOperationOnVolatileField
            darkMode = viewsConfiguration.getConfig("core").getBoolean("darkMode", darkMode);


        if (closeScheduler != null) {
            LOGGER.debug("㋡ {} Session already created", sessionId);
            return;
        }

        createContent();
        createHeader(user);
        createDrawer();

        LOGGER.debug("㋡ {} Start Refresh Scheduler", sessionId);
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
            helpSticky = helpConfiguration.isSticky();
            createHelpContent();
        }
        content.add(notNull(contentContainer, helpContent));

    }

    private void createHelpContent() {
        helpContent = new VerticalLayout();
        helpContent.setVisible(false);
        helpContent.setWidth(helpConfiguration.getWindowWidth());
        helpContent.setPadding(false);
        helpContent.setMargin(false);
        helpContent.setHeightFull();

        helpBrowser = new IFrame();
        helpBrowser.setSizeFull();
        helpBrowser.getElement().setAttribute("frameborder", "0");
        helpBrowser.setSrc("/docs/index.html");
        helpContent.add(helpBrowser);
        helpContent.setClassName("helpcontent");
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
        if (SecurityUtils.getUser() == null) {
            LOGGER.error("㋡ {} No user found (onAttach)", sessionId);
            SecurityUtils.exitToLogin("No User Found");
            return;
        }
        ui = attachEvent.getUI();
        uiLost = 0;

        session = ui.getSession();
        sessionId = session.getSession().getId();
        LOGGER.debug("㋡ {} UI attach {}", sessionId, Objects.hashCode(ui));
        createIdleNotification();

        ui.addHeartbeatListener(event -> {
            LOGGER.debug("♥ {} UI Heartbeat ({})", event.getSource().getSession().getSession().getId(), event.getSource().getSession().getBrowser().getBrowserApplication());
        });

        Thread.startVirtualThread(() -> {
            MThread.sleep(300);
            ui.access(() -> {
                ui.getPage().setTitle("KT2L");
                switchDarkMode(darkMode);
            });
        });

    }

    private void createIdleNotification() {
        var idleConf = viewsConfiguration.getConfig("core").getObject("idle").orElse(MTree.EMPTY_MAP);
        if (idleConf.getBoolean("enabled", true)) {
            ui().getChildren().filter(c -> c instanceof IdleNotification).forEach(ui()::remove);
            LOGGER.debug("㋡ {} Create Idle Notification for UI {}", sessionId, Objects.hashCode(ui));
            IdleNotification idleNotification = new IdleNotification();
            idleNotification.setSecondsBeforeNotification( Math.max( 90, idleConf.getInt("notifyBeforeSeconds", 90)) );
            var maxInactiveInterval = idleConf.getInt("maxInactiveIntervalSeconds", 0);
            if (maxInactiveInterval > 0)
                idleNotification.setMaxInactiveInterval( maxInactiveInterval );
            idleNotification.setMessage("Your session will expire in " +
                    IdleNotification.MessageFormatting.SECS_TO_TIMEOUT
                    + " seconds.");
            idleNotification.addExtendSessionButton("Extend session");
            idleNotification.addRedirectButton("Logout now", "/reset");
            idleNotification.addCloseButton();
            idleNotification.setExtendSessionOnOutsideClick(true);
            idleNotification.addExtendSessionListener(event -> {
                LOGGER.debug("㋡ {} Idle Notification Extend Session", sessionId);
            });
            idleNotification.addOpenListener(event -> {
                LOGGER.debug("㋡ {} Idle Notification Opened", sessionId);
                if (idleConf.getBoolean("autoExtend", true))
                    idleNotification.getElement().executeJs(
                            "var self=this;setTimeout(() => { try {self.click(); }" +
                                    " catch (error) {console.log(error);} }, " +
                                    idleConf.getInt("autoExtendWaitSeconds", 1) * 1000 +
                                    ");");
            });
            ui().add(idleNotification);
        }
    }

    protected synchronized void closeSession() {
        if (session == null) return;
        LOGGER.debug("㋡ {} Close Session", sessionId);
        if (closeScheduler != null)
            closeScheduler.cancel(false);
        if (tabBar != null)
            detached(tabBar.getTabs()).forEach(DeskTab::closeTab);
        clusteredJobsClose();
        if (coreListeners != null)
            coreListeners.forEach(l -> l.onCoreDestroyed(this));
        session = null;
    }
    protected void onDetach(DetachEvent detachEvent) {
        LOGGER.debug("㋡ {} UI {} onDetach with session state {}", sessionId, Objects.hashCode(detachEvent.getUI()), session == null ? "?" : session.getState());
        checkSession();
        ui = null;
        uiLost = System.currentTimeMillis();
    }

    private void clusteredJobsClose() {
        synchronized (backgroundJobs) {
            backgroundJobs.values().forEach(m -> m.values().forEach(ClusterBackgroundJob::close));
            backgroundJobs.clear();
        }
    }

    private void clusteredJobsCleanup() {
        LOGGER.trace("㋡ {} Cleanup Clustered Jobs",sessionId);
        synchronized (backgroundJobs) {
            backgroundJobs.values().forEach(map -> {
                map.values().removeIf(job -> {
                    if (job instanceof AbstractClusterWatch<?> watch) {
                        if (watch.getEventHandler().size() == 0) {
                            LOGGER.debug("㋡ {} Close idle Job {}", sessionId, watch.getClass().getSimpleName());
                            watch.close();
                            return true;
                        }
                    }
                    return false;
                });
            });
        }
    }

    private void createHeader(AaaUser user) {

        tabTitle = new Span("");
        tabTitle.setWidthFull();
        tabTitle.addClassName("ktool-title");

        if (helpConfiguration.isEnabled()) {
            helpToggle = new Button(VaadinIcon.QUESTION_CIRCLE_O.create());
            helpToggle.addClassNames("help-toggle");
            helpMenu = new ContextMenu();
            helpMenu.setTarget(helpToggle);
            helpMenu.setOpenOnClick(true);
            Shortcuts.addShortcutListener(helpToggle, () -> {
                if (helpContent.isVisible())
                    helpContent.setVisible(false);
                else
                    showHelp(true); // open the help menu instead, not possible at the moment
            }, Key.KEY_H, KeyModifier.CONTROL);
        }
        var space = new Span(" ");

        Button userButton = new Button(LumoIcon.USER.create());
        userButton.setTooltipText(user.getUserId());
        var userMenu = new ContextMenu();
        userMenu.setTarget(userButton);
        userMenu.setOpenOnClick(true);

        var darkModeMenu = userMenu.addItem("Dark mode").getSubMenu();

        darkModeToggle = darkModeMenu.addItem("Dark");
        darkModeToggle.setCheckable(true);
        darkModeToggle.addClickListener(e -> {
            if (e.isFromClient()) {
                switchAutoDarkMode(false);
                switchDarkMode(e.getSource().isChecked());
            }
        });
        darkModeToggle.setChecked(darkMode);

        autoDarkModeToggle = darkModeMenu.addItem("Automatic");
        autoDarkModeToggle.setCheckable(true);
        autoDarkModeToggle.addClickListener(e -> {
            if (e.isFromClient()) {
                switchAutoDarkMode(e.getSource().isChecked());
            }
        });
        autoDarkModeToggle.setChecked(autoDarkMode);

        if (cfgService.isUserCfgEnabled()) {
            UiUtil.createIconItem(userMenu, VaadinIcon.COG, "User Settings", null, true).addClickListener(click -> {
                cfgService.showUserCfg(this);
            });
        }
        if (cfgService.isGlobalCfgEnabled()) {
            UiUtil.createIconItem(userMenu, VaadinIcon.COGS, "Global Settings", null, true).addClickListener(click -> {
                cfgService.showGlobalCfg(this);
            });
        }
        if (user.getDisplayName().equals(loginConfiguration.getAutoLoginUser())) {
            UiUtil.createIconItem(userMenu, VaadinIcon.RECYCLE, "Reset Session", null, true).addClickListener(click -> {
                resetSession();
            });
        } else {
            UiUtil.createIconItem(userMenu, VaadinIcon.SIGN_OUT, "Logout " + user.getDisplayName(), null, true).addClickListener(click -> {
                resetSession();
            });
        }
        if (securityService.hasRole(UsersConfiguration.ROLE.ADMIN.name()) && Kt2lApplication.canRestart()) {
            UiUtil.createIconItem(userMenu, VaadinIcon.WARNING, "Restart Server", null, true).addClickListener(click -> {
                ConfirmDialog dialog = new ConfirmDialog();
                dialog.setHeader("Restart");
                dialog.setText("Do you really want to restart the server?");
                dialog.setConfirmText("Restart");
                dialog.setCancelable(true);
                dialog.addConfirmListener(e -> Kt2lApplication.restart());
                dialog.open();
            });
        }
        var header = new HorizontalLayout(notNull(new DrawerToggle(), createLogo(), tabTitle, userButton, helpToggle, space));

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames(
                LumoUtility.Padding.Vertical.NONE,
                LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header);
    }

    private void switchAutoDarkMode(boolean auto) {
        ui.access(() -> {
            autoDarkMode = auto;
            if (autoDarkModeToggle != null && autoDarkModeToggle.isChecked() != autoDarkMode) {
                autoDarkModeToggle.setChecked(autoDarkMode);
            }
            if (autoDarkMode) {
                checkAutoDarkMode();
            }
        });
    }

    private void checkAutoDarkMode() {
        ui.access(() -> {
            this.getElement().executeJs("return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches").then(Boolean.class, res -> {
                if (res != null) {
                    LOGGER.trace("㋡ {} Auto Dark Mode {}", sessionId, res);
                    if (darkMode != res) {
                        LOGGER.debug("㋡ {} Switch dark mode {}", sessionId, darkMode);
                        switchDarkMode(res);
                    }
                }
            });
        });
    }

    private void switchDarkMode(boolean dark) {
        ui.access(() -> {
            darkMode = dark;
            var js = "document.documentElement.setAttribute('theme', $0)";
            getElement().executeJs(js, darkMode ? Lumo.DARK : Lumo.LIGHT);
            if (darkModeToggle != null && darkModeToggle.isChecked() != darkMode)
                darkModeToggle.setChecked(darkMode);
        });
    }

    protected void updateHelpMenu(boolean setDefaultDocu) {
        if (helpMenu == null || !helpConfiguration.isEnabled()) return;

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

        if (!links.isEmpty())
            helpMenu.add(new Hr());

        var helpStickyToggel = helpMenu.addItem("Sticky");
        helpStickyToggel.setCheckable(true);
        helpStickyToggel.setChecked(helpSticky);
        helpStickyToggel.addClickListener(event -> {
            helpSticky = helpStickyToggel.isChecked();
        });

        if (helpContent.isVisible()) {
            helpMenu.addItem("Close Help", event -> {
                if (!helpContent.isVisible()) return;
                helpContent.setVisible(false);
                updateHelpMenu(false);
            });
            if (setDefaultDocu && !helpSticky) {
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
        LOGGER.debug("㋡ {} Get Help Action for {}", sessionId, link.getAction());
        return helpActions.stream().filter(a -> a.canHandle(link)).findFirst().orElse(null);
    }

    public void showHelp(boolean setDefaultDocu) {
        if (!helpConfiguration.isEnabled()) return;
        if (helpContent.isVisible()) return;
        helpContent.setVisible(true);
        updateHelpMenu(setDefaultDocu);
    }

    private Component createLogo() {
        var span = new Span("[KT2L]");
        span.addClassNames("logo");
        return span;
    }

    private void createDrawer() {

        tabBar = new DeskTabBar(this, deskTabPreserveMode);
        tabBar.setMargin(false);
        addToDrawer(tabBar);

        createMainTab();
    }

    private void createMainTab() {
        homeTab = tabBar.addTab(
                new DeskTab(
                        "home",
                        "Home",
                        false,
                        VaadinIcon.HOME_O.create(),
                        new ClusterOverviewPanel(this))
        ).setReproducable(true).select();
    }

    private void fireRefresh() {
        if (session == null) return;
        try {
            LOGGER.trace("㋡ {} Refresh for session {}", sessionId, session);
            if (uiLostEnabled && ui == null && uiLost > 0) {
                if (System.currentTimeMillis() - uiLost > uiTemeoutSeconds*1000) {
                    LOGGER.error("㋡ {} UI lost, try to close session", sessionId);
                    closeSession();
                }
                return;
            }
            refreshCounter++;
            // get browser info
            if (trackBrowserMemoryUsage &&  refreshCounter % 300 == 0 && ui != null) {
                ui.access(() -> {
                    getElement().executeJs("return performance && performance.memory ? performance.memory.jsHeapSizeLimit + \" \" + performance.memory.totalJSHeapSize + \" \" + performance.memory.usedJSHeapSize : \"\"").then(String.class, value -> {
                        if (!isBlank(value)) {
                            browserMemoryUsage = value;
                            LOGGER.debug("㋡ {} Browser Memory {} ({})", sessionId, MString.toByteDisplayString(MCast.tolong(MString.afterLastIndex(value, ' '), 0)), value);
                        }
                    });
                });
            }
            // cleanup cluster jobs
            if (refreshCounter % 10 == 0) {
                clusteredJobsCleanup();
            }
            // check for dark mode
            if (autoDarkMode && refreshCounter % 15 == 0) {
                checkAutoDarkMode();
            }
            // refresh selected tab
            final var selected = tabBar.getSelectedTab();
            if (selected != null) {
                final var panel = selected.getPanel();
                if (panel instanceof DeskTabListener deskTabListener) {
                    LOGGER.trace("㋡ {} Refresh selected panel {}", sessionId, deskTabListener.getClass());
                    deskTabListener.tabRefresh(refreshCounter);
                }
            }
        } catch (Exception e) {
            LOGGER.error("㋡ {} Error refreshing", sessionId, e);
        }
    }

    private void checkSession() {
        try {
            if (session == null) return;
            if (session.getState() != VaadinSessionState.OPEN) {
                closeSession();
            }
        } catch (Exception e) {
            LOGGER.error("㋡ {} Error checking session", sessionId, e);
        }
    }

    public void setWindowTitle(String title, UiUtil.COLOR color) {
        tabTitle.setText(Objects.requireNonNullElse(title, ""));

        Arrays.stream(UiUtil.COLOR.values()).forEach(c -> tabTitle.removeClassNames("title-" + c.name().toLowerCase()));
        if (color != null && color != UiUtil.COLOR.NONE)
            tabTitle.addClassNames("title-" + color.name().toLowerCase());
    }

    public <T extends ClusterBackgroundJob> T backgroundJobInstance(Cluster cluster, Class<T> watchClass) {
        return (T)getBackgroundJob(cluster.getName(), watchClass, () -> MObject.newInstance(watchClass));
    }

    @SuppressWarnings("unchecked")
    <T extends ClusterBackgroundJob> T getBackgroundJob(String clusterId, Class<? extends T> jobId, Supplier<T> create) {
        return (T) getBackgroundJob(clusterId, jobId.getName(), create::get);
    }

    @SuppressWarnings("unchecked")
    <T extends ClusterBackgroundJob> Optional<T> getBackgroundJob(String clusterId, Class<? extends T> jobId) {
        return (Optional<T>)getBackgroundJob(clusterId, jobId.getName());
    }

    public ClusterBackgroundJob getBackgroundJob(String clusterId, String jobId, Supplier<ClusterBackgroundJob> create) {
        synchronized (backgroundJobs) {
            return backgroundJobs.computeIfAbsent(clusterId, k -> new HashMap<>()).computeIfAbsent(jobId, k -> {
                LOGGER.debug("㋡ {} Create Job {}/{} with class {}", sessionId, clusterId, jobId, k);
                try {
                    final var job = create.get();
                    autowireObject(job);
                    job.init(this, clusterId, jobId);
                    return job;
                } catch (Exception e) {
                    LOGGER.error("㋡ {} Create Job {}", sessionId, k, e);
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
        showHelp(true);
        helpBrowser.reload();
    }
    public void setHelpPanel(Component helpComponent) {
        if (!helpConfiguration.isEnabled()) return;
        autowireObject(helpComponent);
        helpContent.removeAll();
        helpContent.add(helpComponent);
        showHelp(false);
    }

    public UI ui() {
        if (ui == null) return UI.getCurrent();
        return ui;
    }

    public void resetSession() {
        try {
            springContext.getBean(Configuration.class).clearCache();
            var userName = SecurityContext.lookupUserId();
            String[] beanNames = springContext.getBeanDefinitionNames();
            for (String beanName : beanNames) {
                var bean = springContext.getBean(beanName);
                if (bean instanceof AbstractUserRelatedConfig config) {
                    LOGGER.debug("㋡ {} Clear cache for {} and user {}", sessionId, beanName, userName);
                    config.clearCache(userName);
                }
            }
        } catch (Exception t) {
            LOGGER.warn("㋡ {} Can't clear cache", sessionId, t);
        }
        ui.getSession().close();
        closeSession();
        authContext.logout();
    }

    public int getBackgroundJobCount() {
        return backgroundJobs.values().stream().mapToInt(Map::size).sum();
    }

    public List<String> getBackgroundJobClusters() {
        return backgroundJobs.keySet().stream().toList();
    }

    @SuppressWarnings("unchecked")
    public List<String> getBackgroundJobIds(String clusterId) {
        return backgroundJobs.getOrDefault(clusterId, Collections.EMPTY_MAP).keySet().stream().toList();
    }

    public <T> T getBean(Class<T> type) {
        return springContext.getBean(type);
    }

    public void autowireObject(Object object) {
        if (object == null) return;
        // beanFactory.autowireBean(object);
        // hello native image ....
        var className = object.getClass().getCanonicalName();
        try {
            // inject fields
            ReflectionUtils.doWithFields(object.getClass(), field -> {
                try {
                    var anno = field.getAnnotation(Autowired.class);
                    field.setAccessible(true);
                    // if (field.get(object) != null) continue;

                    if (field.getType() == List.class) {
                        var genericType = field.getGenericType().getTypeName();
                        var listType = genericType.substring(genericType.indexOf("<") + 1, genericType.indexOf(">"));
                        var beanList = new ArrayList<>(springContext.getBeansOfType(Class.forName(listType)).values());
                        field.set(object, beanList);
                    } else {
                        var bean = MLang.tryThis(() -> (Object) beanFactory.getBean(field.getName(), field.getType()))
                                .orElseTry(() -> (Object) beanFactory.getBean(field.getType()))
                                .orElseTry(() -> beanFactory.getBean(field.getName()))
                                .orElse(null);
                        if (bean == null) {
                            if (anno.required()) {
                                LOGGER.error("Bean not found: {} {} in class {}", field.getName(), field.getType(), className, new Exception());
                                throw new BeanCreationException("Bean not found: %s %s in class %s".formatted(field.getName(), field.getType(), className));
                            }
                        } else {
                            field.set(object, bean);
                        }
                    }
                } catch (BeansException e) {
                    throw e;
                } catch (Exception e) {
                    LOGGER.error("Error in {} {}", className, field.getName(), e);
                }
            }, field -> field.getAnnotation(Autowired.class) != null && !Modifier.isStatic(field.getModifiers()));
            // inject methods
            ReflectionUtils.doWithMethods(object.getClass(), method -> {
                try {
                    var anno = method.getAnnotation(Autowired.class);
                    if (anno == null) return;
                    var params = Arrays.stream(method.getParameters()).map(p -> {
                        try {
                            return beanFactory.getBean(p.getType());
                        } catch (Exception e) {
                            LOGGER.error("Error in {} {}", className, method.getName(), e);
                            return null;
                        }
                    }).toArray();
                    method.invoke(object, params);
                } catch (Exception e) {
                    LOGGER.error("Error in {} {}", className, method.getName(), e);
                }
            }, method -> method.getAnnotation(Autowired.class) != null && !Modifier.isStatic(method.getModifiers()));
            // execute post construct
            ReflectionUtils.doWithMethods(object.getClass(), method -> {
                try {
                    var anno = method.getAnnotation(PostConstruct.class);
                    if (anno == null) return;
                    method.invoke(object);
                } catch (Exception e) {
                    LOGGER.error("Error in {} {}", className, method.getName(), e);
                }
            }, method -> method.getAnnotation(PostConstruct.class) != null && !Modifier.isStatic(method.getModifiers()));

        } catch (Exception e) {
            LOGGER.error("Error in {}", className, e);
        }

    }

}