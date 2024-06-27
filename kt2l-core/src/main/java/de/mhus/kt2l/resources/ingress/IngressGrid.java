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

package de.mhus.kt2l.resources.ingress;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.commons.tools.MObject;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.util.AbstractGridWithNamespace;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1Ingress;
import io.kubernetes.client.openapi.models.V1IngressList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
public class IngressGrid extends AbstractGridWithNamespace<IngressGrid.Resource, Component, V1Ingress, V1IngressList> {

    @Autowired
    private PanelService panelService;

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return IngressWatch.class;
    }

    @Override
    protected Class<Resource> getManagedResourceItemClass() {
        return Resource.class;
    }

    @Override
    protected void createGridColumnsAfterName(Grid<Resource> resourcesGrid) {
        resourcesGrid.addColumn(Resource::getClazz).setHeader("Class").setSortProperty("class");
        resourcesGrid.addColumn(Resource::getHosts).setHeader("Hosts").setSortProperty("hosts");
        resourcesGrid.addColumn(Resource::getAddress).setHeader("Address").setSortProperty("address");
        resourcesGrid.addColumn(Resource::getPorts).setHeader("Ports").setSortProperty("ports");
    }

    @Override
    protected int sortColumn(String sorted, SortDirection direction, Resource a, Resource b) {
        return switch (sorted) {
            case "class" -> switch (direction) {
                case ASCENDING -> MObject.compareTo(a.getClazz(), b.getClazz());
                case DESCENDING -> MObject.compareTo(b.getClazz(), a.getClazz());
            };
            case "hosts" -> switch (direction) {
                case ASCENDING -> MObject.compareTo(a.getHosts(), b.getHosts());
                case DESCENDING -> MObject.compareTo(b.getHosts(), a.getHosts());
            };
            case "address" -> switch (direction) {
                case ASCENDING -> MObject.compareTo(a.getAddress(), b.getAddress());
                case DESCENDING -> MObject.compareTo(b.getAddress(), a.getAddress());
            };
            case "ports" -> switch (direction) {
                case ASCENDING -> MObject.compareTo(a.getPorts(), b.getPorts());
                case DESCENDING -> MObject.compareTo(b.getPorts(), a.getPorts());
            };
            default -> 0;
        };
    }

    @Override
    protected Resource createResourceItem() {
        return new Resource();
    }

    @Override
    public V1APIResource getManagedType() {
        return K8s.INGRESS;
    }

    @Override
    protected void onShowDetails(Resource item, boolean flip) {
    }

    @Getter
    public static class Resource extends ResourceItem<V1Ingress> {
        private String clazz;
        private String hosts;
        private String address;
        private String ports;

        @Override
        public void updateResource() {
            super.updateResource();
            clazz = resource.getSpec().getIngressClassName();
            hosts = tryThis(() -> resource.getSpec().getRules().stream().map(r -> r.getHost()).reduce((a, b) -> a + ", " + b).orElse("")).orElse("");
            address = tryThis(() -> resource.getStatus().getLoadBalancer().getIngress().stream().map(r -> r.getHostname()).reduce((a,b) -> a + "," + b).orElse("") ).orElse("");
            ports = tryThis(() -> resource.getSpec().getRules().stream().map(r -> r.getHttp().getPaths().stream().map(p -> String.valueOf(p.getBackend().getService().getPort().getNumber())).reduce((a, b) -> a + ", " + b).orElse("")).reduce((a, b) -> a + ", " + b).orElse("")).orElse("");
            setColor(null);
        }

        private String toStringOr0(Integer integer) {
            if (integer == null) return "0";
            return integer.toString();
        }
    }
}
