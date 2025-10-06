package io.github.cyfko.inputspec;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cyfko.inputspec.model.*;
import io.github.cyfko.inputspec.validation.FieldValidator;
import io.github.cyfko.inputspec.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConstraintValidationTest {
    private final FieldValidator validator = new FieldValidator();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void patternFailureSingle() {
        var spec = InputFieldSpec.builder()
                .displayName("Username")
                .dataType(DataType.STRING)
                .expectMultipleValues(false)
                .required(true)
                .constraints(List.of(
                        ConstraintDescriptor.builder().name("syntax").type(ConstraintType.PATTERN)
                                .params(Map.of("regex", "^[A-Z]+$"))
                                .errorMessage("uppercase")
                                .build()))
                .build();
        ValidationResult res = validator.validate(spec, "abc");
        assertFalse(res.isValid());
        assertEquals("syntax", res.getErrors().get(0).getConstraintName());
    }

    @Test
    void minMaxLength() {
        var spec = InputFieldSpec.builder()
                .displayName("Code")
                .dataType(DataType.STRING)
                .expectMultipleValues(false)
                .required(true)
                .constraints(List.of(
                        ConstraintDescriptor.builder().name("minL").type(ConstraintType.MIN_LENGTH).params(Map.of("value", 3)).build(),
                        ConstraintDescriptor.builder().name("maxL").type(ConstraintType.MAX_LENGTH).params(Map.of("value", 5)).build()
                )).build();
        assertFalse(validator.validate(spec, "ab").isValid());
        assertTrue(validator.validate(spec, "abc").isValid());
        assertTrue(validator.validate(spec, "abcd").isValid());
        assertFalse(validator.validate(spec, "abcdef").isValid());
    }

    @Test
    void rangeNumber() {
        var spec = InputFieldSpec.builder()
                .displayName("Temp")
                .dataType(DataType.NUMBER)
                .expectMultipleValues(false)
                .required(true)
                .constraints(List.of(
                        ConstraintDescriptor.builder().name("operational").type(ConstraintType.RANGE)
                                .params(Map.of("min", 0, "max", 10)).build()))
                .build();
        assertFalse(validator.validate(spec, -1).isValid());
        assertTrue(validator.validate(spec, 5).isValid());
        assertFalse(validator.validate(spec, 11).isValid());
    }

    @Test
    void dateBounds() {
        var spec = InputFieldSpec.builder()
                .displayName("Created")
                .dataType(DataType.DATE)
                .expectMultipleValues(false)
                .required(false)
                .constraints(List.of(
                        ConstraintDescriptor.builder().name("after").type(ConstraintType.MIN_DATE).params(Map.of("iso", "2024-01-01T00:00:00Z")).build(),
                        ConstraintDescriptor.builder().name("before").type(ConstraintType.MAX_DATE).params(Map.of("iso", "2025-12-31T23:59:59Z")).build()
                )).build();
        assertFalse(validator.validate(spec, "2023-12-31T23:59:59Z").isValid());
        assertTrue(validator.validate(spec, "2024-06-01T00:00:00Z").isValid());
        assertFalse(validator.validate(spec, "2026-01-01T00:00:00Z").isValid());
    }

    @Test
    void closedDomainMembershipMulti() {
        var endpoint = ValuesEndpoint.builder()
                .protocol(ValuesEndpoint.Protocol.INLINE)
                .mode(ValuesEndpoint.Mode.CLOSED)
                .items(List.of(
                        ValueAlias.builder().value("A").label("A").build(),
                        ValueAlias.builder().value("B").label("B").build()
                )).build();
        var spec = InputFieldSpec.builder()
                .displayName("Tags")
                .dataType(DataType.STRING)
                .expectMultipleValues(true)
                .required(true)
                .valuesEndpoint(endpoint)
                .constraints(List.of())
                .build();
        var res = validator.validate(spec, List.of("A", "X", "B", "Z"));
        assertFalse(res.isValid());
        long membershipErrors = res.getErrors().stream().filter(e -> e.getConstraintName().equals("membership")).count();
        assertEquals(2, membershipErrors);
    }

    @Test
    void suggestionsModeIgnoresMembership() {
        var endpoint = ValuesEndpoint.builder()
                .protocol(ValuesEndpoint.Protocol.INLINE)
                .mode(ValuesEndpoint.Mode.SUGGESTIONS)
                .items(List.of(ValueAlias.builder().value("A").label("A").build()))
                .build();
        var spec = InputFieldSpec.builder()
                .displayName("Country")
                .dataType(DataType.STRING)
                .expectMultipleValues(false)
                .required(false)
                .valuesEndpoint(endpoint)
                .constraints(List.of())
                .build();
        var res = validator.validate(spec, "XYZ");
        assertTrue(res.isValid());
    }
}
