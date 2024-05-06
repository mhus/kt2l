package de.mhus.kt2l.cluster;

import de.mhus.commons.tree.ITreeNode;
import de.mhus.kt2l.core.UiUtil;
import de.mhus.kt2l.k8s.K8s;
import io.kubernetes.client.openapi.models.V1APIResource;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
public class Cluster {
    private final String name;
    private final String title;
    private final boolean enabled;
    private final String defaultNamespace;
    private final K8s.RESOURCE defaultResourceType;
    private final UiUtil.COLOR color;
    private final ITreeNode node;
    @Setter
    private List<String> currentNamespaces;
    @Setter
    private List<V1APIResource> resourceTypes;

    Cluster(String name, String title, boolean enabled, String defaultNamespace, K8s.RESOURCE defaultResourceType, UiUtil.COLOR color,
            ITreeNode node) {
        this.name = name;
        this.title = title;
        this.enabled = enabled;
        this.defaultNamespace = defaultNamespace;
        this.defaultResourceType = defaultResourceType;
        this.color = color;
        this.node = node;
    }

}
