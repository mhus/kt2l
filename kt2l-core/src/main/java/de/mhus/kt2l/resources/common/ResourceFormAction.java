package de.mhus.kt2l.resources.common;

import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;

import java.util.Set;

public class ResourceFormAction implements ResourceAction {

    @Override
    public boolean canHandleType(Cluster cluster, V1APIResource type) {
        return true;
    }

    @Override
    public boolean canHandleResource(Cluster cluster, V1APIResource type, Set<? extends KubernetesObject> selected) {
        return false;
    }

    @Override
    public void execute(ExecutionContext context) {

    }

    @Override
    public String getTitle() {
        return "";
    }

    @Override
    public String getMenuPath() {
        return "";
    }

    @Override
    public int getMenuOrder() {
        return 0;
    }

    @Override
    public String getShortcutKey() {
        return "";
    }

    @Override
    public String getDescription() {
        return "";
    }
}
