package io.github.cyfko.inputspec;

import java.lang.annotation.*;

/**
 * Declares a single value alias for an INLINE ValuesEndpoint.
 *
 * Maps directly to the {@code ValueAlias} entity of the DIFSP protocol (§2.3):
 * a canonical {@code value} returned to the server, and a {@code label}
 * displayed to the user.
 *
 * The label is used as the default text. Translations are resolved from the
 * form's ResourceBundle under the key:
 * <pre>
 *   {formId}.fields.{fieldName}.items.{value}.label
 * </pre>
 *
 * Usage:
 * <pre>
 * {@literal @}FieldMeta(
 *     valuesSource = {@literal @}ValuesSource(
 *         protocol = "INLINE",
 *         items = {
 *             {@literal @}Inline(value = "ACTIVE",   label = "Active"),
 *             {@literal @}Inline(value = "INACTIVE", label = "Inactive"),
 *             {@literal @}Inline(value = "PENDING",  label = "Pending")
 *         }
 *     )
 * )
 * </pre>
 */
@Target({})                        // usable only as an annotation element value
@Retention(RetentionPolicy.SOURCE)
public @interface Inline {

    /** Canonical value submitted to the server. */
    String value();

    /** Default display label (used as fallback when no bundle entry found). */
    String label();
}