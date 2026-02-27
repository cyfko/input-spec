package io.github.cyfko.inputspec.spring.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Input Spec Spring Boot Starter.
 * <p>
 * Prefix: {@code input-spec}
 * </p>
 *
 * <h2>Example application.yml:</h2>
 * <pre>
 * input-spec:
 *   enabled: true
 *   auto-scan: true
 *   entity-scan-packages:
 *     - com.example.myapp.domain
 *     - com.example.myapp.entities
 *   base-path: /api/v1
 *   endpoint-suffix: /input-spec
 * </pre>
 *
 * @author cyfko
 * @since 2.1.0
 */
@ConfigurationProperties(prefix = "input-spec")
public class InputSpecProperties {

    /**
     * Whether input-spec auto-configuration is enabled.
     */
    private boolean enabled = true;

    /**
     * Whether to automatically scan for {@link io.github.cyfko.inputspec.spring.annotations.InputSpecEnabled} entities.
     */
    private boolean autoScan = true;

    /**
     * Base packages to scan for entities.
     * <p>
     * If empty, uses the application's main package.
     * </p>
     */
    private String[] entityScanPackages = new String[0];

    /**
     * Base path prefix for all input-spec endpoints.
     * <p>
     * Default: empty (inherits from server context path)
     * </p>
     */
    private String basePath = "";

    /**
     * Endpoint suffix for input-spec endpoints.
     * <p>
     * Default: "/input-spec"
     * </p>
     * <p>
     * Example: If entity is "User", endpoint will be "/users/input-spec"
     * </p>
     */
    private String endpointSuffix = "/input-spec";

    /**
     * Whether to enable CORS for input-spec endpoints.
     */
    private boolean enableCors = false;

    /**
     * CORS allowed origins (if enableCors is true).
     */
    private String[] corsAllowedOrigins = new String[]{"*"};

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAutoScan() {
        return autoScan;
    }

    public void setAutoScan(boolean autoScan) {
        this.autoScan = autoScan;
    }

    public String[] getEntityScanPackages() {
        return entityScanPackages;
    }

    public void setEntityScanPackages(String[] entityScanPackages) {
        this.entityScanPackages = entityScanPackages;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getEndpointSuffix() {
        return endpointSuffix;
    }

    public void setEndpointSuffix(String endpointSuffix) {
        this.endpointSuffix = endpointSuffix;
    }

    public boolean isEnableCors() {
        return enableCors;
    }

    public void setEnableCors(boolean enableCors) {
        this.enableCors = enableCors;
    }

    public String[] getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(String[] corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }
}
