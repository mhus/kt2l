package de.mhus.kt2l.k8s;

import de.mhus.commons.M;
import de.mhus.commons.tools.MLang;
import de.mhus.commons.tools.MSystem;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.Getter;
import org.springframework.boot.json.GsonJsonParser;

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
