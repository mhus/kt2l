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
package de.mhus.kt2l;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class AiTest {

    static String MODEL_NAME = "mistral"; // try "mistral", "llama2", "codellama", "phi" or "tinyllama"

//    @Container
//    static GenericContainer<?> ollama = new GenericContainer<>("langchain4j/ollama-" + MODEL_NAME + ":latest")
//            .withExposedPorts(11434);

    @Test
    @Disabled
    public void testOllama() throws IOException {

//        ollama.start();

        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(baseUrl())
                .modelName(MODEL_NAME)
//                .format("json")
                .build();

//        String json = model.generate("Give me a JSON with 2 fields: name and age of a John Doe, 42");

        String msg = """
Do you see problems in the following kubernetes resource?
...
... apiVersion: v1
... kind: Pod
... metadata:
...   creationTimestamp: '2024-03-14T17:27:41Z'
...   generateName: logger-7f6f9b9556-
...   labels:
...     app: logger
...     pod-template-hash: 7f6f9b9556
...   name: logger-7f6f9b9556-q8dr6
...   namespace: default
...   ownerReferences:
...   - apiVersion: apps/v1
...     kind: ReplicaSet
...     blockOwnerDeletion: true
...     controller: true
...     name: logger-7f6f9b9556
...     uid: f9fef09f-44ac-42b8-8bad-34b18a6826d7
...   resourceVersion: '12919'
...   uid: 521644a1-bba8-482d-9951-0b2c7f16166c
... spec:
...   containers:
...   - image: chentex/random-logger:latest
...     imagePullPolicy: Always
...     name: random-logger
...     resources: {
...       }
...     terminationMessagePath: /dev/termination-log
...     terminationMessagePolicy: File
...     volumeMounts:
...     - mountPath: /var/run/secrets/kubernetes.io/serviceaccount
...       name: kube-api-access-hjf2n
...       readOnly: true
...   dnsPolicy: ClusterFirst
...   enableServiceLinks: true
...   nodeName: colima
...   preemptionPolicy: PreemptLowerPriority
...   priority: 0
...   restartPolicy: Always
...   schedulerName: default-scheduler
...   securityContext: {
...     }
...   serviceAccount: default
...   serviceAccountName: default
...   terminationGracePeriodSeconds: 30
...   tolerations:
...   - effect: NoExecute
...     key: node.kubernetes.io/not-ready
...     operator: Exists
...     tolerationSeconds: 300
...   - effect: NoExecute
...     key: node.kubernetes.io/unreachable
...     operator: Exists
...     tolerationSeconds: 300
...   volumes:
...   - name: kube-api-access-hjf2n
...     projected:
...       defaultMode: 420
...       sources:
...       - serviceAccountToken:
...           expirationSeconds: 3607
...           path: token
...       - configMap:
...           items:
...           - key: ca.crt
...             path: ca.crt
...           name: kube-root-ca.crt
...       - downwardAPI:
...           items:
...           - fieldRef:
...               apiVersion: v1
...               fieldPath: metadata.namespace
...             path: namespace
""";
        String answer = model.generate(msg);

        System.out.println(answer);


    }

    static String baseUrl() {
//        return String.format("http://%s:%d", ollama.getHost(), ollama.getFirstMappedPort());
        return String.format("http://%s:%d", "127.0.0.1", 11434);
    }

}
