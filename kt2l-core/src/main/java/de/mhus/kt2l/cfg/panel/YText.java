package de.mhus.kt2l.cfg.panel;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.textfield.TextField;
import de.mhus.commons.tree.ITreeNode;

public class YText extends YComponent<String> {

    private TextField text;

    @Override
    public void initUi() {
        text = new TextField();
        text.setWidthFull();
        text.setLabel(label);
    }

    @Override
    public Component getComponent() {
        return text;
    }

    @Override
    public void load(ITreeNode content) {
        text.setValue(content.getString(name, defaultValue));
    }

    @Override
    public void save(ITreeNode node) {
        node.put(name, text.getValue());
    }
}
