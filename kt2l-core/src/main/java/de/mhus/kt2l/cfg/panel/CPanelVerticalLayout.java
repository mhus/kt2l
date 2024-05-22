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
