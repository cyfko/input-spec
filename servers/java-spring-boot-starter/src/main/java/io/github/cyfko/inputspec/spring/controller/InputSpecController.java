package io.github.cyfko.inputspec.spring.controller;

import io.github.cyfko.inputspec.model.InputSpec;
import io.github.cyfko.inputspec.spring.autoconfigure.InputSpecProperties;
import io.github.cyfko.inputspec.spring.registry.InputSpecRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for serving input-spec endpoints.
 * <p>
 * Provides endpoints for:
 * <ul>
 *   <li>GET /{entityName}/input-spec - Get spec for specific entity</li>
 *   <li>GET /input-spec - List all registered entities</li>
 * </ul>
 * </p>
 *
 * @author cyfko
 * @since 2.1.0
 */
@RestController
public class InputSpecController {

    private final InputSpecRegistry registry;
    private final InputSpecProperties properties;

    public InputSpecController(InputSpecRegistry registry, InputSpecProperties properties) {
        this.registry = registry;
        this.properties = properties;
    }

    /**
     * Gets the input-spec for a specific entity.
     * <p>
     * Example: GET /api/users/input-spec
     * </p>
     *
     * @param entityName entity name (plural or singular, case-insensitive)
     * @return input spec document
     */
    @GetMapping("${input-spec.base-path:}/{entityName}${input-spec.endpoint-suffix:/input-spec}")
    @CrossOrigin(origins = "${input-spec.cors-allowed-origins:*}")
    public ResponseEntity<InputSpec> getInputSpec(@PathVariable String entityName) {
        return registry.getSpecByName(entityName)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lists all registered entities with their input-spec endpoint URLs.
     * <p>
     * Example: GET /input-spec
     * </p>
     *
     * @return map of entity names to endpoint URLs
     */
    @GetMapping("${input-spec.base-path:}/input-spec")
    @CrossOrigin(origins = "${input-spec.cors-allowed-origins:*}")
    public ResponseEntity<Map<String, String>> listEntities() {
        Map<String, String> entities = registry.getRegisteredEntities().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> buildEndpointUrl(entry.getKey())
            ));

        return ResponseEntity.ok(entities);
    }

    /**
     * Invalidates the cache for a specific entity (for development/testing).
     * <p>
     * Example: DELETE /users/input-spec/cache
     * </p>
     *
     * @param entityName entity name
     * @return no content response
     */
    @DeleteMapping("${input-spec.base-path:}/{entityName}${input-spec.endpoint-suffix:/input-spec}/cache")
    @CrossOrigin(origins = "${input-spec.cors-allowed-origins:*}")
    public ResponseEntity<Void> invalidateCache(@PathVariable String entityName) {
        return registry.getRegisteredEntities().entrySet().stream()
            .filter(entry -> entry.getKey().equalsIgnoreCase(entityName))
            .findFirst()
            .map(entry -> {
                registry.invalidate(entry.getValue());
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Builds the full endpoint URL for an entity.
     */
    private String buildEndpointUrl(String entityName) {
        String basePath = properties.getBasePath();
        String suffix = properties.getEndpointSuffix();

        StringBuilder url = new StringBuilder();
        if (!basePath.isEmpty()) {
            url.append(basePath);
        }
        url.append("/").append(entityName).append(suffix);

        return url.toString();
    }
}
