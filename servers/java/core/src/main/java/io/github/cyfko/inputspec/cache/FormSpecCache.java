package io.github.cyfko.inputspec.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cyfko.inputspec.model.FormSpecModel;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and caches {@link FormSpecModel} instances from the classpath.
 *
 * JSON specs are expected at {@code META-INF/input-spec/{formId}.json},
 * generated at compile-time by the {@code input-spec-processor} annotation processor.
 *
 * <p><b>Lifecycle:</b></p>
 * <ol>
 *   <li>Call {@link #preloadAll()} at startup to eagerly discover all JSON specs.</li>
 *   <li>Use {@link #get(String)} at runtime for O(1) lookup.</li>
 * </ol>
 *
 * Thread-safe: backed by a {@link ConcurrentHashMap}.
 */
public class FormSpecCache {

    private static final String SPEC_RESOURCE_DIR = "META-INF/input-spec/";
    private static final FormSpecCache INSTANCE = new FormSpecCache();

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, FormSpecModel> cache = new ConcurrentHashMap<>();

    protected FormSpecCache() {}

    public static FormSpecCache getInstance() { return INSTANCE; }

    /**
     * Returns the cached FormSpec for the given id.
     *
     * @param formId the form identifier (matches the JSON filename without extension)
     * @return the spec, or empty if not found
     */
    public Optional<FormSpecModel> get(String formId) {
        // Lazy load if not yet cached
        if (!cache.containsKey(formId)) {
            loadSpec(formId);
        }
        return Optional.ofNullable(cache.get(formId));
    }

    /**
     * Returns all known form ids (those that have been loaded or preloaded).
     */
    public Set<String> knownFormIds() {
        return Collections.unmodifiableSet(cache.keySet());
    }

    /**
     * Eagerly scans the classpath for all {@code META-INF/input-spec/*.json} files
     * and loads them into the cache.
     *
     * Should be called once at application startup.
     */
    public void preloadAll() {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = cl.getResources(SPEC_RESOURCE_DIR);

            while (resources.hasMoreElements()) {
                URL dirUrl = resources.nextElement();
                // For JAR resources, we can't list directory contents directly.
                // The annotation processor generates a manifest file to help discovery.
                // Fallback: load individual specs by their known paths.
            }

            // Also try the manifest approach: META-INF/input-spec/forms.list
            InputStream manifest = cl.getResourceAsStream(SPEC_RESOURCE_DIR + "forms.list");
            if (manifest != null) {
                try (Scanner scanner = new Scanner(manifest, "UTF-8")) {
                    while (scanner.hasNextLine()) {
                        String formId = scanner.nextLine().trim();
                        if (!formId.isEmpty() && !formId.startsWith("#")) {
                            loadSpec(formId);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("InputSpec: Error scanning for form specs: " + e.getMessage());
        }
    }

    /**
     * Loads a single spec from the classpath.
     */
    private void loadSpec(String formId) {
        String path = SPEC_RESOURCE_DIR + formId + ".json";
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (in == null) return;

        try (in) {
            FormSpecModel spec = mapper.readValue(in, FormSpecModel.class);
            cache.put(formId, spec);
        } catch (IOException e) {
            System.err.println("InputSpec: Failed to load " + path + ": " + e.getMessage());
        }
    }

    /**
     * Manually registers a spec (useful for testing or programmatic registration).
     */
    public void register(String formId, FormSpecModel spec) {
        cache.put(formId, spec);
    }

    /** Evicts a specific form from the cache. */
    public void evict(String formId) {
        cache.remove(formId);
    }

    /** Clears the entire cache. */
    public void evictAll() {
        cache.clear();
    }
}
