package de.mhus.kt2l.form;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.mhus.commons.tree.ITreeNode;

import java.util.LinkedList;
import java.util.List;

public abstract class FormPanelVerticalLayout extends VerticalLayout implements FormPanel {

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
        components.forEach(component -> {
            if (!component.readOnly)
                component.save(content);
        });
    }

    @Override
    public boolean isValid() {
        return false;
    }

}
