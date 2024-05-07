package de.mhus.kt2l;

import de.mhus.kt2l.help.AbstractGitSnippetsHelpPanel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SnippetTest {

    @Test
    public void testSnippetParsing() {
        var result = AbstractGitSnippetsHelpPanel.loadSnippet( "yaml","""
# POD busybox

Simple pod template.

```yaml
# TEMPLATE BEGIN
# name: string: Name of the pod
# TEMPLATE END
apiVersion: v1
kind: Pod
metadata:
  name: ${name}
  namespace: ${namespace}
spec:
    containers:
    - name: ${name}
      image: busybox
```
busybox pod""");

        System.out.println(result.title());
        System.out.println(result.description());
        System.out.println(result.snippet());
        System.out.println(result.tags());

        assertThat(result.title()).isEqualTo("POD busybox");
        assertThat(result.description()).isEqualTo("Simple pod template.");
        assertThat(result.snippet()).isEqualTo("""
# TEMPLATE BEGIN
# name: string: Name of the pod
# TEMPLATE END
apiVersion: v1
kind: Pod
metadata:
  name: ${name}
  namespace: ${namespace}
spec:
    containers:
    - name: ${name}
      image: busybox
""".trim());
    assertThat(result.tags()).isEqualTo("busybox pod");
    }

}
