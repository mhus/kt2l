package de.mhus.kt2l.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import lombok.Getter;

import java.util.Arrays;

public class XTab extends HorizontalLayout {

    private final Icon icon;
    @Getter
    private final String tabId;
    @Getter
    private final Text tabTitle; // in the tab
    @Getter
    private String windowTitle; // window top
    @Getter
    private UiUtil.COLOR color = UiUtil.COLOR.NONE;
    @Getter
    private final boolean closeable;
    @Getter
    private Component panel;
    @Getter
    private XTabBar viewer;
    @Getter
    private XTab parentTab;
    @Getter
    private String helpContext;

//    @Getter
//    private Map<String, Object> parameters = new HashMap<>();

    public XTab(String tabId, String title, boolean closeable, Icon icon, Component panel) {
        if (icon == null)
            icon = VaadinIcon.FILE.create();
        this.icon = icon;
        icon.setClassName("tabicon");
        this.tabId = tabId;
        this.closeable = closeable;
        this.panel = panel;
        windowTitle = title;
        tabTitle = new Text(title);
        Span span = new Span();
        span.setClassName("tabtext");
        span.add(tabTitle);
        add(icon, span);
        if (closeable) {
            Icon close = VaadinIcon.CLOSE.create();
            close.addClickListener(click -> {
                closeTab();
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

    public void closeTab() {
        viewer.closeTab(this);
        panel = null;
    }

    public void setXTabViewer(XTabBar tabViewer) {
        this.viewer = tabViewer;
        viewer.getMainView().getBeanFactory().autowireBean(panel);
    }

    public XTab setParentTab(XTab parent) {
        this.parentTab = parent;
        return this;
    }

    public XTab setHelpContext(String helpContext) {
        this.helpContext = helpContext;
        return this;
    }

    public XTab select() {
        viewer.setSelected(this);
        return this;
    }

    public void setShowButtonAsSelected(boolean selected) {
        if (selected) {
            addClassName("selected");
        } else {
            removeClassName("selected");
        }
    }

    public XTab setWindowTitle(String title) {
        this.windowTitle = title;
        return this;
    }

    public XTab setColor(UiUtil.COLOR color) {
        this.color = color;
        if (icon != null) {
            Arrays.stream(UiUtil.COLOR.values()).forEach(c -> removeClassName("color-" + c.name().toLowerCase()));
            icon.addClassName("color-" + color.name().toLowerCase());
        }
        return this;
    }

}
