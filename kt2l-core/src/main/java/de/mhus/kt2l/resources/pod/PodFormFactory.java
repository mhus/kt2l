package de.mhus.kt2l.resources.pod;

import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.form.FormPanel;
import de.mhus.kt2l.form.FormPanelVerticalLayout;
import de.mhus.kt2l.form.YArray;
import de.mhus.kt2l.form.YText;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceFormFactory;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import org.springframework.stereotype.Component;

@Component
public class PodFormFactory implements ResourceFormFactory {
    @Override
    public boolean canHandleType(Cluster cluster, V1APIResource type) {
        return K8s.POD.equals(type);
    }

    @Override
    public boolean canHandleResource(Cluster cluster, V1APIResource type, KubernetesObject selected) {
        return canHandleType(cluster, type);
    }

    @Override
    public FormPanel createForm(ExecutionContext context) {
        return new FormPanelVerticalLayout() {
            @Override
            public void initUi() {
                add(new YText()
                        .path("metadata/name")
                        .label("Name")
                        .readOnly(true));
                add(new YText()
                        .path("metadata/namespace")
                        .label("Namespace")
                        .readOnly(true));
                add(new YArray().create(
                                p -> {
                                    p.add(new YText()
                                            .path("name")
                                            .label("Container Name")
                                    );
                                    p.add(new YText()
                                            .path("image")
                                            .label("Image")
                                    );
                                    p.add(new YArray().create(
                                                            p2 -> {
                                                                p2.add(new YText()
                                                                        .path("name")
                                                                        .label("Name")
                                                                );
                                                                p2.add(new YText()
                                                                        .path("value")
                                                                        .label("Value")
                                                                );
                                                            }
                                                    )
                                                    .path("env")
                                                    .label("Environment Variables")
                                    );
                                }
                        ).path("spec/containers")
                        .label("Containers"));

            }
        };
    }
}
