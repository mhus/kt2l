package de.mhus.kt2l.cluster;

import de.mhus.commons.tree.ITreeNode;
import de.mhus.kt2l.core.UiUtil;

public record Cluster(String name, String title, boolean enabled, String defaultNamespace, String defaultResourceType, UiUtil.COLOR color,
                      ITreeNode node) {
}
