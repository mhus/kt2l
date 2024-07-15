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
package de.mhus.kt2l.form;

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
        component.setValue( getParent(content).getString(getNodeName(), defaultValue));
        component.setReadOnly(readOnly);
    }

    @Override
    public void save(ITreeNode node) {
        getParent(node).put(getNodeName(), component.getValue());
    }
}
