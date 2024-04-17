/**
 * This file is part of kt2l-core.
 *
 * kt2l-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * kt2l-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with kt2l-core.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.kt2l.k8s;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Map;

import static de.mhus.commons.tools.MLang.tryThis;

public class GenericObject implements KubernetesObject {
    @Getter
    private final Map<String, Object> data;
    private final V1ObjectMeta metatdata;
    private final String kind;
    private final String version;

    public GenericObject(Map<String, Object> data) {
        this.data = data;
        metatdata = new V1ObjectMeta();
        metatdata.setName((String) data.get("name"));
        metatdata.setNamespace((String) data.get("namespace"));
        metatdata.setLabels((Map<String, String>) data.get("labels"));
        metatdata.setAnnotations((Map<String, String>) data.get("annotations"));
        tryThis(() -> metatdata.setCreationTimestamp(OffsetDateTime.parse((String) data.get("creationTimestamp"))));
        tryThis(() -> metatdata.setDeletionTimestamp(OffsetDateTime.parse((String)data.get("deletionTimestamp"))));
//        metatdata.setFinalizers((String[]) data.get("finalizers"));
        kind = (String) data.get("kind");
        version = (String) data.get("apiVersion");
    }

    @Override
    public V1ObjectMeta getMetadata() {
        return metatdata;
    }

    @Override
    public String getApiVersion() {
        return version;
    }

    @Override
    public String getKind() {
        return kind;
    }
}
