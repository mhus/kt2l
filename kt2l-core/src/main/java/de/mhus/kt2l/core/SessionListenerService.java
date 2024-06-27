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

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static de.mhus.commons.tools.MLang.tryThis;

@Component
@Slf4j
public class SessionListenerService implements VaadinServiceInitListener {

    /*
    Need to set session for http session manually, because VaadinSession is not a HttpSession and the default
    timeout is 1800 sec (30 min). Need the orrect timeout to release resources.
    It's needed by Idle Notifiation Addon to keep the session alive.
    Set the server.session.timeout in application.properties to minimum 2 minutes, Idle Notifiation will not work with less.
     */
    @Value("${server.session.timeout:5}")
    private int sessionTimeout;

    @PostConstruct
    public void init() {
        LOGGER.debug("◇ SessionListenerService.init");
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        LOGGER.debug("◇ SessionListener.serviceInit {}", event);
        event.getSource().addSessionInitListener(
                    initEvent -> {
                        LOGGER.debug("◇ {} A new Session has been initialized! {}", tryThis(() -> initEvent.getSession().getSession().getId()).orElse("?"), initEvent);
                        initEvent.getSession().getSession().setMaxInactiveInterval(sessionTimeout * 60);
                    }
        );
        event.getSource().addSessionDestroyListener(
                destroyEvent -> LOGGER.debug("◇ {} A Session has been destroyed! {}", tryThis(() -> destroyEvent.getSession().getSession().getId()).orElse("?"), destroyEvent)
        );
        event.getSource().addUIInitListener(
                initEvent -> {
                    LOGGER.debug("◇ {} A new UI has been initialized! {}", tryThis(() -> initEvent.getUI().getSession().getSession().getId()).orElse("?"), initEvent);
                    initEvent.getUI().addAttachListener(
                            attachEvent -> LOGGER.debug("◇ {} A UI has been attached! {}", tryThis(() -> attachEvent.getSession().getSession().getId()).orElse("?"), attachEvent)
                    );
                    initEvent.getUI().addDetachListener(
                            detachEvent -> LOGGER.debug("◇ {} A UI has been detached! {}", tryThis(() -> detachEvent.getSession().getSession().getId()).orElse("?"), detachEvent)
                    );
                }
        );
        event.getSource().addServiceDestroyListener(
                destroyEvent -> LOGGER.debug("◇ A Service has been destroyed! {}", destroyEvent)
        );
    }
}
