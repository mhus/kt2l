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

import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.cfg.CfgFactory;
import de.mhus.kt2l.cluster.ClusterAction;
import de.mhus.kt2l.core.CoreAction;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.resources.ResourceGridFactory;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class AaaDocGeneratorTest {

    @Test
    public void generate() throws IOException {

        List<Action> actions = new ArrayList<>();

        new Reflections("de.mhus.kt2l").getSubTypesOf(CoreAction.class).forEach(
                type -> {
                    var anno = type.getAnnotation(WithRole.class);
                    if (anno != null) {
                        actions.add(new Action(
                                "core_action",
                                type.getCanonicalName(),
                                Arrays.toString(anno.value())
                        ));
                    }
                }
        );

        new Reflections("de.mhus.kt2l").getSubTypesOf(ClusterAction.class).forEach(
                type -> {
                    var anno = type.getAnnotation(WithRole.class);
                    if (anno != null) {
                        actions.add(new Action(
                                "cluster_action",
                                type.getCanonicalName(),
                                Arrays.toString(anno.value())
                        ));
                    }
                }
        );

        new Reflections("de.mhus.kt2l").getSubTypesOf(ResourceAction.class).forEach(
                type -> {
                    var anno = type.getAnnotation(WithRole.class);
                    if (anno != null) {
                        actions.add(new Action(
                                "resource_action",
                                type.getCanonicalName(),
                                Arrays.toString(anno.value())
                        ));
                    }
                }
        );

        new Reflections("de.mhus.kt2l").getSubTypesOf(ResourceGridFactory.class).forEach(
                type -> {
                    var anno = type.getAnnotation(WithRole.class);
                    if (anno != null) {
                        actions.add(new Action(
                                "resource_grid",
                                type.getCanonicalName(),
                                Arrays.toString(anno.value())
                        ));
                    }
                }
        );

        new Reflections("de.mhus.kt2l").getSubTypesOf(CfgFactory.class).forEach(
                type -> {
                    var anno = type.getAnnotation(WithRole.class);
                    if (anno != null) {
                        actions.add(new Action(
                                "cfg",
                                type.getCanonicalName(),
                                Arrays.toString(anno.value())
                        ));
                    }
                }
        );

        var out = new File("../docs/docs/configuration/config-aaa-actions.md");
        var os = new FileWriter(out);
        writeHeader(os);
        actions.stream().map(a -> a.type).distinct().sorted().toList().forEach(type -> {
            try {
                writeTypeHeader(os, type);
                actions.stream().filter(a -> a.type.equals(type)).sorted(Comparator.comparing(a -> a.id)).forEach(action -> {
                    try {
                        writeAction(os, action);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                writeTypeFooter(os);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        writeFooter(os);
    }

    private void writeAction(FileWriter os, Action action) throws IOException {
        os .write("  " + action.id + ": " + action.defaultRoles.substring(1, action.defaultRoles.length()-1) + "\n");
    }

    private void writeTypeHeader(FileWriter os, String type) throws IOException {
        os.write(type + ":\n");
    }

    private void writeTypeFooter(FileWriter os) throws IOException {
        os.write("\n");
    }

    private void writeHeader(FileWriter os) throws IOException {
        os.write(
"""
---
sidebar_position: 20
title: Authorization Actions
---

# Authorization Configuration Actions

This document lists all known actions and their default roles. 

```yaml
"""
        );
    }

    private void writeFooter(FileWriter os) throws IOException {
        os.write("```\n");
    }

    record Action(String type, String id, String defaultRoles) {
    }
}
