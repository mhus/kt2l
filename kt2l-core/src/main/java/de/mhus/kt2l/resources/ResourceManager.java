/**
 * This file is part of kt2l-core.
 *
 * kt2l-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * kt2l-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with kt2l-core.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.kt2l.resources;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import io.kubernetes.client.common.KubernetesObject;

import java.util.ArrayList;
import java.util.List;

public class ResourceManager<T extends KubernetesObject> {
    private final List<T> resources;
    private volatile List<T> selectedResources;
    private final boolean canChange;
    private MenuItem menuBarItem;

    public ResourceManager(List<T> resources, boolean canChange) {
        this.resources = resources;
        this.selectedResources = resources;
        this.canChange = canChange;
    }

    public List<T> getResources() {
        return selectedResources;
    }

    public void injectMenu(MenuBar menuBar) {
        menuBarItem = menuBar.addItem(VaadinIcon.BULLSEYE.create(), e -> {
            showMenu(menuBarItem);
        });
    }

    private void showMenu(Component component) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Selected resources");

        Grid<T> grid = new Grid<>();
        grid.setSizeFull();
        if (canChange) {
            grid.setSelectionMode(Grid.SelectionMode.MULTI);
            ((GridMultiSelectionModel)grid.getSelectionModel()).setSelectAllCheckboxVisibility(GridMultiSelectionModel.SelectAllCheckboxVisibility.VISIBLE);
        }
        grid.addColumn(v -> getColumnValue(v)).setHeader("Name");

        grid.setItems(resources);
        selectedResources.forEach(grid::select);

        dialog.add(grid);
        dialog.setWidth("500px");
        dialog.setHeight("80%");

        Button cancelButton = new Button("Cancel", e -> {
            dialog.close();
            component.getUI().get().remove(dialog);
        });
        dialog.getFooter().add(cancelButton);
        if (canChange) {
            Button saveButton = new Button("Select", e -> {

                selectedResources = new ArrayList<>(grid.getSelectedItems());

                dialog.close();
                component.getUI().get().remove(dialog);
            });
            dialog.getFooter().add(saveButton);
        }

        component.getUI().get().add(dialog);
        dialog.open();
    }

    private String getColumnValue(T v) {
        return v.getMetadata().getName();
    }
}
