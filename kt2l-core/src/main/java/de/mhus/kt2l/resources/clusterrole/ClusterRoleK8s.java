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

package de.mhus.kt2l.resources.clusterrole;

import de.mhus.commons.console.ConsoleTable;
import de.mhus.kt2l.generated.K8sV1ClusterRole;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ClusterRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ClusterRoleK8s extends K8sV1ClusterRole {

    @Override
    public String getDescribe(ApiProvider apiProvider, KubernetesObject res) {
        var sb = new StringBuilder();
        K8sUtil.describeHeader(apiProvider, this, res, sb);

        if (res instanceof V1ClusterRole clusterRole) {
            sb.append("Rules:\n");
            var table = new ConsoleTable();
            table.setHeaderValues("Verbs", "API Groups", "Resources", "Resource Names", "Non Resource URLs");
            for (var rule : clusterRole.getRules()) {
                table.addRowValues(
                        String.join(",", rule.getVerbs()),
                        String.join(",", rule.getApiGroups()),
                        String.join(",", rule.getResources()),
                        String.join(",", rule.getResourceNames()),
                        String.join(",", rule.getNonResourceURLs())
                );
            }
            sb.append(table);
        }
        K8sUtil.describeFooter(apiProvider, this, res, sb);
        return sb.toString();
    }

}
