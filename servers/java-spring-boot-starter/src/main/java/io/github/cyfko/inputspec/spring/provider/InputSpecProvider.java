package io.github.cyfko.inputspec.spring.provider;

import io.github.cyfko.inputspec.model.InputSpec;

/**
 * Strategy interface for providing custom {@link InputSpec} instances.
 * <p>
 * Implement this interface to gain full programmatic control over input-spec generation,
 * bypassing the automatic generation from {@link io.github.cyfko.inputspec.spring.annotations.InputSpecEnabled}.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * @Component
 * public class UserFormProvider implements InputSpecProvider<User> {
 *
 *     @Override
 *     public Class<User> getEntityClass() {
 *         return User.class;
 *     }
 *
 *     @Override
 *     public InputSpec provide() {
 *         return InputSpec.builder()
 *             .protocolVersion("2.1")
 *             .addField(InputFieldSpec.builder()
 *                 .displayName("Username")
 *                 .description("Unique login identifier")
 *                 .dataType(DataType.STRING)
 *                 .required(true)
 *                 .constraints(List.of(
 *                     ConstraintDescriptor.builder()
 *                         .name("minLength")
 *                         .type(ConstraintType.MIN_LENGTH)
 *                         .params(3)
 *                         .build(),
 *                     ConstraintDescriptor.builder()
 *                         .name("pattern")
 *                         .type(ConstraintType.PATTERN)
 *                         .params("^[a-zA-Z0-9_]+$")
 *                         .errorMessage("Only alphanumeric and underscores allowed")
 *                         .build()
 *                 ))
 *                 .build())
 *             .addField(InputFieldSpec.builder()
 *                 .displayName("Country")
 *                 .dataType(DataType.STRING)
 *                 .valuesEndpoint(ValuesEndpoint.builder()
 *                     .protocol(ValuesEndpoint.Protocol.HTTP)
 *                     .mode(ValuesEndpoint.Mode.CLOSED)
 *                     .uri("/api/countries")
 *                     .method(ValuesEndpoint.HttpMethod.GET)
 *                     .searchParams(Map.of("lang", "en"))
 *                     .searchParamsSchema(Map.of(
 *                         "type", "object",
 *                         "properties", Map.of(
 *                             "lang", Map.of("type", "string", "enum", List.of("en", "fr"))
 *                         )
 *                     ))
 *                     .cacheStrategy(ValuesEndpoint.CacheStrategy.builder()
 *                         .type(ValuesEndpoint.CacheStrategy.Type.PERSISTENT)
 *                         .ttlSeconds(3600)
 *                         .build())
 *                     .debounceMs(300)
 *                     .build())
 *                 .build())
 *             .build();
 *     }
 * }
 * }</pre>
 *
 * <h2>Priority</h2>
 * <p>
 * If a {@code InputSpecProvider} bean is found for an entity class, it takes
 * precedence over automatic generation from {@code @InputSpecEnabled}.
 * </p>
 *
 * @param <T> the entity type this provider handles
 * @author cyfko
 * @since 2.1.0
 * @see io.github.cyfko.inputspec.spring.annotations.InputSpecEnabled
 */
public interface InputSpecProvider<T> {

    /**
     * Returns the entity class this provider handles.
     * <p>
     * Used to match the provider with the correct entity during Spring context scanning.
     * </p>
     *
     * @return entity class
     */
    Class<T> getEntityClass();

    /**
     * Provides the {@link InputSpec} for the entity.
     * <p>
     * This method is called when the input-spec endpoint is accessed.
     * Implementations can build the spec programmatically using the builder API.
     * </p>
     *
     * @return complete input specification
     */
    InputSpec provide();
}
