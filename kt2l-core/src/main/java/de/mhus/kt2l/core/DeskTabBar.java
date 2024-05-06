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
import com.vaadin.flow.component.UI;
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
public class DeskTabBar extends VerticalLayout {

    private final Core core;
    private List<DeskTab> tabs = new LinkedList<>();
    private DeskTab selectedTab;

    public DeskTabBar(Core core) {
        setWidthFull();
        this.core = core;
        addClassName("xtabview");
    }

    synchronized DeskTab addTab(String id, String title, boolean closeable, boolean unique, Icon icon, Supplier<Component> panelCreator) {
        if (unique) {
            Optional<DeskTab> tab = getTab(id);
            if (tab.isPresent()) {
                return tab.get();
            }
        }
        final var newTab = new DeskTab(id, title, closeable, icon, panelCreator.get());
        return addTab(newTab);
    }

    public synchronized DeskTab addTab(DeskTab tab) {
        tabs.add(tab);
        tab.setXTabViewer(this);
        add(tab);

        if (tab.getPanel() != null && tab.getPanel() instanceof DeskTabListener) {
            Try.run(() -> ((DeskTabListener) tab.getPanel()).tabInit(tab)).onFailure(e -> LOGGER.warn("TabListener:tabInit failed", e));
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
            Try.run(() -> ((DeskTabListener) tab.getPanel()).tabDestroyed()).onFailure(e -> LOGGER.warn("TabListener:tabDestroyed failed", e));

        tabs.remove(tab);
        remove(tab);
    }

    public Optional<DeskTab> getTab(String main) {
        return tabs.stream().filter(t -> t.getTabId().equals(main)).findFirst();
    }

    public synchronized void setSelected(DeskTab tab) {
        // deselect
        if (selectedTab != null) {
            if (selectedTab.getPanel() != null && selectedTab.getPanel() instanceof DeskTabListener) {
                Try.run(() -> ((DeskTabListener) selectedTab.getPanel()).tabUnselected()).onFailure(e -> LOGGER.warn("TabListener:tabDeselected failed", e));
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
            core.setContent(selectedTab.getPanel());
            core.setWindowTitle(selectedTab.getWindowTitle(), selectedTab.getColor());
            if (selectedTab.getPanel() != null && selectedTab.getPanel() instanceof DeskTabListener) {
                Try.run(() -> ((DeskTabListener) selectedTab.getPanel()).tabSelected()).onFailure(e -> LOGGER.warn("TabListener:tabSelected failed", e));
            }
            core.updateHelpMenu(true);
            UI.getCurrent().getPage().setTitle("KT2L " + selectedTab.getWindowTitle());
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
