package io.github.cyfko.inputspec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cyfko.inputspec.model.ConstraintDescriptor;
import io.github.cyfko.inputspec.model.ConstraintType;
import io.github.cyfko.inputspec.model.DataType;
import io.github.cyfko.inputspec.model.InputFieldSpec;
import io.github.cyfko.inputspec.model.ValuesEndpoint;
import io.github.cyfko.inputspec.validation.FieldValidator;
import io.github.cyfko.inputspec.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ModelSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void roundTripInputFieldSpec() throws JsonProcessingException {
    InputFieldSpec spec = InputFieldSpec.builder()
        .displayName("Age")
        .dataType(DataType.NUMBER)
        .expectMultipleValues(false)
        .required(true)
        .constraints(List.of(
            ConstraintDescriptor.builder().name("range1").type(ConstraintType.RANGE).params(Map.of("min", 0, "max", 120)).errorMessage("0-120").build()
        ))
        .build();

        String json = mapper.writeValueAsString(spec);
        InputFieldSpec back = mapper.readValue(json, InputFieldSpec.class);

    assertEquals("Age", back.getDisplayName());
    assertEquals(DataType.NUMBER, back.getDataType());
    assertEquals(1, back.getConstraints().size());
    assertEquals("RANGE", back.getConstraints().get(0).getType().name());
    }

    @Test
    void validatorRequiredConstraint() {
    InputFieldSpec spec = InputFieldSpec.builder()
        .displayName("Username")
        .dataType(DataType.STRING)
        .expectMultipleValues(false)
        .required(true)
        .constraints(List.of())
        .build();

        FieldValidator validator = new FieldValidator();
        ValidationResult result = validator.validate(spec, null);
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("required", result.getErrors().get(0).getConstraintName());
    }
}
