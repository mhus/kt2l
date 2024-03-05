package de.mhus.kt2l;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class XTabViewer extends VerticalLayout {

    private final MainView mainView;
    private List<XTab> tabs = new LinkedList<>();
    private XTab selectedTab;

    public XTabViewer(MainView mainView) {
        setWidthFull();
        this.mainView = mainView;
        addClassName("xtabview");
    }

    public XTab addTab(String id, String title, boolean closeable, boolean unique, Icon icon, Supplier<Component> panelCreator) {
        if (unique) {
            Optional<XTab> tab = getTab(id);
            if (tab.isPresent()) {
                return tab.get();
            }
        }
        final var newTab = new XTab(id, title, closeable, icon, panelCreator.get());
        return addTab(newTab);
    }

    public XTab addTab(XTab tab) {
        tabs.add(tab);
        tab.setXTabViewer(this);
        add(tab);
        return tab;
    }

    public void closeTab(XTab tab) {
        if (selectedTab == tab) {
            setSelected(tabs.getFirst());
        }
        tabs.remove(tab);
        remove(tab);
    }

    public Optional<XTab> getTab(String main) {
        return tabs.stream().filter(t -> t.getTabId().equals(main)).findFirst();
    }

    public void setSelected(XTab tab) {
        if (selectedTab == tab) return;
        selectedTab = tab;
        tabs.forEach(t -> t.setSelectedVision(t == tab));
        mainView.setContent(tab.getPanel());

//        mainView.showRouterLayoutContent(panel);
    }

    public MainView getMainView() {
        return mainView;
    }
}
