package io.github.cyfko.inputspec.processor.spi;

import io.github.cyfko.inputspec.spi.FieldContext;
import io.github.cyfko.inputspec.spi.FieldTransformer;
import io.github.cyfko.inputspec.spi.FormContributor;
import io.github.cyfko.inputspec.spi.FormContext;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
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
 * // For each @FieldMeta element:
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
 *     String json = defaultGenerator.generate(fieldCtx);
 *     formContextBuilder.addDefaultField(fieldCtx.fieldName());
 *     formFields.add(json);
 * }
 *
 * // After all fields:
 * DefaultFormContext formCtx = formContextBuilder.build();
 * for (String json : spi.collectAdditionalFields(formCtx)) {
 *     formFields.add(json);
 * }
 * for (String json : spi.collectAdditionalCrossConstraints(formCtx)) {
 *     crossConstraints.add(json);
 * }
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
     * @return a TransformResult if a transformer claimed the field, empty otherwise
     */
    public Optional<TransformResult> tryTransform(FieldContext ctx) {
        FieldTransformer winner = null;
        for (FieldTransformer t : fieldTransformers) {
            if (t.supports(ctx)) {
                if (winner != null) {
                    // Conflict: two transformers claim the same field
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

        String ref  = winner.fieldRefName(ctx);
        String json = winner.transform(ctx);
        return Optional.of(new TransformResult(ref, json));
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
     * Returns true if at least one FieldTransformer or FormContributor is registered.
     * Useful for skipping SPI overhead when no extensions are present.
     */
    public boolean hasExtensions() {
        return !fieldTransformers.isEmpty() || !formContributors.isEmpty();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static <T> List<T> load(Class<T> spiClass) {
        List<T> result = new ArrayList<>();
        // Use the processor's own classloader — it sees the annotationProcessorPath
        ServiceLoader.load(spiClass, SpiDispatcher.class.getClassLoader())
                .forEach(result::add);
        return Collections.unmodifiableList(result);
    }

    private static String classNames(List<?> items) {
        if (items.isEmpty()) return "none";
        StringBuilder sb = new StringBuilder();
        for (Object item : items) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(item.getClass().getSimpleName());
        }
        return sb.toString();
    }

    // ─── Result type ──────────────────────────────────────────────────────────

    /**
     * Result of a successful field transformation.
     *
     * @param ref  the stable ref name (e.g. "AGE") — used as the field's "name" in JSON
     * @param json the complete InputFieldSpec JSON string
     */
    public record TransformResult(String ref, String json) {}
}
