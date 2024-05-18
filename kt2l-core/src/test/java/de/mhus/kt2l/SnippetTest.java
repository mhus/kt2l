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
package de.mhus.kt2l;

import de.mhus.kt2l.help.AbstractGitSnippetsHelpPanel;
import de.mhus.kt2l.help.SnippetsService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SnippetTest {

    @Test
    public void testSnippetParsing() {
        var snippets = new SnippetsService.Snippets();
        snippets.setCodeType("yaml");
        var result = snippets.loadSnippet( """
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
