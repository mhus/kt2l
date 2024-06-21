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
//    @Value("${server.session.timeout}")
//    private int sessionTimeout;

    @PostConstruct
    public void init() {
        LOGGER.debug("◇ SessionListenerService.init");
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        LOGGER.debug("◇ SessionListener.serviceInit {}", event);
        event.getSource().addSessionInitListener(
                    initEvent -> {
                        LOGGER.debug("◇ {} A new Session has been initialized! {}", tryThis(() -> initEvent.getSession().getSession().getId()).or("?"), initEvent);
                        // initEvent.getSession().getSession().setMaxInactiveInterval(sessionTimeout * 60);
                    }
        );
        event.getSource().addSessionDestroyListener(
                destroyEvent -> LOGGER.debug("◇ {} A Session has been destroyed! {}", tryThis(() -> destroyEvent.getSession().getSession().getId()).or("?"), destroyEvent)
        );
        event.getSource().addUIInitListener(
                initEvent -> {
                    LOGGER.debug("◇ {} A new UI has been initialized! {}", tryThis(() -> initEvent.getUI().getSession().getSession().getId()).or("?"), initEvent);
                    initEvent.getUI().addAttachListener(
                            attachEvent -> LOGGER.debug("◇ {} A UI has been attached! {}", tryThis(() -> attachEvent.getSession().getSession().getId()).or("?"), attachEvent)
                    );
                    initEvent.getUI().addDetachListener(
                            detachEvent -> LOGGER.debug("◇ {} A UI has been detached! {}", tryThis(() -> detachEvent.getSession().getSession().getId()).or("?"), detachEvent)
                    );
                }
        );
        event.getSource().addServiceDestroyListener(
                destroyEvent -> LOGGER.debug("◇ A Service has been destroyed! {}", destroyEvent)
        );
    }
}
