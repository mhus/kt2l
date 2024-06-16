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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.mhus.commons.lang.Function0;
import de.mhus.commons.tools.MCollection;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
public class DeskTabBar extends VerticalLayout {

    private final Core core;
    private final VerticalLayout content;
    private List<DeskTab> tabs = new LinkedList<>();
    private DeskTab selectedTab;
    /**
     * Preserve mode will keep the panel in the content area even if the tab is not selected.
     * This is useful for tabs that are not reproducable and should not be recreated.
     *
     * Need to disable preserve mode if in integration tests. Vaadin will throw an assertation exception
     * if the addClassname() / removeClassname() is called on a component in this environment - don't know why.
     */
    private boolean preserveMode = true;

    public DeskTabBar(Core core, boolean preserveMode) {
        this.preserveMode = preserveMode;
        LOGGER.info("DeskTabBar preserve mode: {}", preserveMode);
        setWidthFull();
        this.core = core;
        content = new VerticalLayout();
        content.setSizeFull();
        content.setMargin(false);
        content.setPadding(false);
        content.setSpacing(false);
        core.setContent(content);
        addClassName("desktabview");
    }

    synchronized DeskTab addTab(String id, String title, boolean closeable, boolean unique, AbstractIcon icon, Function0<Component> panelCreator) {
        if (unique) {
            Optional<DeskTab> tab = getTab(id);
            if (tab.isPresent()) {
                return tab.get();
            }
        }
        try {
            final var newTab = new DeskTab(id, title, closeable, icon, panelCreator.apply());
            return addTab(newTab);
        } catch (Exception e) {
            LOGGER.error("addTab {}", id, e);
            UiUtil.showErrorNotification("Error open '" + title + "': " + e.getMessage());
            return null;
        }
    }

    public synchronized DeskTab addTab(DeskTab tab) {
        tabs.add(tab);
        tab.setTabViewer(this);
        add(tab);

        if (tab.getPanel() != null && tab.getPanel() instanceof DeskTabListener) {
            tryThis(() -> ((DeskTabListener) tab.getPanel()).tabInit(tab)).onFailure(e -> LOGGER.warn("TabListener:tabInit failed", e));
        }
        if (preserveMode && !tab.isReproducable() && tab.getPanel() != null) {
            var panel = tab.getPanel();
            panel.addClassName("hidden-tab");
            content.add(panel);
        }
        return tab;
    }

    // internal, use getTab().closeTab()
    synchronized void closeTab(DeskTab tab) {

        if (selectedTab == tab) {
            setSelected(
                    MCollection.contains(tabs, selectedTab.getParentTab())
                            ? selectedTab.getParentTab() : null );
        }

        if (tab.getPanel() != null && tab.getPanel() instanceof DeskTabListener)
            tryThis(() -> ((DeskTabListener) tab.getPanel()).tabDestroyed()).onFailure(e -> LOGGER.warn("TabListener:tabDestroyed failed", e));

        tabs.remove(tab);
        content.remove(tab.getPanel());
        remove(tab);
    }

    public Optional<DeskTab> getTab(String main) {
        return tabs.stream().filter(t -> t.getTabId().equals(main)).findFirst();
    }

    public synchronized void setSelected(DeskTab tab) {
        // deselect
        if (selectedTab != null) {
            if (selectedTab.getPanel() != null) {
                if (selectedTab.getPanel() instanceof DeskTabListener deskTabListener) {
                    tryThis(() -> deskTabListener.tabUnselected()).onFailure(e -> LOGGER.warn("TabListener:tabDeselected failed", e));
                }
                if (!preserveMode || selectedTab.isReproducable())
                    content.remove(selectedTab.getPanel());
            }
        }
        // select fallback
        if (tab == null && !tabs.isEmpty() && tabs.get(0) != selectedTab) {
            tab = tabs.get(0);
        }
        // cleanup tab buttons
        final var finalTab = tab;
        tabs.forEach(t -> t.setShowButtonAsSelected(t == finalTab));
        // cleanup content
        if (preserveMode)
            tabs.forEach(t -> tryThis(() -> {if (t == finalTab) t.getPanel().removeClassName("hidden-tab"); else t.getPanel().addClassName("hidden-tab");  } ));
        // select
        selectedTab = tab;
        if (selectedTab != null) {
            if (selectedTab.getPanel() != null) {
                if (!preserveMode || selectedTab.isReproducable())
                    content.add(selectedTab.getPanel());
                if (selectedTab.getPanel() instanceof DeskTabListener deskTabListener) {
                    tryThis(() -> deskTabListener.tabSelected()).onFailure(e -> LOGGER.warn("TabListener:tabSelected failed", e));
                }
            }
            core.setWindowTitle(selectedTab.getWindowTitle(), selectedTab.getColor());
            core.updateHelpMenu(true);
            // do not set title UI.getCurrent().getPage().setTitle("KT2L " + selectedTab.getWindowTitle());
        }
    }

    public Core getCore() {
        return core;
    }

    public DeskTab getSelectedTab() {
        return selectedTab;
    }

    public List<DeskTab> getTabs() {
        return tabs;
    }
}
