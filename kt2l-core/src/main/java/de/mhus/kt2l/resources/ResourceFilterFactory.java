package de.mhus.kt2l.resources;

import io.kubernetes.client.common.KubernetesObject;

import java.util.function.Function;

public class ResourceFilterFactory {

    private final String title;
    private final Function<KubernetesObject, Boolean> filter;

    public ResourceFilterFactory(String title, Function<KubernetesObject, Boolean> filter) {
        this.title = title;
        this.filter = filter;
    }

    public String getTitle() {
        return title;
    }

    public ResourcesFilter create() {
        return new ResourcesFilter() {
            @Override
            public boolean filter(KubernetesObject res) {
                return filter.apply(res);
            }

            @Override
            public String getDescription() {
                return title;
            }
        };
    }
}
