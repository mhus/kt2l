package de.mhus.kt2l;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.RouteParameters;

public record RouteEntry(String name, Class<? extends Component> clazz, RouteParameters parameters, Component icon) {
}
