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
package de.mhus.kt2l.core;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class Tail extends Scroller {

    private final VerticalLayout content;
    private LinkedList<TailRow> rows = new LinkedList<>();
    @Getter @Setter
    private int maxRows;
    @Getter @Setter
    private boolean autoScroll;

    public Tail() {
        content = new VerticalLayout();
        setContent(content);
        content.setMargin(false);
        content.setPadding(false);
        content.setSpacing(false);
//        content.setWidth("100%");
    }

    public void removeRow(TailRow row) {
        synchronized (rows) {
            rows.remove(row);
            content.remove(row.getElement());
        }
    }

    public void clear() {
        synchronized (rows) {
            rows.clear();
            content.removeAll();
        }
    }

    public List<TailRow> getRows() {
        synchronized (rows) {
            return List.copyOf(rows);
        }
    }

    public void addRows(Collection<TailRow> roww) {
        synchronized (rows) {
            rows.forEach(row -> {
                rows.remove(row);
                rows.add(row);

                if (row.getElement() != null) {
                    content.remove(row.getElement()); // for secure
                } else {
                    row.setElement(new Paragraph(row.getText()));
                }
                if (row.getColor() != null)
                    row.getElement().addClassName("color-" + row.getColor().name().toLowerCase());
                if (row.getBgcolor() != null)
                    row.getElement().addClassName("bgcolor-" + row.getBgcolor().name().toLowerCase());
                content.add(row.getElement());
            });
            while (maxRows > 0 && rows.size() > maxRows) {
                TailRow first = rows.removeFirst();
                content.remove(first.getElement());
            }
        }
        if (autoScroll) {
            scrollToEnd();
        }
    }

    public void addRow(TailRow row) {
        synchronized (rows) {
            rows.remove(row);
            rows.add(row);

            if (row.getElement() != null) {
                content.remove(row.getElement()); // for secure
            } else {
                row.setElement(new Paragraph(row.getText()));
            }
            if (row.getColor() != null)
                row.getElement().addClassName("color-" + row.getColor().name().toLowerCase());
            if (row.getBgcolor() != null)
                row.getElement().addClassName("bgcolor-" + row.getBgcolor().name().toLowerCase());
            content.add(row.getElement());
            while (maxRows > 0 && rows.size() > maxRows) {
                TailRow first = rows.removeFirst();
                content.remove(first.getElement());
            }
        }
        if (autoScroll) {
            scrollToEnd();
        }

    }

    public void scrollToEnd() {
        getElement().executeJs("this.scrollTop = this.scrollHeight;");
    }

}
