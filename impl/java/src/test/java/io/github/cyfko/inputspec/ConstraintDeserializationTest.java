package io.github.cyfko.inputspec;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cyfko.inputspec.model.ConstraintDescriptor;
import io.github.cyfko.inputspec.model.ConstraintType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConstraintDeserializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void unknownConstraintTypeMapsToUNKNOWN() throws Exception {
        String json = "{\n" +
                "  \"name\": \"futuristic\",\n" +
                "  \"type\": \"someFutureThing\",\n" +
                "  \"params\": { \"x\": 1 }\n" +
                "}";
        ConstraintDescriptor cd = mapper.readValue(json, ConstraintDescriptor.class);
        assertEquals("futuristic", cd.getName());
        assertEquals(ConstraintType.UNKNOWN, cd.getType());
        assertTrue(cd.getParams() instanceof Map);
    }

    @Test
    void customConstraintTypeStaysCUSTOM() throws Exception {
        String json = "{\n" +
                "  \"name\": \"scripted\",\n" +
                "  \"type\": \"custom\",\n" +
                "  \"params\": { \"script\": \"return true;\" }\n" +
                "}";
        ConstraintDescriptor cd = mapper.readValue(json, ConstraintDescriptor.class);
        assertEquals(ConstraintType.CUSTOM, cd.getType());
        assertTrue(cd.getParams() instanceof Map);
    }
}
