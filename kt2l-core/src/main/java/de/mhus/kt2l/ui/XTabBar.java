package de.mhus.kt2l.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.mhus.commons.tools.MCollection;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
public class XTabBar extends VerticalLayout {

    private final MainView mainView;
    private List<XTab> tabs = new LinkedList<>();
    private XTab selectedTab;

    public XTabBar(MainView mainView) {
        setWidthFull();
        this.mainView = mainView;
        addClassName("xtabview");
    }

    synchronized XTab addTab(String id, String title, boolean closeable, boolean unique, Icon icon, Supplier<Component> panelCreator) {
        if (unique) {
            Optional<XTab> tab = getTab(id);
            if (tab.isPresent()) {
                return tab.get();
            }
        }
        final var newTab = new XTab(id, title, closeable, icon, panelCreator.get());
        return addTab(newTab);
    }

    public synchronized XTab addTab(XTab tab) {
        tabs.add(tab);
        tab.setXTabViewer(this);
        add(tab);

        if (tab.getPanel() != null && tab.getPanel() instanceof XTabListener) {
            Try.run(() -> ((XTabListener) tab.getPanel()).tabInit(tab)).onFailure(e -> LOGGER.warn("TabListener:tabInit failed", e));
        }

        return tab;
    }

    // internal, use getTab().closeTab()
    synchronized void closeTab(XTab tab) {

        if (selectedTab == tab) {
            setSelected(
                    MCollection.contains(tabs, selectedTab.getParentTab())
                            ? selectedTab.getParentTab() : null );
        }

        if (tab.getPanel() != null && tab.getPanel() instanceof XTabListener)
            Try.run(() -> ((XTabListener) tab.getPanel()).tabDestroyed()).onFailure(e -> LOGGER.warn("TabListener:tabDestroyed failed", e));

        tabs.remove(tab);
        remove(tab);
    }

    public Optional<XTab> getTab(String main) {
        return tabs.stream().filter(t -> t.getTabId().equals(main)).findFirst();
    }

    public synchronized void setSelected(XTab tab) {
        // deselect
        if (selectedTab != null) {
            if (selectedTab.getPanel() != null && selectedTab.getPanel() instanceof XTabListener) {
                Try.run(() -> ((XTabListener) selectedTab.getPanel()).tabUnselected()).onFailure(e -> LOGGER.warn("TabListener:tabDeselected failed", e));
            }
        }
        // select fallback
        if (tab == null && !tabs.isEmpty() && tabs.get(0) != selectedTab) {
            tab = tabs.get(0);
        }
        // cleanup tab buttons
        final var finalTab = tab;
        tabs.forEach(t -> t.setShowButtonAsSelected(t == finalTab));
        // select
        selectedTab = tab;
        if (selectedTab != null) {
            mainView.setContent(selectedTab.getPanel());
            mainView.setWindowTitle(selectedTab.getWindowTitle(), selectedTab.getColor());
            if (selectedTab.getPanel() != null && selectedTab.getPanel() instanceof XTabListener) {
                Try.run(() -> ((XTabListener) selectedTab.getPanel()).tabSelected()).onFailure(e -> LOGGER.warn("TabListener:tabSelected failed", e));
            }
            mainView.updateHelpMenu(true);

        }
    }

    public MainView getMainView() {
        return mainView;
    }

    public XTab getSelectedTab() {
        return selectedTab;
    }

    public List<XTab> getTabs() {
        return tabs;
    }
}
