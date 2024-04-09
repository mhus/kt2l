package de.mhus.kt2l.kscript;

import de.mhus.commons.lang.ICloseable;
import de.mhus.commons.tools.MLang;
import de.mhus.commons.tree.IProperties;
import de.mhus.commons.tree.MProperties;
import io.kubernetes.client.openapi.apis.CoreV1Api;
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
    CoreV1Api api;
    @Getter
    @Setter
    V1Pod pod;
    @Getter
    private List<Error> errors = new ArrayList<>();
    private StringBuffer content = new StringBuffer();

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
