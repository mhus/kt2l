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

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.mhus.kt2l.cluster.ClusterConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

@AnonymousAllowed
@Route(value = "/reset")
@Slf4j
public class UiReset extends VerticalLayout {

    @Autowired
    private ClusterConfiguration clusterConfiguration;

    public UiReset() {
        LOGGER.info("UiReset");
        var backButton = new Button("KT2L", e -> getUI().ifPresent(ui -> ui.navigate("/")));
        backButton.setIcon(VaadinIcon.BACKSPACE_A.create());
        backButton.setAutofocus(true);
        backButton.setWidthFull();
        add(backButton);
    }

    @PostConstruct
    public void init() {
        clusterConfiguration.clearClusterCache(); // XXX is this session thread save ?
    }

}
