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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import de.mhus.kt2l.config.ViewsConfiguration;
import de.mhus.kt2l.ui.UiUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class DeskTab extends HorizontalLayout {

    private final AbstractIcon icon;
    @Getter
    private final String tabId;
    @Getter
    private final Div tabTitle; // in the tab
    @Getter
    private String windowTitle; // window top
    @Getter
    private UiUtil.COLOR color = UiUtil.COLOR.NONE;
    @Getter
    private final boolean closeable;
    @Getter
    private Component panel;
    @Getter
    private DeskTabBar tabBar;
    @Getter
    private DeskTab parentTab;
    @Getter
    private String helpContext;
    @Getter
    private boolean reproducable = false;
    @Getter
    private boolean panelClosed = false;

//    @Getter
//    private Map<String, Object> parameters = new HashMap<>();

    public DeskTab(String tabId, String title, boolean closeable, AbstractIcon icon, Component panel) {
        if (icon == null)
            icon = VaadinIcon.FILE.create();
        this.icon = icon;
        icon.setClassName("tabicon");
        this.tabId = tabId;
        this.closeable = closeable;
        this.panel = panel;
        windowTitle = title;
        tabTitle = new Div(title);
        Span span = new Span();
        span.setClassName("tabtext");
        span.add(tabTitle);
        add(icon, span);
        if (closeable) {
            Icon close = VaadinIcon.CLOSE.create();
            close.addClickListener(click -> {
                closeTabClicked();
            });
            close.setClassName("tabclose");
            add(close);
        }
        addClassName("tabitem");
        setWidthFull();

        addClickListener(click -> {
            select();
        });
    }

    public void closeTabClicked() {
        if (panel instanceof DeskTabCloseClickedListener ask) {
            ask.tabCloseClicked(this);
        } else {
            closeTab();
        }
    }

    public void closeTab() {
        tabBar.closeTab(this);
        panel = null;
    }

    public void setTabViewer(DeskTabBar tabViewer) {
        this.tabBar = tabViewer;
        var vb = tabBar.getCore().getBean(ViewsConfiguration.class);
        LOGGER.debug("Autowire {} {} and config {}", getWindowTitle(), panel, vb);
        tabBar.getCore().autowireObject(panel);
    }

    public DeskTab setParentTab(DeskTab parent) {
        this.parentTab = parent;
        return this;
    }

    public DeskTab setHelpContext(String helpContext) {
        this.helpContext = helpContext;
        return this;
    }

    public DeskTab select() {
        tabBar.setSelected(this);
        return this;
    }

    public void setShowButtonAsSelected(boolean selected) {
        if (selected) {
            addClassName("selected");
        } else {
            removeClassName("selected");
        }
    }

    public DeskTab setWindowTitle(String title) {
        this.windowTitle = title;
        if (tabBar != null) tabBar.updateWindowTitle(this);
        return this;
    }

    public DeskTab setColor(UiUtil.COLOR color) {
        this.color = color;
        if (icon != null) {
            Arrays.stream(UiUtil.COLOR.values()).forEach(c -> removeClassName("color-" + c.name().toLowerCase()));
            icon.addClassName("color-" + color.name().toLowerCase());
        }
        return this;
    }

    /**
     * Set to false if the content will be lost after switching to another tab. In this case the
     * content will be hidden instead of removed. In this case the tab will allocate memory in the
     * browser for all live time. Default is false.
     *
     * @param reproducable
     * @return
     */
    public DeskTab setReproducable(boolean reproducable) {
        this.reproducable = reproducable;
        return this;
    }

    public DeskTab setTabTitle(String title) {
        tabTitle.setText(title);
        return this;
    }

    public void setPanelClosed(boolean panelClosed) {
        if (panelClosed == this.panelClosed) return;
        this.panelClosed = panelClosed;
        try {
            if (panelClosed) {
                tabTitle.addClassName("strikethrough");
            } else {
                tabTitle.removeClassName("strikethrough");
            }
        } catch (Exception e) {
            LOGGER.warn("Error", e);
        }
    }

}
