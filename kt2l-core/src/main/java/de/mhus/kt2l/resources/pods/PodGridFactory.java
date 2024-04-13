package de.mhus.kt2l.resources.pods;

import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.ResourceGridFactory;
import de.mhus.kt2l.resources.ResourcesGrid;
import de.mhus.kt2l.core.WithRole;
import org.springframework.stereotype.Component;

@Component
@WithRole(UsersConfiguration.ROLE.READ)
public class PodGridFactory implements ResourceGridFactory {
    @Override
    public boolean canHandleResourceType(String resourceType) {
        return K8sUtil.RESOURCE_PODS.equals(resourceType);
    }

    @Override
    public ResourcesGrid create(String resourcesType) {
        return new PodGrid();
    }
}
