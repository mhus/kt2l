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

package de.mhus.kt2l.kscript;

import de.mhus.commons.lang.ICloseable;
import de.mhus.commons.tools.MLang;
import de.mhus.commons.tree.IProperties;
import de.mhus.commons.tree.MProperties;
import de.mhus.kt2l.core.SecurityContext;
import de.mhus.kt2l.k8s.ApiProvider;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class RunContext implements ICloseable {
    @Getter
    private IProperties properties = new MProperties();
    @Getter
    @Setter
    Consumer<String> textChangedObserver;

    @Getter
    @Setter
    ApiProvider apiProvider;
    @Getter
    @Setter
    V1Pod pod;
    @Getter
    private List<Error> errors = new ArrayList<>();
    private StringBuffer content = new StringBuffer();
    @Getter
    private SecurityContext securityContext = SecurityContext.create();

    public void setScope(String scope, Scope object) {
        var current = properties.get(RunCompiler.PROP_SCOPE + scope);
        if (current != null) {
            MLang.tryThis(() -> ((Closeable) current).close()).onError(e -> LOGGER.error("Close Scope", e));
        }
        properties.put(RunCompiler.PROP_SCOPE + scope, object);
    }

    public Scope getScope(String scope) {
        return (Scope)properties.get(RunCompiler.PROP_SCOPE + scope);
    }

    public void addError(Cmd cmd, Exception e) {
        errors.add(new Error(cmd, e));
    }

    @Override
    public void close() {
        for (var entry : properties.entrySet()) {
            if (entry.getKey().startsWith(RunCompiler.PROP_SCOPE)) {
                MLang.tryThis(() -> ((Closeable) entry.getValue()).close()).onError(e -> LOGGER.error("Close Scope", e));
            }
        }
    }

    public void addContent(String line) {
        content.append(line);
        if (content.length() > RunCompiler.DEFAULT_MAX_CONTENT_SIZE)
            content.delete(0, content.length() - RunCompiler.DEFAULT_MAX_CONTENT_SIZE);
        if (textChangedObserver != null)
            textChangedObserver.accept(content.toString());
    }

}
