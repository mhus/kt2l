/*
 * kt2l-native - kt2l as native server
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

import de.mhus.commons.directory.ClassLoaderResourceProvider;
import de.mhus.commons.services.DefaultEnvironmentProvider;
import de.mhus.commons.tree.DefaultNodeFactory;
import org.reflections.Reflections;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import java.util.Set;
import java.util.regex.Pattern;

@Configuration
@ImportRuntimeHints(NativeConfiguration.TypeMappedAnnotationArrayRuntimeHints.class)
public class NativeConfiguration {


    public static class TypeMappedAnnotationArrayRuntimeHints implements RuntimeHintsRegistrar {
        private RuntimeHints hints;

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            this.hints = hints;
            // spring
            registerType("org.springframework.core.annotation.TypeMappedAnnotation[]");
            // mhus-commons
            registerType(DefaultEnvironmentProvider.class);
            registerType(ClassLoaderResourceProvider.class);
            registerType(DefaultNodeFactory.class);
            // vaadin
            registerType("com.vaadin.copilot.SpringIntegration");
//            new Reflections("de.f0rce.ace").getTypesAnnotatedWith(EventData.class).forEach(
//                    type -> registerType(type)
//            );
            registerType(de.f0rce.ace.events.AceChanged.class);
            registerType(de.f0rce.ace.events.AceBlurChanged.class);
            registerType(de.f0rce.ace.events.AceForceSyncDomEvent.class);
            registerType(de.f0rce.ace.events.AceSelectionChanged.class);
            registerType(de.f0rce.ace.events.AceHTMLGeneratedEvent.class);
            registerType(com.flowingcode.vaadin.addons.xterm.ITerminalConsole.AnyKeyEvent.class);

            // kt2l
            new Reflections("de.mhus.kt2l").getTypesAnnotatedWith(Configurable.class).forEach(
                    type -> registerType(type)
            );
            // kubernetes

            final ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
            provider.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));
            {
                final Set<BeanDefinition> classes = provider.findCandidateComponents("io.kubernetes.client.openapi.models");
                classes.forEach(
                        beanDefinition -> {
                            try {
                                registerType(Class.forName(beanDefinition.getBeanClassName()));
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                );
            }
            {
                final Set<BeanDefinition> classes = provider.findCandidateComponents("io.kubernetes.client.custom");
                classes.forEach(
                        beanDefinition -> {
                            try {
                                registerType(Class.forName(beanDefinition.getBeanClassName()));
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                );
            }
            // com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest.class

            registerType("sun.security.smartcardio.SunPCSC");
            registerType("org.jcp.xml.dsig.internal.dom.XMLDSigRI");
            registerType("sun.security.pkcs11.SunPKCS11");

            {
                final Set<BeanDefinition> classes = provider.findCandidateComponents("io.kubernetes.client.util");
                classes.forEach(
                        beanDefinition -> {
                            try {
                                registerType(Class.forName(beanDefinition.getBeanClassName()));
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                );
            }

            registerType(io.kubernetes.client.custom.Quantity.QuantityAdapter.class);

            // resources
            hints.resources().registerPattern(".*\\.properties");
        }

        private void registerType(String type) {
            System.out.println("Register Type: " + type);
            hints.reflection().registerType(TypeReference.of(type),
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS, MemberCategory.INTROSPECT_DECLARED_METHODS, MemberCategory.DECLARED_CLASSES, MemberCategory.INVOKE_DECLARED_METHODS);

        }

        private void registerType(Class<?> type) {
            System.out.println("Register Type: " + type);
            hints.reflection().registerType(type,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS, MemberCategory.INTROSPECT_DECLARED_METHODS, MemberCategory.DECLARED_CLASSES, MemberCategory.INVOKE_DECLARED_METHODS);

        }

    }

}
