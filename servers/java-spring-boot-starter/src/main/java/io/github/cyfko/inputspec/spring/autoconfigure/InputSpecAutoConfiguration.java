package io.github.cyfko.inputspec.spring.autoconfigure;

import io.github.cyfko.inputspec.spring.annotations.InputSpecEnabled;
import io.github.cyfko.inputspec.spring.controller.InputSpecController;
import io.github.cyfko.inputspec.spring.generator.InputSpecGenerator;
import io.github.cyfko.inputspec.spring.provider.InputSpecProvider;
import io.github.cyfko.inputspec.spring.registry.InputSpecRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Map;

/**
 * Auto-configuration for Input Spec Spring Boot Starter.
 * <p>
 * This configuration:
 * <ul>
 *   <li>Scans for {@link InputSpecEnabled} annotated entities</li>
 *   <li>Registers {@link InputSpecProvider} beans</li>
 *   <li>Configures {@link InputSpecController} for REST endpoints</li>
 *   <li>Sets up {@link InputSpecGenerator} for automatic spec generation</li>
 * </ul>
 * </p>
 *
 * @author cyfko
 * @since 2.1.0
 */
@AutoConfiguration
@ConditionalOnClass(WebMvcConfigurer.class)
@EnableConfigurationProperties(InputSpecProperties.class)
public class InputSpecAutoConfiguration {

    /**
     * Creates the {@link InputSpecGenerator} bean.
     */
    @Bean
    @ConditionalOnMissingBean
    public InputSpecGenerator inputSpecGenerator() {
        return new InputSpecGenerator();
    }

    /**
     * Creates the {@link InputSpecRegistry} bean.
     * <p>
     * The registry holds mappings between entity classes and their providers/specs.
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    public InputSpecRegistry inputSpecRegistry(
            ApplicationContext applicationContext,
            InputSpecGenerator generator,
            InputSpecProperties properties) {

        InputSpecRegistry registry = new InputSpecRegistry(generator);

        // Register custom providers
        Map<String, InputSpecProvider> providers = applicationContext.getBeansOfType(InputSpecProvider.class);
        for (InputSpecProvider<?> provider : providers.values()) {
            registry.registerProvider(provider);
        }

        // Scan for @InputSpecEnabled entities
        if (properties.isAutoScan()) {
            scanAndRegisterEntities(registry, properties);
        }

        return registry;
    }

    /**
     * Creates the {@link InputSpecController} bean.
     */
    @Bean
    @ConditionalOnMissingBean
    public InputSpecController inputSpecController(
            InputSpecRegistry registry,
            InputSpecProperties properties) {
        return new InputSpecController(registry, properties);
    }

    /**
     * Scans for entities annotated with {@link InputSpecEnabled}.
     */
    private void scanAndRegisterEntities(InputSpecRegistry registry, InputSpecProperties properties) {
        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AnnotationTypeFilter(InputSpecEnabled.class));

        String[] basePackages = properties.getEntityScanPackages();
        if (basePackages == null || basePackages.length == 0) {
            // Default: scan application's base package
            return;
        }

        for (String basePackage : basePackages) {
            scanner.findCandidateComponents(basePackage).forEach(beanDefinition -> {
                try {
                    Class<?> entityClass = Class.forName(beanDefinition.getBeanClassName());
                    registry.registerEntity(entityClass);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Failed to load entity class: " + beanDefinition.getBeanClassName(), e);
                }
            });
        }
    }
}
