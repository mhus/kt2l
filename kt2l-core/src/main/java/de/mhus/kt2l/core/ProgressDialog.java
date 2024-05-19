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

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.progressbar.ProgressBar;

public class ProgressDialog extends Dialog {

    private final ProgressBar progress;
    private final Div current;
    private final Div details;

    public ProgressDialog() {
        progress = new ProgressBar();
        progress.setWidthFull();
        progress.setMin(0);
        progress.setMax(1);
        progress.setValue(0);
        add(progress);
        current = new Div("");
        current.setWidthFull();
        add(current);
        details = new Div("");
        details.setWidthFull();
        add(details);
        setWidth("500px");
        setHeight("200px");
        setCloseOnEsc(false);
    }

    public void setMin(double min) {
        progress.setMin(min);
    }

    public void setMax(double max) {
        progress.setMax(max);
    }

    public void setProgress(double value, String item) {
        progress.setValue(value);
        current.setText(item);
    }

    public void setIndeterminate(boolean indeterminate) {
        progress.setIndeterminate(indeterminate);
    }

    public void setProgressItem(String item) {
        current.setText(item);
        details.setText("");
    }

    public double getProgress() {
        return progress.getValue();
    }

    public void next(String item) {
        progress.setValue(progress.getValue() + 1);
        current.setText(item);
        details.setText("");
    }

    public void setProgressDetails(String details) {
        this.details.setText(details);
    }
}
