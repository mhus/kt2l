
* Logo zuschneiden
// * Fix commons: properties parser, add \n \r \t, auch multiple line inputs
// * exec: rename run in exec, add exec=
// * exec: multi line commands
* exec: single execution, ohne pod - pod wird dann erst erstellt
* details: auch multi create zulassen d.h. yaml editor mit apply ---
// * exec: command return value in context, e.g. timeout als status nicht als fehler in WAIT
// * exec: EXEC tty, stdin als parameter
* help: hilfe button und panel + configs im header
* used resources anzeige ueber button im header ?

* Menu in sub menus packen mit pfad und order

* Config provider für spring boot aus konfiguration 

* nodes grid
* deployment grid
* cronjob, job, statefulset grid
* action von pod zu nodes, deployments, cronjobs, jobs, statefulsets

* Docu: Lizenzen info fuer logo (https://www.svgrepo.com/svg/164744/wrench, https://www.svgrepo.com/svg/376331/kubernetes)
* Docu: author info ?
* Lizenz auf GPL?!
* Doku automatisch erstellen
* Doku auch in jar packen für offline hilfe oder nur online? - versioniert!

* Cockpit view mit Buttons für seiten,zb. Grafana und automat. portforwards machen (config im cluster)
* port forward background manager
* port forward view

* status view mit grafischem status aller nodes und pods, mem, cpu und storage

* shell: enhance terminal komponente, add key listener, kein echo

## release

* als standalone app mit embedded browser (https://github.com/chromiumembedded/java-cef)
  (https://github.com/jcefmaven/jcefmaven)
* pom submodule ??? für launcher ?

## exec

* LDIR command
* CP command
* output abschnitte in lokale dateien umleiten (wohin?)
// * IF ELSE ELIF FI commands
* LET command bzw SET als LET

## Handle
```
java.io.IOException: Cannot run program "gke-gcloud-auth-plugin": error=2, No such file or directory
at java.base/java.lang.ProcessBuilder.start(Unknown Source)
at java.base/java.lang.ProcessBuilder.start(Unknown Source)
at io.kubernetes.client.util.KubeConfig.runExec(KubeConfig.java:345)
at io.kubernetes.client.util.KubeConfig.credentialsViaExecCredential(KubeConfig.java:281)
at io.kubernetes.client.util.KubeConfig.getCredentials(KubeConfig.java:237)
at io.kubernetes.client.util.credentials.KubeconfigAuthentication.<init>(KubeconfigAuthentication.java:59)
at io.kubernetes.client.util.ClientBuilder.kubeconfig(ClientBuilder.java:299)
at io.kubernetes.client.util.Config.fromConfig(Config.java:98)
at de.mhus.kt2l.k8s.K8sService.getKubeClient(K8sService.java:98)
at de.mhus.kt2l.k8s.K8sService.getCoreV1Api(K8sService.java:102)
at de.mhus.kt2l.resources.ResourcesGridPanel.lambda$tabInit$7518f186$1(ResourcesGridPanel.java:198)
at io.vavr.control.Try.of(Try.java:83)
at de.mhus.kt2l.resources.ResourcesGridPanel.tabInit(ResourcesGridPanel.java:198)
at de.mhus.kt2l.ui.XTabBar.lambda$addTab$0(XTabBar.java:45)
at io.vavr.control.Try.run(Try.java:154)

io.kubernetes.client.openapi.ApiException: java.net.ConnectException: Failed to connect to /127.0.0.1:6443
at io.kubernetes.client.openapi.ApiClient.execute(ApiClient.java:888)
at io.kubernetes.client.openapi.apis.CoreV1Api.listPodForAllNamespacesWithHttpInfo(CoreV1Api.java:37296)
at io.kubernetes.client.openapi.apis.CoreV1Api.listPodForAllNamespaces(CoreV1Api.java:37189)
at de.mhus.kt2l.pods.PodGrid$PodProvider.lambda$new$56cc5271$1(PodGrid.java:294)
at io.vavr.control.Try.of(Try.java:83)
at de.mhus.kt2l.pods.PodGrid$PodProvider.lambda$new$512e4660$1(PodGrid.java:294)
at com.vaadin.flow.data.provider.CallbackDataProvider.sizeInBackEnd(CallbackDataProvider.java:142)
at com.vaadin.flow.data.provider.AbstractBackEndDataProvider.size(AbstractBackEndDataProvider.java:66)
at com.vaadin.flow.data.provider.DataCommunicator.getDataProviderSize(DataCommunicator.java:940)
at com.vaadin.flow.data.provider.DataCommunicator.flush(DataCommunicator.java:1193)
at com.vaadin.flow.data.provider.DataCommunicator.lambda$requestFlush$7258256f$1(DataCommunicator.java:1138)
at com.vaadin.flow.internal.StateTree.lambda$runExecutionsBeforeClientResponse$2(StateTree.java:397)
at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.accept(Unknown Source)
at java.base/java.util.stream.ReferencePipeline$2$1.accept(Unknown Source)
at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(Unknown Source)
```