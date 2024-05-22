package de.mhus.kt2l.cfg.panel;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import de.mhus.commons.tree.ITreeNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class YCombobox extends YComponent<String> {

    private ComboBox<String> component;

    private List<String> values = new ArrayList<>();

    public YCombobox values(Collection<String> values) {
        this.values.addAll(values);
        return this;
    }

    public YCombobox values(String... values) {
        for (String v : values)
            this.values.add(v);
        return this;
    }

    @Override
    public void initUi() {
        component = new ComboBox<String>();
        component.setLabel(label);
        component.setItems(values);
        component.setWidthFull();
    }

    @Override
    public Component getComponent() {
        return component;
    }

    @Override
    public void load(ITreeNode content) {
        component.setValue(content.getString(name, defaultValue));
    }

    @Override
    public void save(ITreeNode node) {
        node.put(name, component.getValue());
    }
}
