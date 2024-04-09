package de.mhus.kt2l.resources;

import io.kubernetes.client.common.KubernetesObject;

public interface ResourcesFilter {
    boolean filter(KubernetesObject res);

    String getDescription();
}
