package de.mhus.kt2l.cfg.panel;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import de.mhus.commons.tree.ITreeNode;

public class YBoolean extends YComponent<Boolean> {
    private Checkbox component;

    @Override
    public void initUi() {
        component = new Checkbox();
        component.setLabel(label);
        component.setWidthFull();
    }

    @Override
    public Component getComponent() {
        return component;
    }

    @Override
    public void load(ITreeNode content) {
        component.setValue(content.getBoolean(name, defaultValue));
    }

    @Override
    public void save(ITreeNode node) {
        node.put(name, component.getValue());
    }
}
