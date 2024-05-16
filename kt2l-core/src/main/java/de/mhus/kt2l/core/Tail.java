package de.mhus.kt2l.core;

import com.vaadin.flow.component.html.Div;
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
            content.remove(row.getDiv());
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

                if (row.getDiv() != null) {
                    content.remove(row.getDiv()); // for secure
                } else {
                    row.setDiv(new Div(row.getText()));
                }
                if (row.getColor() != null)
                    row.getDiv().addClassName("color-" + row.getColor().name().toLowerCase());
                if (row.getBgcolor() != null)
                    row.getDiv().addClassName("bgcolor-" + row.getBgcolor().name().toLowerCase());
                content.add(row.getDiv());
            });
            while (maxRows > 0 && rows.size() > maxRows) {
                TailRow first = rows.removeFirst();
                content.remove(first.getDiv());
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

            if (row.getDiv() != null) {
                content.remove(row.getDiv()); // for secure
            } else {
                row.setDiv(new Div(row.getText()));
            }
            if (row.getColor() != null)
                row.getDiv().addClassName("color-" + row.getColor().name().toLowerCase());
            if (row.getBgcolor() != null)
                row.getDiv().addClassName("bgcolor-" + row.getBgcolor().name().toLowerCase());
            content.add(row.getDiv());
            while (maxRows > 0 && rows.size() > maxRows) {
                TailRow first = rows.removeFirst();
                content.remove(first.getDiv());
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
