package io.github.cyfko.inputspec.spring;

import io.github.cyfko.inputspec.cache.BundleResolver;
import io.github.cyfko.inputspec.cache.FormSpecCache;
import io.github.cyfko.inputspec.validation.FormSpecValidator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for InputSpec MCP (Model Context Protocol) integration.
 *
 * <p>Activates only when <b>both</b> conditions are met:</p>
 * <ul>
 *   <li>Spring AI's {@code @Tool} annotation is on the classpath
 *       (i.e. the user added {@code spring-ai-starter-mcp-server} to their project)</li>
 *   <li>{@code inputspec.mcp.enabled=true} in the application configuration</li>
 * </ul>
 *
 * <p>When active, registers the {@link InputSpecMcpTools} bean which exposes
 * 4 MCP tools for AI agent interaction:</p>
 * <ul>
 *   <li>{@code inputspec_list_forms} — discover available forms</li>
 *   <li>{@code inputspec_get_form} — get full form specification</li>
 *   <li>{@code inputspec_validate_form} — validate data without submitting</li>
 *   <li>{@code inputspec_submit_form} — validate and submit data</li>
 * </ul>
 *
 * <p>This configuration is separate from {@link InputSpecAutoConfiguration} to ensure
 * that the Spring AI dependency does not cause {@link ClassNotFoundException} when
 * the user has not included it.</p>
 *
 * @see InputSpecMcpTools
 * @see InputSpecProperties.Mcp
 */
@AutoConfiguration(after = InputSpecAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.ai.tool.annotation.Tool")
@ConditionalOnProperty(prefix = "inputspec.mcp", name = "enabled", havingValue = "true")
public class InputSpecMcpAutoConfiguration {

    /**
     * Registers the {@link InputSpecMcpTools} bean that exposes InputSpec forms
     * as MCP tools for AI agents.
     *
     * <p>Re-uses existing beans from {@link InputSpecAutoConfiguration}:
     * {@link FormSpecCache}, {@link FormSpecValidator}, {@link FormHandlerRegistry},
     * and {@link BundleResolver}.</p>
     *
     * @param cache          the form specification cache
     * @param validator      the form validator
     * @param registry       the form handler registry
     * @param bundleResolver the i18n bundle resolver
     * @return the configured MCP tools instance
     */
    @Bean
    @ConditionalOnMissingBean
    public InputSpecMcpTools inputSpecMcpTools(FormSpecCache cache,
                                               FormSpecValidator validator,
                                               FormHandlerRegistry registry,
                                               BundleResolver bundleResolver) {
        return new InputSpecMcpTools(cache, validator, registry, bundleResolver);
    }
}
