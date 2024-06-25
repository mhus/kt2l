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

package de.mhus.kt2l.resources.cronjob;

import de.mhus.kt2l.generated.K8sV1CronJob;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1CronJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CronJobK8s extends K8sV1CronJob {

    @Override
    public String getDescribe(ApiProvider apiProvider, KubernetesObject res) {
        var sb = new StringBuilder();
        K8sUtil.describeHeader(apiProvider, this, res, sb);
        if (res instanceof V1CronJob) {
            var cronJob = (V1CronJob) res;
            sb.append("Schedule:           ").append(cronJob.getSpec().getSchedule()).append("\n");
            sb.append("Concurrency Policy: ").append(cronJob.getSpec().getConcurrencyPolicy()).append("\n");
            sb.append("Suspend:            ").append(cronJob.getSpec().getSuspend()).append("\n");
            sb.append("Starting Deadline Seconds:     ").append(cronJob.getSpec().getStartingDeadlineSeconds()).append("\n");
            sb.append("Successful Jobs History Limit: ").append(cronJob.getSpec().getSuccessfulJobsHistoryLimit()).append("\n");
            sb.append("Failed Jobs History Limit:     ").append(cronJob.getSpec().getFailedJobsHistoryLimit()).append("\n");
            sb.append("Last Schedule Time:            ").append(cronJob.getStatus().getLastScheduleTime()).append("\n");
        }
        K8sUtil.describeFooter(apiProvider, this, res, sb);
        return sb.toString();
    }

}
