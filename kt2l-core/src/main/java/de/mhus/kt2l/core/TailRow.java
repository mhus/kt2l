package de.mhus.kt2l.core;

import com.vaadin.flow.component.html.Div;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@AllArgsConstructor
@Getter
public class TailRow<U> {
    private final String text;
    private Div div;
    private UiUtil.COLOR color;
    private UiUtil.COLOR bgcolor;
    @Setter
    private U userObject;

    public TailRow(String text) {
        this.text = text;
    }

    void setDiv(Div div) {
        this.div = div;
    }
}
