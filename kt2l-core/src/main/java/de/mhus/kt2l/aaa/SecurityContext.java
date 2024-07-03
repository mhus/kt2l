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
package de.mhus.kt2l.aaa;

import com.vaadin.flow.component.UI;
import de.mhus.commons.errors.AuthorizationException;
import de.mhus.commons.lang.ICloseable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
public class SecurityContext {

    private static final ThreadLocal<SecurityContext> threadLocalConfigurationContext = new ThreadLocal<>();

    @Getter
    private final AaaUser user;

    public static SecurityContext create() {
        return new SecurityContext();
    }

    public SecurityContext(AaaUser user) {
        this.user = user;
    }

    protected SecurityContext() {
        user = lookupUser();
    }

    public String getUserId() {
        return user.getUserId();
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

    public static String lookupUserId() {
        var user = lookupUser();
        if (user == null) return null;
        return user.getUserId();
    }

    public static AaaUser lookupUser() {
        var context = threadLocalConfigurationContext.get();
        final var user = context != null ? context.getUser() : tryThis(() -> SecurityUtils.getUser()).getOrThrow(() -> {
            LOGGER.error("Calling config() without user in UI context", new Exception());
            return new AuthorizationException("No user in UI context");
        });
        return user;
    }

}
