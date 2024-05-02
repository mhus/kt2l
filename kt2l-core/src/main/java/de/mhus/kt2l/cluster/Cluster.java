package de.mhus.kt2l.cluster;

import de.mhus.commons.tree.ITreeNode;
import de.mhus.kt2l.core.UiUtil;
import de.mhus.kt2l.k8s.K8s;

public record Cluster(String name, String title, boolean enabled, String defaultNamespace, K8s.RESOURCE defaultResourceType, UiUtil.COLOR color,
                      ITreeNode node) {
}
