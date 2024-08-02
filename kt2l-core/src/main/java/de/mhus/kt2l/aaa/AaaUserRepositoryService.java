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
package de.mhus.kt2l.aaa;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class AaaUserRepositoryService {

    @Autowired
    private List<AaaUserRepository> userRepositories;
    @Autowired
    private LoginConfiguration loginConfig;
    @Getter
    private AaaUserRepository repository;

    @PostConstruct
    public void init() {
        var urc = loginConfig.getUserRepositoryClass();
        if (urc == null) {
            if (userRepositories.size() != 1)
                throw new IllegalArgumentException("User repository not configured properly. Options: " + userRepositories.stream().map(r -> r.getClass().getCanonicalName()).toList());
            repository = userRepositories.getFirst();
        } else {
            repository = userRepositories.stream()
                    .filter(r -> r.getClass().getCanonicalName().equals(urc))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("User repository not found: " + urc));
        }
    }

}
