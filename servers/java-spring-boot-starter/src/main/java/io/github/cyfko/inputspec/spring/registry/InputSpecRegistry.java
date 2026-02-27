package io.github.cyfko.inputspec.spring.registry;

import io.github.cyfko.inputspec.model.InputSpec;
import io.github.cyfko.inputspec.spring.generator.InputSpecGenerator;
import io.github.cyfko.inputspec.spring.provider.InputSpecProvider;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing {@link InputSpec} instances and their providers.
 * <p>
 * This registry acts as a central repository for:
 * <ul>
 *   <li>Custom {@link InputSpecProvider} beans</li>
 *   <li>Auto-generated specs from entities</li>
 *   <li>Cached spec instances</li>
 * </ul>
 * </p>
 *
 * @author cyfko
 * @since 2.1.0
 */
public class InputSpecRegistry {

    private final InputSpecGenerator generator;
    private final Map<Class<?>, InputSpecProvider<?>> providers = new ConcurrentHashMap<>();
    private final Map<Class<?>, InputSpec> cache = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> entityNameMapping = new ConcurrentHashMap<>();

    public InputSpecRegistry(InputSpecGenerator generator) {
        this.generator = generator;
    }

    /**
     * Registers a custom provider for an entity.
     *
     * @param provider the provider to register
     * @param <T> entity type
     */
    public <T> void registerProvider(InputSpecProvider<T> provider) {
        Class<T> entityClass = provider.getEntityClass();
        providers.put(entityClass, provider);
        registerEntityName(entityClass);
    }

    /**
     * Registers an entity for auto-generation.
     *
     * @param entityClass the entity class
     */
    public void registerEntity(Class<?> entityClass) {
        registerEntityName(entityClass);
    }

    /**
     * Gets the {@link InputSpec} for an entity class.
     * <p>
     * Priority:
     * <ol>
     *   <li>Cached spec</li>
     *   <li>Custom provider</li>
     *   <li>Auto-generated spec</li>
     * </ol>
     * </p>
     *
     * @param entityClass the entity class
     * @return input spec, or empty if not found
     */
    public Optional<InputSpec> getSpec(Class<?> entityClass) {
        // Check cache first
        InputSpec cached = cache.get(entityClass);
        if (cached != null) {
            return Optional.of(cached);
        }

        // Check for custom provider
        InputSpecProvider<?> provider = providers.get(entityClass);
        if (provider != null) {
            InputSpec spec = provider.provide();
            cache.put(entityClass, spec);
            return Optional.of(spec);
        }

        // Try auto-generation
        try {
            InputSpec spec = generator.generateFromEntity(entityClass);
            cache.put(entityClass, spec);
            return Optional.of(spec);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Gets the {@link InputSpec} by entity name (e.g., "user", "product").
     *
     * @param entityName the entity name (case-insensitive, plural or singular)
     * @return input spec, or empty if not found
     */
    public Optional<InputSpec> getSpecByName(String entityName) {
        Class<?> entityClass = entityNameMapping.get(entityName.toLowerCase());
        if (entityClass != null) {
            return getSpec(entityClass);
        }
        return Optional.empty();
    }

    /**
     * Clears the cache for a specific entity.
     *
     * @param entityClass the entity class
     */
    public void invalidate(Class<?> entityClass) {
        cache.remove(entityClass);
    }

    /**
     * Clears all cached specs.
     */
    public void invalidateAll() {
        cache.clear();
    }

    /**
     * Registers entity name mappings (for lookup by name).
     */
    private void registerEntityName(Class<?> entityClass) {
        String simpleName = entityClass.getSimpleName();
        entityNameMapping.put(simpleName.toLowerCase(), entityClass);
        entityNameMapping.put(pluralize(simpleName).toLowerCase(), entityClass);
    }

    /**
     * Simple pluralization (can be improved with dedicated library).
     */
    private String pluralize(String word) {
        if (word.endsWith("y")) {
            return word.substring(0, word.length() - 1) + "ies";
        } else if (word.endsWith("s") || word.endsWith("x") || word.endsWith("ch") || word.endsWith("sh")) {
            return word + "es";
        } else {
            return word + "s";
        }
    }

    /**
     * Gets all registered entity classes.
     *
     * @return map of entity names to classes
     */
    public Map<String, Class<?>> getRegisteredEntities() {
        return Map.copyOf(entityNameMapping);
    }
}
