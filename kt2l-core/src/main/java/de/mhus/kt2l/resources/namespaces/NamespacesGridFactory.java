package de.mhus.kt2l.resources.namespaces;

import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.core.WithRole;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.ResourceGridFactory;
import de.mhus.kt2l.resources.ResourcesGrid;
import org.springframework.stereotype.Component;

@Component
@WithRole(UsersConfiguration.ROLE.READ)
public class NamespacesGridFactory implements ResourceGridFactory {
    @Override
    public boolean canHandleResourceType(String resourceType) {
        return K8sUtil.RESOURCE_NAMESPACE.equals(resourceType);
    }

    @Override
    public ResourcesGrid create(String resourcesType) {
        return new NamespacesGrid();
    }
}
