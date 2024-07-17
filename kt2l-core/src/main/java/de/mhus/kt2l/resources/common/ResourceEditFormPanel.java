package de.mhus.kt2l.resources.common;

import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.mhus.commons.tree.MTree;
import de.mhus.commons.tree.YamlTreeNodeBuilder;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.form.FormPanel;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.ui.UiUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

@Slf4j
@Configurable
public class ResourceEditFormPanel extends VerticalLayout implements DeskTabListener {

    private final Core core;
    private final Cluster cluster;
    private final KubernetesObject resource;
    private final FormPanel form;
    private final V1APIResource type;
    private MenuItem btnSave;

    @Autowired
    private K8sService k8sService;

    public ResourceEditFormPanel(Core core, Cluster cluster, KubernetesObject resource, FormPanel form, V1APIResource type) {
        this.core = core;
        this.cluster = cluster;
        this.resource = resource;
        this.form = form;
        this.type = type;
    }

    @Override
    public void tabInit(DeskTab deskTab) {
        core.getBeanFactory().autowireBean(form);
        form.initUi();

        var scroller = new Scroller();
        scroller.setContent(form.getPanel());
        scroller.setSizeFull();
        add(scroller);

        MenuBar menuBar = new MenuBar();
        btnSave = menuBar.addItem("Save", e -> doSave());
        add(menuBar);

        try {
            var yamlString = Yaml.dump(resource);
            var tree = new YamlTreeNodeBuilder().readFromString(yamlString);
            form.load(tree);
        } catch (Exception e) {
            LOGGER.error("Can't load resource", e);
            btnSave.setEnabled(false);
        }
        setSizeFull();
        setMargin(false);
        setPadding(false);
    }

    private void doSave() {
        try {
            var handler = k8sService.getTypeHandler(type);
            var name = resource.getMetadata().getName();
            var namespace = resource.getMetadata().getNamespace();
            var yamlString = Yaml.dump(resource);
            var tree = new YamlTreeNodeBuilder().readFromString(yamlString);
            form.save(tree);

            tree.remove("status");
            tree.getObject("metadata").orElse(MTree.EMPTY_MAP).remove("managedFields");

            var yaml = new YamlTreeNodeBuilder().writeToString(tree);
            handler.replace(cluster.getApiProvider(), name, namespace, yaml);
            UiUtil.showSuccessNotification("Resource saved");
        } catch (Exception e) {
            LOGGER.warn("Can't save resource", e);
            UiUtil.showErrorNotification("Can't save resource", e);
        }
    }

    @Override
    public void tabSelected() {

    }

    @Override
    public void tabUnselected() {

    }

    @Override
    public void tabDestroyed() {

    }

    @Override
    public void tabRefresh(long counter) {

    }

}
