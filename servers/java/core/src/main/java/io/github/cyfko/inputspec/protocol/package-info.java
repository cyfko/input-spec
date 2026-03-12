/**
 * DIFSP protocol value sets — single source of truth.
 *
 * <p>These enums are the canonical representation of every closed value set
 * defined in the protocol specification. They are used at three levels:</p>
 *
 * <ol>
 *   <li><b>Annotations</b> — {@code @FormSpec}, {@code @ValuesSource}, etc. (SOURCE retention)</li>
 *   <li><b>Runtime models</b> — {@link io.github.cyfko.inputspec.model.InputFieldSpec},
 *       {@link io.github.cyfko.inputspec.model.ValuesEndpoint}, etc. (Jackson deserialization)</li>
 *   <li><b>Validator</b> — {@link io.github.cyfko.inputspec.validation.FormSpecValidator}
 *       switch expressions (exhaustiveness check)</li>
 * </ol>
 *
 * <p>Jackson deserialization uses {@code @JsonCreator} on each enum to handle:</p>
 * <ul>
 *   <li>Case-insensitive matching</li>
 *   <li>CamelCase JSON values that differ from the Java UPPER_SNAKE name
 *       (e.g. {@code "fieldComparison"} → {@code FIELD_COMPARISON})</li>
 *   <li>Unknown values gracefully via an {@code UNKNOWN} sentinel where the protocol
 *       mandates tolerance of future extensions</li>
 * </ul>
 */
package io.github.cyfko.inputspec.protocol;
