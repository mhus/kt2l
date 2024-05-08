package de.mhus.kt2l.core;

import com.vaadin.flow.component.UI;
import de.mhus.commons.errors.AuthorizationException;
import de.mhus.commons.lang.ICloseable;
import de.mhus.kt2l.Kt2lApplication;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.Principal;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
public class SecurityContext {

    private static final ThreadLocal<SecurityContext> threadLocalConfigurationContext = new ThreadLocal<>();

    @Getter
    private final String userName;
    @Getter
    private final Principal principal;

    public static SecurityContext create() {
        return new SecurityContext();
    }

    protected SecurityContext() {
        userName = lookupUserName();
        principal = lookupPrincipal();
    }

//    public static boolean has() {
//        return threadLocalConfigurationContext.get() != null;
//    }
//
//    public static SecurityContext get() {
//        return threadLocalConfigurationContext.get();
//    }

    public Environment enter() {
        return new Environment(this);
    }

    public static class Environment implements ICloseable {
        private final SecurityContext context;
        private final SecurityContext lastContext;

        private Environment(SecurityContext context) {
            this.context = context;
            this.lastContext = threadLocalConfigurationContext.get();
            threadLocalConfigurationContext.set(context);
        }

        @Override
        public void close() {
            threadLocalConfigurationContext.remove();
            if (lastContext != null)
                threadLocalConfigurationContext.set(lastContext);
        }
    }

    public static String lookupUserName() {
        var context = threadLocalConfigurationContext.get();
        final var userName = context != null ? context.getUserName() : tryThis(() -> (String) UI.getCurrent().getSession().getAttribute(Kt2lApplication.UI_USERNAME)).getOrThrow(() -> {
            LOGGER.error("Calling config() without user in UI context", new Exception());
            return new AuthorizationException("No user in UI context");
        });
        return userName;
    }

    public static Principal lookupPrincipal() {
        var context = threadLocalConfigurationContext.get();
        final var principal = context != null ? context.getPrincipal() : tryThis(() -> (Principal) SecurityUtils.getPrincipal()).getOrThrow(() -> {
            LOGGER.error("Calling config() without user in UI context", new Exception());
            return new AuthorizationException("No user in UI context");
        });
        return principal;
    }

}
