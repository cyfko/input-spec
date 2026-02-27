package io.github.cyfko.inputspec.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the InputSpec Spring Boot starter.
 *
 * <pre>
 * inputspec:
 *   base-path: /api/forms
 *   enabled: true
 * </pre>
 */
@ConfigurationProperties(prefix = "inputspec")
public class InputSpecProperties {

    /** Base path for all InputSpec REST endpoints. Default: {@code /api/forms} */
    private String basePath = "/api/forms";

    /** Whether InputSpec auto-configuration is enabled. Default: {@code true} */
    private boolean enabled = true;

    public String getBasePath() { return basePath; }
    public void setBasePath(String basePath) { this.basePath = basePath; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
