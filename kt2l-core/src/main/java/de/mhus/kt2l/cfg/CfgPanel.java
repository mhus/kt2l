package de.mhus.kt2l.cfg;

import com.vaadin.flow.component.Component;
import de.mhus.commons.tree.ITreeNode;

public interface CfgPanel {

    Component getPanel();

    void load(ITreeNode content);

    void save(ITreeNode content);

    boolean isValid();

    String getTitle();

    void initUi();

}
