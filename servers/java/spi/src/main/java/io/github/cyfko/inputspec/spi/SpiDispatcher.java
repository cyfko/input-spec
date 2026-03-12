package io.github.cyfko.inputspec.spi;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.util.*;

/**
 * Loaded once per processor round, this class discovers all registered
 * {@link FieldTransformer} and {@link FormContributor} implementations
 * via {@link ServiceLoader} and dispatches to them during FormSpec generation.
 *
 * <h2>Usage in the InputSpec processor</h2>
 * <pre>{@code
 * SpiDispatcher spi = new SpiDispatcher(messager);
 *
 * // For each element (field or interface method):
 * Optional<TransformResult> result = spi.tryTransform(fieldCtx);
 * if (result.isPresent()) {
 *     formContextBuilder.addTransformedField(
 *         result.get().ref(),
 *         result.get().json(),
 *         fieldCtx.fieldName()
 *     );
 *     formFields.add(result.get().json());
 * } else {
 *     // default InputSpec generation
 *     formContextBuilder.addDefaultField(fieldCtx.fieldName());
 *     formFields.add(defaultGenerator.generate(fieldCtx));
 * }
 *
 * // After all fields:
 * DefaultFormContext formCtx = formContextBuilder.build();
 *
 * for (String json : spi.collectAdditionalFields(formCtx)) {
 *     formFields.add(json);
 * }
 * for (String json : spi.collectAdditionalCrossConstraints(formCtx)) {
 *     crossConstraints.add(json);
 * }
 *
 * // Merge SPI bundle entries into the skeleton before writing it
 * bundle.putAll(spi.collectBundleEntries(formCtx));
 * writeBundleSkeleton(formId, bundle);
 * }</pre>
 */
public final class SpiDispatcher {

    private final List<FieldTransformer> fieldTransformers;
    private final List<FormContributor>  formContributors;
    private final Messager               messager;

    public SpiDispatcher(Messager messager) {
        this.messager          = messager;
        this.fieldTransformers = load(FieldTransformer.class);
        this.formContributors  = load(FormContributor.class);

        if (!fieldTransformers.isEmpty() || !formContributors.isEmpty()) {
            messager.printMessage(Diagnostic.Kind.NOTE,
                    String.format("[InputSpec SPI] Loaded %d FieldTransformer(s), %d FormContributor(s): %s / %s",
                            fieldTransformers.size(), formContributors.size(),
                            classNames(fieldTransformers), classNames(formContributors)));
        }
    }

    // ─── Field-level dispatch ─────────────────────────────────────────────────

    /**
     * Attempts to find a FieldTransformer that claims the given field.
     *
     * <p>The returned {@link TransformResult} carries both the generated JSON
     * and the bundle entries contributed by the transformer, so the processor
     * can merge them into the bundle skeleton in a single pass.
     *
     * @return a TransformResult if a transformer claimed the field, empty otherwise
     */
    public Optional<TransformResult> tryTransform(FieldContext ctx) {
        FieldTransformer winner = null;
        for (FieldTransformer t : fieldTransformers) {
            if (t.supports(ctx)) {
                if (winner != null) {
                    messager.printMessage(Diagnostic.Kind.WARNING,
                            String.format("[InputSpec SPI] Multiple transformers claim field '%s' in form '%s'. " +
                                            "First one wins (%s). Ignored: %s.",
                                    ctx.fieldName(), ctx.formId(),
                                    winner.getClass().getName(),
                                    t.getClass().getName()),
                            ctx.element());
                } else {
                    winner = t;
                }
            }
        }
        if (winner == null) return Optional.empty();

        String              ref    = winner.fieldRefName(ctx);
        String              json   = winner.transform(ctx);
        Map<String, String> bundle = winner.bundleEntries(ctx);
        return Optional.of(new TransformResult(ref, json, bundle));
    }

    // ─── Form-level dispatch ──────────────────────────────────────────────────

    /**
     * Collects additional InputFieldSpec JSON strings from all active FormContributors.
     */
    public List<String> collectAdditionalFields(FormContext ctx) {
        List<String> result = new ArrayList<>();
        for (FormContributor c : formContributors) {
            if (c.supports(ctx)) {
                List<String> fields = c.additionalFields(ctx);
                if (fields != null) result.addAll(fields);
            }
        }
        return result;
    }

    /**
     * Collects additional CrossConstraintDescriptor JSON strings from all active FormContributors.
     */
    public List<String> collectAdditionalCrossConstraints(FormContext ctx) {
        List<String> result = new ArrayList<>();
        for (FormContributor c : formContributors) {
            if (c.supports(ctx)) {
                List<String> constraints = c.additionalCrossConstraints(ctx);
                if (constraints != null) result.addAll(constraints);
            }
        }
        return result;
    }

    /**
     * Collects all i18n bundle entries (key → default text) from all active
     * {@link FormContributor}s.
     *
     * <p>The InputSpec processor must call this method after
     * {@link #collectAdditionalFields} and {@link #collectAdditionalCrossConstraints},
     * then merge the result into its bundle map before writing the skeleton:
     * <pre>
     *   bundle.putAll(spi.collectBundleEntries(formCtx));
     *   writeBundleSkeleton(formId, bundle, origin);
     * </pre>
     *
     * <p>This ensures that every {@code i18nKey} emitted by a contributor
     * appears in {@code META-INF/difsp/i18n/{formId}.properties}.
     *
     * @return merged map of all contributor bundle entries; never {@code null}
     */
    public Map<String, String> collectBundleEntries(FormContext ctx) {
        Map<String, String> result = new LinkedHashMap<>();
        for (FormContributor c : formContributors) {
            if (c.supports(ctx)) {
                Map<String, String> entries = c.bundleEntries(ctx);
                if (entries != null) result.putAll(entries);
            }
        }
        return result;
    }

    /**
     * Returns true if at least one FieldTransformer or FormContributor is registered.
     * Useful for skipping SPI overhead when no extensions are present.
     */
    public boolean hasExtensions() {
        return !fieldTransformers.isEmpty() || !formContributors.isEmpty();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static <T> List<T> load(Class<T> spiClass) {
        List<T> result = new ArrayList<>();
        ServiceLoader.load(spiClass, SpiDispatcher.class.getClassLoader())
                .forEach(result::add);
        return Collections.unmodifiableList(result);
    }

    private static String classNames(List<?> items) {
        if (items.isEmpty()) return "none";
        StringBuilder sb = new StringBuilder();
        for (Object item : items) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(item.getClass().getSimpleName());
        }
        return sb.toString();
    }

    // ─── Result type ──────────────────────────────────────────────────────────

    /**
     * Result of a successful field transformation.
     *
     * @param ref    the stable ref name (e.g. "AGE") — used as the field's "name" in JSON
     * @param json   the complete InputFieldSpec JSON string
     * @param bundle i18n bundle entries (key → default text) for all {@code i18nKey}
     *               references emitted in {@code json}; may be empty, never {@code null}
     */
    public record TransformResult(String ref, String json, Map<String, String> bundle) {}
}