package io.github.cyfko.inputspec;

import io.github.cyfko.inputspec.model.*;
import io.github.cyfko.inputspec.model.ConstraintType;
import io.github.cyfko.inputspec.validation.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationOptionsTest {

    @Test
    void httpMethodDefaultsToGet() {
        ValuesEndpoint ve = ValuesEndpoint.builder()
            .protocol(ValuesEndpoint.Protocol.INLINE)
            .mode(ValuesEndpoint.Mode.CLOSED)
            .items(List.of(new ValueAlias("A","A")))
            .build();
        assertEquals(ValuesEndpoint.HttpMethod.GET, ve.getMethod());
    }

    @Test
    void unknownConstraintIgnored() {
        // Simule une contrainte inconnue : on force UNKNOWN et un params arbitraire
        ConstraintDescriptor unknown = ConstraintDescriptor.builder()
            .name("futuristic")
            .type(ConstraintType.UNKNOWN)
            .params(Map.of("x",1))
            .build();
        InputFieldSpec spec = InputFieldSpec.builder()
            .displayName("Field")
            .dataType(DataType.STRING)
            .expectMultipleValues(false)
            .constraints(List.of(unknown))
            .build();
        FieldValidator validator = new FieldValidator();
        ValidationResult res = validator.validate(spec, "abc");
        assertTrue(res.isValid());
    }

    @Test
    void customConstraintIgnored() {
        ConstraintDescriptor custom = ConstraintDescriptor.builder()
            .name("custom1")
            .type(ConstraintType.CUSTOM)
            .params(Map.of("script","return true;"))
            .build();
        InputFieldSpec spec = InputFieldSpec.builder()
            .displayName("Field")
            .dataType(DataType.NUMBER)
            .expectMultipleValues(false)
            .constraints(List.of(custom))
            .build();
        FieldValidator validator = new FieldValidator();
        ValidationResult res = validator.validate(spec, 42);
        assertTrue(res.isValid());
    }

    @Test
    void validationOptionsShortCircuit() {
        InputFieldSpec spec = InputFieldSpec.builder()
            .displayName("Ref")
            .dataType(DataType.STRING)
            .expectMultipleValues(false)
            .constraints(List.of(
                ConstraintDescriptor.builder().name("pattern1").type(ConstraintType.PATTERN).params(Map.of("regex","^[0-9]+$")) .build(),
                ConstraintDescriptor.builder().name("pattern2").type(ConstraintType.PATTERN).params(Map.of("regex","^XYZ.*")) .build()
            ))
            .build();
        FieldValidator validator = new FieldValidator();
        ValidationResult res = validator.validate(spec, "abc", ValidationOptions.shortCircuit());
        assertFalse(res.isValid());
        assertEquals(1, res.getErrors().size());
    }
}
