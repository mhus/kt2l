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

package de.mhus.kt2l.resources.clusterrolebinding;

import de.mhus.commons.console.ConsoleTable;
import de.mhus.kt2l.generated.K8sV1ClusterRoleBinding;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ClusterRoleBinding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ClusterRoleBindingK8s extends K8sV1ClusterRoleBinding {

    @Override
    public String getDescribe(ApiProvider apiProvider, KubernetesObject res) {
        var sb = new StringBuilder();
        K8sUtil.describeHeader(apiProvider, this, res, sb);

        if (res instanceof V1ClusterRoleBinding clusterRoleBinding) {
            sb.append("Role: ").append(clusterRoleBinding.getRoleRef().getName()).append("\n");
            sb.append("Subjects: ").append(clusterRoleBinding.getSubjects().size()).append("\n");
            var table = new ConsoleTable();
            table.setHeaderValues("Kind", "Name", "Namespace");
            for (var subject : clusterRoleBinding.getSubjects()) {
                table.addRowValues(subject.getKind(), subject.getName(), subject.getNamespace());
            }
            sb.append(table);
        }
        K8sUtil.describeFooter(apiProvider, this, res, sb);
        return sb.toString();
    }

}
