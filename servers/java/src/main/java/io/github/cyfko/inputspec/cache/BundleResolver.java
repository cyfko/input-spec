package io.github.cyfko.inputspec.cache;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves i18n keys from DIFSP ResourceBundle files at runtime.
 *
 * Bundles are loaded from:
 *   META-INF/difsp/i18n/{formId}_{locale}.properties
 *
 * Resolution cascade for a given locale (e.g. {@code fr-CA}):
 *   1. {formId}_fr-CA.properties   exact locale
 *   2. {formId}_fr.properties       language-only fallback
 *   3. {formId}.properties          default bundle (skeleton)
 *   4. {@code defaultText}          value declared in the annotation
 *
 * Bundles are cached after first load. Call {@link #evict(String)} to force
 * a reload (e.g. after hot-deploying a translation file).
 *
 * Usage:
 * <pre>
 *   BundleResolver resolver = BundleResolver.getInstance();
 *
 *   // Resolve a LocalizedString JsonNode with a locale
 *   String label = resolver.resolve(field.displayName(), "booking-form", Locale.FRENCH);
 *
 *   // Resolve a known key directly
 *   String msg = resolver.resolveKey("booking-form", "booking-form.crossConstraints.dateRange.errorMessage", Locale.FRENCH)
 *                        .orElse("End date must be after start date");
 * </pre>
 */
public final class BundleResolver {

    private static final String BUNDLE_BASE = "META-INF/difsp/i18n/";
    private static final BundleResolver INSTANCE = new BundleResolver();

    /** Cache key: "{formId}_{locale-tag}" → Properties */
    private final Map<String, Properties> cache = new ConcurrentHashMap<>();

    private BundleResolver() {}

    public static BundleResolver getInstance() { return INSTANCE; }

    // ─── Main resolution entry point ──────────────────────────────────────────

    /**
     * Resolves a {@code LocalizedString} JsonNode to a plain string.
     *
     * The node may be:
     * <ul>
     *   <li>A plain string → returned as-is</li>
     *   <li>An i18n object {@code {"default": "...", "i18nKey": "..."}} →
     *       the key is looked up in the bundle for the given locale;
     *       falls back to {@code "default"} if not found</li>
     * </ul>
     *
     * @param node      The JsonNode representing the LocalizedString
     * @param formId    The form id (used to locate the bundle file)
     * @param locale    The requested locale (may be null → returns default text)
     */
    public String resolve(JsonNode node, String formId, Locale locale) {
        if (node == null || node.isNull() || node.isMissingNode()) return "";
        if (node.isTextual()) return node.asText();

        String defaultText = node.path("default").asText("");
        String i18nKey     = node.path("i18nKey").asText("");

        if (i18nKey.isEmpty() || locale == null) return defaultText;

        return resolveKey(formId, i18nKey, locale).orElse(defaultText);
    }

    /**
     * Looks up a specific key in the bundle cascade for the given locale.
     *
     * @return the resolved value, or {@link Optional#empty()} if not found anywhere
     */
    public Optional<String> resolveKey(String formId, String key, Locale locale) {
        // 1. Exact locale (e.g. fr-CA → "fr-CA")
        Optional<String> value = lookupInBundle(formId, locale.toLanguageTag(), key);
        if (value.isPresent()) return value;

        // 2. Language-only fallback (e.g. fr-CA → "fr")
        if (!locale.getCountry().isEmpty()) {
            value = lookupInBundle(formId, locale.getLanguage(), key);
            if (value.isPresent()) return value;
        }

        // 3. Default bundle (skeleton, no locale suffix)
        return lookupInBundle(formId, null, key);
    }

    // ─── Bundle loading ───────────────────────────────────────────────────────

    private Optional<String> lookupInBundle(String formId, String localeSuffix, String key) {
        String cacheKey  = formId + (localeSuffix != null ? "_" + localeSuffix : "");
        Properties props = cache.computeIfAbsent(cacheKey, k -> loadBundle(formId, localeSuffix));
        if (props.isEmpty()) return Optional.empty();
        return Optional.ofNullable(props.getProperty(key));
    }

    private Properties loadBundle(String formId, String localeSuffix) {
        String filename = formId + (localeSuffix != null ? "_" + localeSuffix : "") + ".properties";
        String path     = BUNDLE_BASE + filename;

        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (in == null) return new Properties(); // not found → empty, will be cached

        Properties props = new Properties();
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            // Log but don't throw — missing bundle falls back gracefully
            System.err.println("DIFSP: Could not load bundle " + path + ": " + e.getMessage());
        }
        return props;
    }

    // ─── Cache management ─────────────────────────────────────────────────────

    /** Evicts all cached bundles for a given form id (all locales). */
    public void evict(String formId) {
        cache.keySet().removeIf(k -> k.equals(formId) || k.startsWith(formId + "_"));
    }

    /** Evicts a specific locale bundle. */
    public void evict(String formId, Locale locale) {
        cache.remove(formId + "_" + locale.toLanguageTag());
        cache.remove(formId + "_" + locale.getLanguage());
    }

    /** Clears the entire bundle cache. */
    public void evictAll() { cache.clear(); }
}