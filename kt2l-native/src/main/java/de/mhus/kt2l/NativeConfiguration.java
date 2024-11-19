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
import org.springframework.core.type.filter.AnnotationTypeFilter;
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
            registerType(com.vaadin.copilot.SpringIntegration.class);
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
            registerType(io.kubernetes.client.custom.Quantity.QuantityAdapter.class);
        }

        private void registerType(String type) {
            System.out.println("Register Type: " + type);
            hints.reflection().registerType(TypeReference.of(type),
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS, MemberCategory.INTROSPECT_DECLARED_METHODS);

        }

        private void registerType(Class<?> type) {
            System.out.println("Register Type: " + type);
            hints.reflection().registerType(type,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS, MemberCategory.INTROSPECT_DECLARED_METHODS);

        }

    }

}
