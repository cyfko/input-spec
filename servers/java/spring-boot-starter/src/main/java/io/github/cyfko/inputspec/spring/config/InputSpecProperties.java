package io.github.cyfko.inputspec.spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the InputSpec Spring Boot starter.
 *
 * <p><b>Example configuration:</b></p>
 * <pre>
 * inputspec:
 *   base-path: /api/forms   # REST endpoint base path (default: /api/forms)
 *   enabled: true            # enable/disable REST endpoints (default: true)
 *   mcp:
 *     enabled: false          # enable MCP Tool Server for AI agents (default: false)
 * </pre>
 *
 * <p><b>MCP integration:</b> When {@code inputspec.mcp.enabled=true} and
 * {@code spring-ai-starter-mcp-server} is on the classpath, the starter
 * registers 4 MCP tools: {@code inputspec_list_forms}, {@code inputspec_get_form},
 * {@code inputspec_validate_form}, {@code inputspec_submit_form}.</p>
 *
 * @see InputSpecAutoConfiguration
 * @see InputSpecMcpAutoConfiguration
 */
@ConfigurationProperties(prefix = "inputspec")
public class InputSpecProperties {

    /** Base path for all InputSpec REST endpoints. Default: {@code /api/forms} */
    private String basePath = "/api/forms";

    /** Whether InputSpec auto-configuration is enabled. Default: {@code true} */
    private boolean enabled = true;

    /** MCP (Model Context Protocol) configuration for AI agent integration. */
    private Mcp mcp = new Mcp();

    public String getBasePath() { return basePath; }
    public void setBasePath(String basePath) { this.basePath = basePath; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Mcp getMcp() { return mcp; }
    public void setMcp(Mcp mcp) { this.mcp = mcp; }

    /**
     * MCP-specific properties.
     *
     * <p>Requires {@code spring-ai-starter-mcp-server} on the classpath.
     * Without it, MCP features are silently ignored even if enabled.</p>
     */
    public static class Mcp {

        /**
         * Whether MCP Tool Server integration is enabled.
         * Default: {@code false} (opt-in).
         *
         * <p>When enabled, the following MCP tools are registered:</p>
         * <ul>
         *   <li>{@code inputspec_list_forms} — discover available forms</li>
         *   <li>{@code inputspec_get_form} — get full form specification</li>
         *   <li>{@code inputspec_validate_form} — validate data without submitting</li>
         *   <li>{@code inputspec_submit_form} — validate and submit data</li>
         * </ul>
         */
        private boolean enabled = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
