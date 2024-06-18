package de.mhus.kt2l.generators;

import de.mhus.commons.tools.MFile;
import de.mhus.commons.tools.MString;
import de.mhus.kt2l.k8s.K8s;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class GenerateK8sHandlers {

    private final File targetDir;
    private final List<K8s> resources;
    private final boolean testGenerator = true;

    public static void main(String[] args) {
        new GenerateK8sHandlers().doGenerate();
    }

    public GenerateK8sHandlers() {
        File root = null;
        if (new File("kt2l/kt2l-core").exists())
            root = new File("kt2l/kt2l-core");
        else if (new File("kt2l-core").exists())
            root = new File("kt2l-core");
        else
            throw new RuntimeException("Root not found");
        targetDir = new File(root, "src/main/java/de/mhus/kt2l/generated");

        resources = Arrays.stream(K8s.values()).filter(r ->
                        (!testGenerator || r.resourceType().equals("pods") || r.resourceType().equals("nodes"))
                        &&
                        !r.resourceType().equals("CUSTOM")
                        &&
                        !r.resourceType().equals("GENERIC")
                        &&
                        !r.resourceType().equals("customresourcedefinitions")
                        &&
                        !r.resourceType().equals("containers")
                    ).toList();
    }

    private void doGenerate() {
        resources.forEach(this::generateHandler);
    }

    private void generateHandler(K8s k8s) {
        LOGGER.info("Generate {}", k8s);
        StringBuffer o = new StringBuffer();
        classHeader(o, k8s);
        managedResourceType(o, k8s);
        replaceResourceType(o, k8s);
        deleteResourceType(o, k8s);
        createResourceType(o, k8s);
        createResourceListWithoutNamespace(o, k8s);
        createResourceListWithNamespace(o, k8s);
        createResourceWatchCall(o, k8s);
        patchResourceType(o, k8s);

        classFooter(o, k8s);
        MFile.writeFile(getFile(k8s), o.toString());
    }

    /*
        @Override
    public Object patch(ApiProvider apiProvider, String namespace, String name, String patchString) throws ApiException {
        V1Patch patch = new V1Patch(patchString);
        return PatchUtils.patch(
                V1Pod.class,
                () -> apiProvider.getCoreV1Api().patchNamespacedPodCall(
                        name,
                        namespace,
                        patch,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                V1Patch.PATCH_FORMAT_JSON_PATCH,
                apiProvider.getClient()
        );
    }
    @Override
    public Object patch(ApiProvider apiProvider, String namespace, String name, String patchString) throws ApiException {
        V1Patch patch = new V1Patch(patchString);
        return PatchUtils.patch(
                V1Node.class,
                () -> apiProvider.getCoreV1Api().patchNodeCall(
                        name,
                        patch,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                V1Patch.PATCH_FORMAT_JSON_PATCH,
                apiProvider.getClient()
        );
    }
     */
    private void patchResourceType(StringBuffer o, K8s k8s) {
        o.append("    @Override\n");
        o.append("    public Object patch(ApiProvider apiProvider, String namespace, String name, String patchString) throws ApiException {\n");
        o.append("        var patch = new V1Patch(patchString);\n");
        o.append("        return apiProvider.").append(apiFunction(k8s)).append(".patch").append(methodName(k8s)).append("Call(\n");
        o.append("            name,\n");
        if (k8s.isNamespaced())
            o.append("            namespace,\n");
        o.append("            patch,\n");
        o.append("            null, null, null, null, null, null\n");
        o.append("        );\n");
        o.append("    }\n");
        o.append("\n");
    }

/*
    @Override
    public Call createResourceWatchCall(ApiProvider apiProvider) throws ApiException {
        try {
            return apiProvider.getCoreV1Api().listPodForAllNamespacesCall(null, null, null, null, null, null, null, null, null, null, true, new CallBackAdapter<V1Pod>(LOGGER));
        } catch (ApiException apiException) {
            LOGGER.warn("ApiException RC {}, Body {}", apiException.getCode(), apiException.getResponseBody());
            apiProvider.invalidate();
            return apiProvider.getCoreV1Api().listPodForAllNamespacesCall(null, null, null, null, null, null, null, null, null, null, true, new CallBackAdapter<V1Pod>(LOGGER));
        }
    }
    @Override
    public Call createResourceWatchCall(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getCoreV1Api().listNodeCall(null, null, null, null, null, null, null, null, null, null, true, new CallBackAdapter(LOGGER));
    }
 */
    private void createResourceWatchCall(StringBuffer o, K8s k8s) {
        o.append("    @Override\n");
        o.append("    public Call createResourceWatchCall(ApiProvider apiProvider) throws ApiException {\n");
        if (k8s.isNamespaced())
            o.append("        return apiProvider.").append(apiFunction(k8s)).append(".list").append(k8s.kind()).append("ForAllNamespacesCall(\n");
        else
            o.append("        return apiProvider.").append(apiFunction(k8s)).append(".list").append(k8s.kind()).append("Call(\n");
        o.append("            null, null, null, null, null, null, null, null, null, null, true,\n");
        o.append("            new CallBackAdapter<").append(resourceClassName(k8s)).append(">(LOGGER)\n");
        o.append("        );\n");
        o.append("    }\n");
        o.append("\n");
    }

/*
    @Override
    public V1PodList createResourceListWithNamespace(ApiProvider apiProvider, String namespace) throws ApiException {
        try {
            return apiProvider.getCoreV1Api().listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null, null);
        } catch (ApiException apiException) {
            LOGGER.warn("ApiException RC {}, Body {}", apiException.getCode(), apiException.getResponseBody());
            apiProvider.invalidate();
            return apiProvider.getCoreV1Api().listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null, null);
        }
    }
    @Override
    public <L extends KubernetesListObject> L createResourceListWithNamespace(ApiProvider apiProvider, String namespace) throws ApiException {
        throw new NotImplementedException();
    }
 */
    private void createResourceListWithNamespace(StringBuffer o, K8s k8s) {
        o.append("    @Override\n");
        o.append("    public ").append(resourceListClassName(k8s)).append(" createResourceListWithNamespace(ApiProvider apiProvider, String namespace) throws ApiException {\n");

        if (!k8s.isNamespaced()) {
            o.append("      throw new NotImplementedException();\n");
        } else {
            o.append("        return apiProvider.").append(apiFunction(k8s)).append(".list").append(methodName(k8s)).append("(\n");
            o.append("            namespace,\n");
            o.append("            null, null, null, null, null, null, null, null, null, null, null\n");
            o.append("        );\n");
        }
        o.append("    }\n");
        o.append("\n");
    }

/*
    @Override
    public V1PodList createResourceListWithoutNamespace(ApiProvider apiProvider) throws ApiException {
        try {
            return apiProvider.getCoreV1Api().listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null, null);
        } catch (ApiException apiException) {
            LOGGER.warn("ApiException RC {}, Body {}", apiException.getCode(), apiException.getResponseBody());
            apiProvider.invalidate();
            return apiProvider.getCoreV1Api().listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null, null);
        }
    }
    @Override
    public V1NodeList createResourceListWithoutNamespace(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getCoreV1Api().listNode(null, null, null, null, null, null, null, null, null, null, null);
    }
 */
    private void createResourceListWithoutNamespace(StringBuffer o, K8s k8s) {
        o.append("    @Override\n");
        o.append("    public ").append(resourceListClassName(k8s)).append(" createResourceListWithoutNamespace(ApiProvider apiProvider) throws ApiException {\n");
        o.append("        return apiProvider.").append(apiFunction(k8s)).append(".list").append(k8s.kind());
        if (k8s.isNamespaced())
            o.append("ForAllNamespaces");
        o.append("(\n");
        o.append("            null, null, null, null, null, null, null, null, null, null, null\n");
        o.append("        );\n");
        o.append("    }\n");
        o.append("\n");
    }

    /*
        @Override
    public KubernetesObject create(ApiProvider apiProvider, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1Pod.class);
        if (body.getSpec().getOverhead() != null && body.getSpec().getOverhead().size() == 0) {
            body.getSpec().setOverhead(null);
        }
        return apiProvider.getCoreV1Api().createNamespacedPod(body.getMetadata().getNamespace(), body, null, null, null, null);
    }
    @Override
    public Object create(ApiProvider apiProvider, String yaml) throws ApiException {
        // this is dangerous ... deny! - or stupid?
        K8sUtil.checkDeleteAccess(securityService, K8s.NODE);
        var body = Yaml.loadAs(yaml, V1Node.class);
        return apiProvider.getCoreV1Api().createNode(body,null, null, null, null);
    }
     */
    private void createResourceType(StringBuffer o, K8s k8s) {
        o.append("    @Override\n");
        o.append("    public Object create(ApiProvider apiProvider, String yaml) throws ApiException {\n");
        o.append("        var body = Yaml.loadAs(yaml, ").append(resourceClassName(k8s)).append(".class);\n");
        o.append("        return apiProvider.").append(apiFunction(k8s)).append(".create").append(methodName(k8s)).append("(\n");
        if (k8s.isNamespaced())
            o.append("            body.getMetadata().getNamespace(),\n");
        o.append("            body,\n");
        o.append("            null, null, null, null\n");
        o.append("        );\n");
        o.append("    }\n");
        o.append("\n");
    }

    /*
    @Override
    public KubernetesObject delete(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        K8sUtil.checkDeleteAccess(securityService, K8s.POD);
        return apiProvider.getCoreV1Api().deleteNamespacedPod(name, namespace, null, null, null, null, null, null);
    }
    @Override
    public V1Status delete(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        // this is dangerous ... deny!
        K8sUtil.checkDeleteAccess(securityService, K8s.NODE);
        return apiProvider.getCoreV1Api().deleteNode(name, null, null, null, null, null, null );
    }
     */
    private void deleteResourceType(StringBuffer o, K8s k8s) {
        o.append("    @Override\n");
        o.append("    public Object delete(ApiProvider apiProvider, String name, String namespace) throws ApiException {\n");
        o.append("        K8sUtil.checkDeleteAccess(securityService, K8s.").append(staticName(k8s)).append(");\n");
        o.append("        return apiProvider.").append(apiFunction(k8s)).append(".delete").append(methodName(k8s)).append("(\n");
        o.append("            name,\n");
        if (k8s.isNamespaced())
            o.append("            namespace,\n");
        o.append("            null, null, null, null, null, null\n");
        o.append("        );\n");
        o.append("    }\n");
        o.append("\n");
    }

    /*
    @Override
    public void replace(ApiProvider apiProvider, String name, String namespace, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1Pod.class);
        apiProvider.getCoreV1Api().replaceNamespacedPod(
                name,
                namespace,
                body,
                null, null, null, null
        );
    }
    @Override
    public void replace(ApiProvider apiProvider, String name, String namespace, String yaml) throws ApiException {
        // this is dangerous ... deny like delete!
        K8sUtil.checkDeleteAccess(securityService, K8s.NODE);
        var body = Yaml.loadAs(yaml, V1Node.class);
        apiProvider.getCoreV1Api().replaceNode(
                name,
                body,
                null, null, null, null
        );
    }
     */
    private void replaceResourceType(StringBuffer o, K8s k8s) {
        o.append("    @Override\n");
        o.append("    public void replace(ApiProvider apiProvider, String name, String namespace, String yaml) throws ApiException {\n");
        o.append("        var body = Yaml.loadAs(yaml, ").append(resourceClassName(k8s)).append(".class);\n");
        o.append("        apiProvider.").append(apiFunction(k8s)).append(".replace").append(methodName(k8s)).append("(\n");
        o.append("            name,\n");
        if (k8s.isNamespaced())
            o.append("            namespace,\n");
        o.append("            body,\n");
        o.append("            null, null, null, null\n");
        o.append("        );\n");
        o.append("    }\n");
        o.append("\n");
    }

    private String methodName(K8s k8s) {
        if (k8s.isNamespaced())
            return "Namespaced" + k8s.kind();
        else
            return k8s.kind();
    }

    private String apiFunction(K8s k8s) {
        if (k8s.group() == null) return "getCoreV1Api()";
        switch (k8s.group()) {
            case "apps":
                return "getAppsV1Api()";
            case "batch":
                return "getBatchV1Api()";
            case "extensions":
                return "getExtensionsV1beta1Api()";
            case "networking.k8s.io":
                return "getNetworkingV1Api()";
            case "rbac.authorization.k8s.io":
                return "getRbacAuthorizationV1Api()";
            case "storage.k8s.io":
                return "getStorageV1Api()";
            case "autoscaling":
                return "getAutoscalingV1Api()";
            case "apiextensions.k8s.io":
                return "getApiextensionsV1Api()";
            default:
                throw new RuntimeException("Unknown group " + k8s.group());
        }
    }

    private void managedResourceType(StringBuffer o, K8s k8s) {
        o.append("    @Override\n");
        o.append("    public K8s getManagedResourceType() {\n");
        o.append("        return K8s.").append(staticName(k8s)).append(";\n");
        o.append("    }\n");
        o.append("\n");
    }

    private String staticName(K8s k8s) {
        var res =  MString.camelToSnake( k8s.kind() ).toUpperCase();
        if (res.equals("HORIZONTAL_POD_AUTOSCALER")) return "HPA";
        return res;
    }

    private void classFooter(StringBuffer o, K8s k8s) {
        o.append("}\n");
    }

    private void classHeader(StringBuffer o, K8s k8s) {
        o.append("package de.mhus.kt2l.generated;\n");
        o.append("\n");
        o.append("""
import de.mhus.kt2l.core.SecurityService;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.CallBackAdapter;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.PodLogs;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.PatchUtils;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.commons.lang3.NotImplementedException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
""");

        o.append("\n");
        o.append("import ").append(k8s.clazz().getName()).append(";\n");
        o.append("import ").append(k8s.clazz().getName()).append("List;\n");
        o.append("\n");
        o.append("@Slf4j\n");
        o.append("public abstract class ").append(generatedClassName(k8s)).append(" implements HandlerK8s {\n");
        o.append("\n");
        o.append("    @Autowired\n");
        o.append("    private SecurityService securityService;\n");
        o.append("\n");


    }

    private File getFile(K8s k8s) {
        return new File(targetDir, generatedClassName(k8s) + ".java");
    }

    private String resourceClassName(K8s k8s) {
        return k8s.clazz().getSimpleName();
    }

    private String resourceListClassName(K8s k8s) {
        return k8s.clazz().getSimpleName() + "List";
    }

    private String generatedClassName(K8s k8s) {
        return "K8s" + k8s.clazz().getSimpleName();
    }
}
