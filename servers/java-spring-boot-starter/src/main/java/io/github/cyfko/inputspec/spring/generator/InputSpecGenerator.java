package io.github.cyfko.inputspec.spring.generator;

import io.github.cyfko.inputspec.model.*;
import io.github.cyfko.inputspec.spring.annotations.InputField;
import io.github.cyfko.inputspec.spring.annotations.InputSpecEnabled;
import io.github.cyfko.inputspec.spring.annotations.ValuesEndpointConfig;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.*;

/**
 * Automatic generator for {@link InputSpec} from JPA entities.
 * <p>
 * Analyzes entity classes annotated with {@link InputSpecEnabled} and generates
 * complete input-spec documents by:
 * <ul>
 *   <li>Auto-detecting data types from Java field types</li>
 *   <li>Extracting constraints from Bean Validation annotations</li>
 *   <li>Reading JPA metadata ({@code @Column}, {@code @Enumerated})</li>
 *   <li>Merging with explicit {@link InputField} metadata</li>
 * </ul>
 * </p>
 *
 * @author cyfko
 * @since 2.1.0
 */
@Component
public class InputSpecGenerator {

    /**
     * Generates an {@link InputSpec} from an entity class.
     *
     * @param entityClass the entity class annotated with {@link InputSpecEnabled}
     * @return complete input specification
     */
    public InputSpec generateFromEntity(Class<?> entityClass) {
        InputSpecEnabled config = entityClass.getAnnotation(InputSpecEnabled.class);
        if (config == null) {
            throw new IllegalArgumentException(
                "Class " + entityClass.getName() + " is not annotated with @InputSpecEnabled"
            );
        }

        InputSpec.Builder specBuilder = InputSpec.builder()
            .protocolVersion(config.protocolVersion());

        List<Field> fields = getFields(entityClass, config.explicitFieldsOnly());

        for (Field field : fields) {
            InputFieldSpec fieldSpec = generateFieldSpec(field);
            if (fieldSpec != null) {
                specBuilder.addField(fieldSpec);
            }
        }

        return specBuilder.build();
    }

    /**
     * Gets the list of fields to include in the spec.
     */
    private List<Field> getFields(Class<?> entityClass, boolean explicitOnly) {
        List<Field> result = new ArrayList<>();
        Field[] declaredFields = entityClass.getDeclaredFields();

        for (Field field : declaredFields) {
            InputField inputField = field.getAnnotation(InputField.class);

            // Skip excluded fields
            if (inputField != null && inputField.exclude()) {
                continue;
            }

            // If explicitOnly, skip fields without @InputField
            if (explicitOnly && inputField == null) {
                continue;
            }

            // Skip static and transient fields
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) ||
                java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                continue;
            }

            result.add(field);
        }

        return result;
    }

    /**
     * Generates {@link InputFieldSpec} from a Java field.
     */
    private InputFieldSpec generateFieldSpec(Field field) {
        InputField annotation = field.getAnnotation(InputField.class);

        InputFieldSpec.Builder builder = InputFieldSpec.builder()
            .displayName(getDisplayName(field, annotation))
            .description(getDescription(annotation))
            .dataType(detectDataType(field))
            .expectMultipleValues(detectMultipleValues(field, annotation))
            .required(detectRequired(field, annotation))
            .formatHint(getFormatHint(field, annotation));

        // Add values endpoint if configured
        ValuesEndpoint valuesEndpoint = buildValuesEndpoint(field, annotation);
        if (valuesEndpoint != null) {
            builder.valuesEndpoint(valuesEndpoint);
        }

        // Add constraints from Bean Validation
        List<ConstraintDescriptor> constraints = extractConstraints(field);
        if (!constraints.isEmpty()) {
            builder.constraints(constraints);
        }

        return builder.build();
    }

    /**
     * Determines the display name for a field.
     */
    private String getDisplayName(Field field, InputField annotation) {
        if (annotation != null && !annotation.displayName().isEmpty()) {
            return annotation.displayName();
        }
        // Convert camelCase to Title Case
        return toTitleCase(field.getName());
    }

    /**
     * Gets the description from annotation.
     */
    private String getDescription(InputField annotation) {
        if (annotation != null && !annotation.description().isEmpty()) {
            return annotation.description();
        }
        return null;
    }

    /**
     * Gets format hint from annotation or auto-detects from field type.
     */
    private String getFormatHint(Field field, InputField annotation) {
        if (annotation != null && !annotation.formatHint().isEmpty()) {
            return annotation.formatHint();
        }

        // Auto-detect common format hints
        String fieldName = field.getName().toLowerCase();
        if (fieldName.contains("email")) {
            return "email";
        } else if (fieldName.contains("url") || fieldName.contains("website")) {
            return "url";
        } else if (fieldName.contains("phone") || fieldName.contains("tel")) {
            return "tel";
        } else if (fieldName.contains("color")) {
            return "color";
        } else if (fieldName.contains("password")) {
            return "password";
        }

        return null;
    }

    /**
     * Detects the data type from Java field type.
     */
    private DataType detectDataType(Field field) {
        Class<?> type = field.getType();

        // Handle collections
        if (Collection.class.isAssignableFrom(type) || type.isArray()) {
            // For collections, we'd need generics inspection (simplified here)
            return DataType.STRING;
        }

        // String types
        if (type == String.class || type == Character.class || type == char.class) {
            return DataType.STRING;
        }

        // Numeric types
        if (type == Integer.class || type == int.class ||
            type == Long.class || type == long.class ||
            type == Double.class || type == double.class ||
            type == Float.class || type == float.class ||
            type == Short.class || type == short.class ||
            type == Byte.class || type == byte.class) {
            return DataType.NUMBER;
        }

        // Boolean types
        if (type == Boolean.class || type == boolean.class) {
            return DataType.BOOLEAN;
        }

        // Date/Time types
        if (Temporal.class.isAssignableFrom(type) ||
            type == Date.class ||
            type == LocalDate.class ||
            type == LocalDateTime.class) {
            return DataType.DATE;
        }

        // Enums default to STRING (with ValuesEndpoint)
        if (type.isEnum()) {
            return DataType.STRING;
        }

        // Default to STRING
        return DataType.STRING;
    }

    /**
     * Detects if field expects multiple values.
     */
    private boolean detectMultipleValues(Field field, InputField annotation) {
        if (annotation != null) {
            return annotation.expectMultipleValues();
        }

        Class<?> type = field.getType();
        return Collection.class.isAssignableFrom(type) || type.isArray();
    }

    /**
     * Detects if field is required from JPA and Bean Validation annotations.
     */
    private boolean detectRequired(Field field, InputField annotation) {
        if (annotation != null) {
            return annotation.required();
        }

        // Check @NotNull
        if (field.isAnnotationPresent(NotNull.class)) {
            return true;
        }

        // Check @NotBlank (implies required for strings)
        if (field.isAnnotationPresent(NotBlank.class)) {
            return true;
        }

        // Check @Column(nullable = false)
        Column column = field.getAnnotation(Column.class);
        if (column != null && !column.nullable()) {
            return true;
        }

        return false;
    }

    /**
     * Builds {@link ValuesEndpoint} from field metadata.
     */
    private ValuesEndpoint buildValuesEndpoint(Field field, InputField annotation) {
        // Check for enum type
        if (field.getType().isEnum()) {
            return buildEnumValuesEndpoint(field);
        }

        // Check for explicit values endpoint config
        if (annotation != null) {
            ValuesEndpointConfig config = annotation.valuesEndpoint();
            if (config != null && !config.uri().isEmpty()) {
                return buildRemoteValuesEndpoint(config);
            }
        }

        return null;
    }

    /**
     * Builds INLINE values endpoint for enum fields.
     */
    private ValuesEndpoint buildEnumValuesEndpoint(Field field) {
        Class<?> enumType = field.getType();
        Object[] enumConstants = enumType.getEnumConstants();

        List<ValueAlias> items = new ArrayList<>();
        for (Object constant : enumConstants) {
            items.add(new ValueAlias(
                constant.toString(),  // value
                toTitleCase(constant.toString())   // label
            ));
        }

        return ValuesEndpoint.builder()
            .protocol(ValuesEndpoint.Protocol.INLINE)
            .mode(ValuesEndpoint.Mode.CLOSED)
            .items(items)
            .build();
    }

    /**
     * Builds remote HTTP values endpoint from config.
     */
    private ValuesEndpoint buildRemoteValuesEndpoint(ValuesEndpointConfig config) {
        ValuesEndpoint.Builder builder = ValuesEndpoint.builder()
            .protocol(ValuesEndpoint.Protocol.HTTP)
            .uri(config.uri())
            .method("POST".equalsIgnoreCase(config.method()) ?
                ValuesEndpoint.HttpMethod.POST : ValuesEndpoint.HttpMethod.GET);

        // Set mode
        if ("SUGGESTIONS".equalsIgnoreCase(config.mode())) {
            builder.mode(ValuesEndpoint.Mode.SUGGESTIONS);
        } else {
            builder.mode(ValuesEndpoint.Mode.CLOSED);
        }

        // Set search parameters
        if (config.searchable()) {
            if (config.debounceMs() > 0) {
                builder.debounceMs(config.debounceMs());
            }
            if (config.minSearchLength() > 0) {
                builder.minSearchLength(config.minSearchLength());
            }
        }

        return builder.build();
    }

    /**
     * Extracts constraints from Bean Validation annotations.
     */
    private List<ConstraintDescriptor> extractConstraints(Field field) {
        List<ConstraintDescriptor> constraints = new ArrayList<>();

        // @Size
        Size size = field.getAnnotation(Size.class);
        if (size != null) {
            if (size.min() > 0) {
                constraints.add(ConstraintDescriptor.builder()
                    .name("minLength")
                    .type(ConstraintType.MIN_LENGTH)
                    .params(size.min())
                    .build());
            }
            if (size.max() < Integer.MAX_VALUE) {
                constraints.add(ConstraintDescriptor.builder()
                    .name("maxLength")
                    .type(ConstraintType.MAX_LENGTH)
                    .params(size.max())
                    .build());
            }
        }

        // @Min
        Min min = field.getAnnotation(Min.class);
        if (min != null) {
            constraints.add(ConstraintDescriptor.builder()
                .name("minValue")
                .type(ConstraintType.MIN_VALUE)
                .params(min.value())
                .build());
        }

        // @Max
        Max max = field.getAnnotation(Max.class);
        if (max != null) {
            constraints.add(ConstraintDescriptor.builder()
                .name("maxValue")
                .type(ConstraintType.MAX_VALUE)
                .params(max.value())
                .build());
        }

        // @Pattern
        Pattern pattern = field.getAnnotation(Pattern.class);
        if (pattern != null) {
            constraints.add(ConstraintDescriptor.builder()
                .name("pattern")
                .type(ConstraintType.PATTERN)
                .params(pattern.regexp())
                .errorMessage(pattern.message())
                .build());
        }

        // @Email
        if (field.isAnnotationPresent(Email.class)) {
            constraints.add(ConstraintDescriptor.builder()
                .name("email")
                .type(ConstraintType.PATTERN)
                .params("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
                .errorMessage("Must be a valid email address")
                .build());
        }

        return constraints;
    }

    /**
     * Converts camelCase to Title Case.
     */
    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        result.append(Character.toUpperCase(input.charAt(0)));

        for (int i = 1; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append(' ');
            }
            result.append(c);
        }

        return result.toString();
    }
}
