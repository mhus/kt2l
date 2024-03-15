package de.mhus.kt2l.generic;

import com.vaadin.flow.component.icon.VaadinIcon;
import io.kubernetes.client.common.KubernetesObject;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class ActionDetails implements ResourceAction {
    @Override
    public boolean canHandleResourceType(String resourceType) {
        return true;
    }

    @Override
    public boolean canHandleResource(String resourceType, Set<?> selected) {
        return selected.size() == 1;
    }

    @Override
    public void execute(ExecutionContext context) {

        var selected = context.getSelected().iterator().next();
        if (selected instanceof IResourceProvider) selected = ((IResourceProvider)selected).getResource();

        String namespace = "";
        String name = selected.toString();

        if (selected instanceof Map) {
            var metadata = (Map) ((Map) selected).get("metadata");
            namespace = (String) metadata.get("namespace");
            name = (String) metadata.get("name");
        } else
        if (selected instanceof KubernetesObject) {
            var metadata = ((KubernetesObject) selected).getMetadata();
            namespace = metadata.getNamespace();
            name = metadata.getName();
        }

        final var finalResource = (KubernetesObject)selected;

        context.getMainView().getTabBar().addTab(
                context.getClusterConfiguration().name() + ":" + context.getResourceType() + ":" + name + ":details",
                name,
                true,
                true,
                VaadinIcon.FILE_TEXT_O.create(),
                () ->
                        new ResourceDetailsPanel(
                                context.getClusterConfiguration(),
                                context.getApi(),
                                context.getMainView(),
                                context.getResourceType(),
                                finalResource
                        )).setColor(context.getClusterConfiguration().color()).select().setParentTab(context.getSelectedTab());

    }

    @Override
    public String getTitle() {
        return "Details";
    }

    @Override
    public String getMenuBarPath() {
        return null;
    }

    @Override
    public String getShortcutKey() {
        return "d";
    }

    @Override
    public String getPopupPath() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Resource Details";
    }
}
