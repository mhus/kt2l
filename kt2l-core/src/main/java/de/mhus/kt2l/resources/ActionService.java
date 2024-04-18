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

package de.mhus.kt2l.resources;

import de.mhus.kt2l.config.AaaConfiguration;
import de.mhus.kt2l.core.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;

@Component
public class ActionService {

    @Autowired(required = false)
    private Collection<ResourceAction> actions;

    @Autowired
    private SecurityService securityService;


    public Collection<ResourceAction> findActionsForResource(String resourceType) {
        if (actions == null) return Collections.emptyList();
        return actions.stream().filter(a -> hasAccess(a) && canHandle(resourceType, a)).toList();
    }

    private boolean canHandle(String resourceType, ResourceAction a) {
        return a.canHandleResourceType(resourceType);
    }

    private boolean hasAccess(ResourceAction a) {
        return securityService.hasRole(AaaConfiguration.SCOPE_RESOURCE_ACTION, a);
    }

}
