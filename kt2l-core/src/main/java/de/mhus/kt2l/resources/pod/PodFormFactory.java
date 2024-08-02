/*
 * kt2l-core - kt2l core implementation
 * Copyright © 2024 Mike Hummel (mh@mhus.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
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
