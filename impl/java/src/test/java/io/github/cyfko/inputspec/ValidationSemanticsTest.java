package io.github.cyfko.inputspec;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cyfko.inputspec.model.*;
import io.github.cyfko.inputspec.validation.FieldValidator;
import io.github.cyfko.inputspec.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationSemanticsTest {

    private ConstraintDescriptor minLength(int v) {
        return ConstraintDescriptor.builder().name("minLength").type(ConstraintType.MIN_LENGTH).params(Map.of("value", v)).build();
    }
    private ConstraintDescriptor maxLength(int v) {
        return ConstraintDescriptor.builder().name("maxLength").type(ConstraintType.MAX_LENGTH).params(Map.of("value", v)).build();
    }
    private ConstraintDescriptor range(Number min, Number max, Number step) {
        Map<String,Object> params = step == null ? Map.of("min", min, "max", max) : Map.of("min", min, "max", max, "step", step);
        return ConstraintDescriptor.builder().name("range").type(ConstraintType.RANGE).params(params).build();
    }
    private ConstraintDescriptor pattern(String regex) {
        return ConstraintDescriptor.builder().name("pattern").type(ConstraintType.PATTERN).params(Map.of("regex", regex)).build();
    }

    @Test
    void arrayLengthConstraintsApplyToCollectionSize() {
        InputFieldSpec spec = InputFieldSpec.builder()
            .displayName("Tags")
            .dataType(DataType.STRING)
            .expectMultipleValues(true)
            .constraints(List.of(minLength(2), maxLength(4)))
            .build();
        FieldValidator validator = new FieldValidator();

        // size 3 OK
        ValidationResult ok = validator.validate(spec, List.of("a", "b", "c"));
        assertTrue(ok.isValid());

        // size 1 -> minLength error
        ValidationResult tooShort = validator.validate(spec, List.of("only"));
        assertFalse(tooShort.isValid());
        assertEquals(1, tooShort.getErrors().size());
        assertEquals("minLength", tooShort.getErrors().get(0).getConstraintName());

        // size 5 -> maxLength error
        ValidationResult tooLong = validator.validate(spec, List.of("a","b","c","d","e"));
        assertFalse(tooLong.isValid());
        assertEquals(1, tooLong.getErrors().size());
        assertEquals("maxLength", tooLong.getErrors().get(0).getConstraintName());
    }

    @Test
    void singleValueIgnoresLengthConstraints() {
        InputFieldSpec spec = InputFieldSpec.builder()
            .displayName("Code")
            .dataType(DataType.STRING)
            .expectMultipleValues(false)
            .constraints(List.of(minLength(5)))
            .build();
        FieldValidator validator = new FieldValidator();
        ValidationResult res = validator.validate(spec, "abc"); // shorter than 5 but should be ignored
        assertTrue(res.isValid());
    }

    @Test
    void rangeConstraintWithStep() {
        InputFieldSpec spec = InputFieldSpec.builder()
            .displayName("Even")
            .dataType(DataType.NUMBER)
            .expectMultipleValues(false)
            .constraints(List.of(range(0,10,2)))
            .build();
        FieldValidator validator = new FieldValidator();
        assertTrue(validator.validate(spec, 6).isValid());
        ValidationResult invalid = validator.validate(spec, 7);
        assertFalse(invalid.isValid());
        assertEquals("range", invalid.getErrors().get(0).getConstraintName());
    }

    @Test
    void shortCircuitStopsAfterFirstConstraintError() {
        InputFieldSpec spec = InputFieldSpec.builder()
            .displayName("Ref")
            .dataType(DataType.STRING)
            .expectMultipleValues(false)
            .constraints(List.of(
                pattern("^[0-9]+$"), // should fail for 'abc'
                pattern("^XYZ.*")    // would also fail but should be skipped when shortCircuit=true
            ))
            .build();
        FieldValidator validator = new FieldValidator();
        ValidationResult resNoShort = validator.validate(spec, "abc", false);
        assertFalse(resNoShort.isValid());
        assertEquals(2, resNoShort.getErrors().size());

        ValidationResult resShort = validator.validate(spec, "abc", true);
        assertFalse(resShort.isValid());
        assertEquals(1, resShort.getErrors().size());
        assertEquals("pattern", resShort.getErrors().get(0).getConstraintName());
    }

    @Test
    void inputSpecSerializationIncludesProtocolVersion() throws Exception {
        InputFieldSpec field = InputFieldSpec.builder()
            .displayName("Age")
            .dataType(DataType.NUMBER)
            .expectMultipleValues(false)
            .build();
        InputSpec spec = InputSpec.builder().addField(field).build();
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(spec);
        assertTrue(json.contains("\"protocolVersion\""));
        InputSpec back = mapper.readValue(json, InputSpec.class);
        assertEquals(InputSpec.CURRENT_PROTOCOL_VERSION, back.getProtocolVersion());
        assertEquals(1, back.getFields().size());
    }
}
