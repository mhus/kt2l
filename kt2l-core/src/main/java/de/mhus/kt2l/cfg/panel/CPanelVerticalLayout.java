package de.mhus.kt2l.cfg.panel;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.mhus.commons.tree.ITreeNode;
import de.mhus.kt2l.cfg.CfgPanel;

import java.util.LinkedList;
import java.util.List;

public abstract class CPanelVerticalLayout extends VerticalLayout implements CfgPanel {

    private List<YComponent> components = new LinkedList<>();

    public void add(YComponent build) {
        build.initUi();
        components.add(build);
        add(build.getComponent());
    }

    @Override
    public Component getPanel() {
        return this;
    }

    @Override
    public void load(ITreeNode content) {
        components.forEach(component -> component.load(content));
    }

    @Override
    public void save(ITreeNode content) {
        components.forEach(component -> component.save(content));
    }

    @Override
    public boolean isValid() {
        return false;
    }

}
