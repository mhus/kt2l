package de.mhus.kt2l.core;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.progressbar.ProgressBar;

public class ProgressDialog extends Dialog {

    private final ProgressBar progress;
    private final Div current;

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
    }

    public double getProgress() {
        return progress.getValue();
    }
}
