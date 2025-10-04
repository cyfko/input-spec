package io.github.cyfko.inputspec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Protocol and HttpMethod enums and ValuesEndpoint defaults
 */
class ProtocolEnumTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("ValuesEndpoint should default to HTTPS protocol")
    void testDefaultProtocolIsHttps() {
        ResponseMapping mapping = ResponseMapping.builder("data").build();
        ValuesEndpoint endpoint = ValuesEndpoint.builder("/api/test", mapping).build();
        
        assertEquals(Protocol.HTTPS, endpoint.getProtocol());
    }

    @Test
    @DisplayName("ValuesEndpoint should default to GET method")
    void testDefaultMethodIsGet() {
        ResponseMapping mapping = ResponseMapping.builder("data").build();
        ValuesEndpoint endpoint = ValuesEndpoint.builder("/api/test", mapping).build();
        
        assertEquals(HttpMethod.GET, endpoint.getMethod());
    }

    @Test
    @DisplayName("Protocol enum should serialize correctly to JSON")
    void testProtocolEnumSerialization() throws JsonProcessingException {
        ResponseMapping mapping = ResponseMapping.builder("data").build();
        ValuesEndpoint endpoint = ValuesEndpoint.builder("/api/test", mapping)
                .protocol(Protocol.HTTP)
                .build();
        
        String json = objectMapper.writeValueAsString(endpoint);
        assertTrue(json.contains("\"protocol\":\"HTTP\""));
    }

    @Test
    @DisplayName("HttpMethod enum should serialize correctly to JSON")
    void testHttpMethodEnumSerialization() throws JsonProcessingException {
        ResponseMapping mapping = ResponseMapping.builder("data").build();
        ValuesEndpoint endpoint = ValuesEndpoint.builder("/api/test", mapping)
                .method(HttpMethod.POST)
                .build();
        
        String json = objectMapper.writeValueAsString(endpoint);
        assertTrue(json.contains("\"method\":\"POST\""));
    }

    @Test
    @DisplayName("ValuesEndpoint should deserialize protocol from JSON correctly")
    void testProtocolEnumDeserialization() throws JsonProcessingException {
        String json = "{\"uri\":\"/api/test\",\"protocol\":\"GRPC\",\"method\":\"GET\",\"responseMapping\":{\"dataField\":\"data\"}}";
        
        ValuesEndpoint endpoint = objectMapper.readValue(json, ValuesEndpoint.class);
        
        assertEquals(Protocol.GRPC, endpoint.getProtocol());
    }

    @Test
    @DisplayName("ValuesEndpoint should deserialize method from JSON correctly")
    void testHttpMethodEnumDeserialization() throws JsonProcessingException {
        String json = "{\"uri\":\"/api/test\",\"protocol\":\"HTTPS\",\"method\":\"POST\",\"responseMapping\":{\"dataField\":\"data\"}}";
        
        ValuesEndpoint endpoint = objectMapper.readValue(json, ValuesEndpoint.class);
        
        assertEquals(HttpMethod.POST, endpoint.getMethod());
    }

    @Test
    @DisplayName("Builder should allow setting all protocol values")
    void testBuilderWithAllProtocols() {
        ResponseMapping mapping = ResponseMapping.builder("data").build();
        
        ValuesEndpoint httpsEndpoint = ValuesEndpoint.builder("/api/test", mapping)
                .protocol(Protocol.HTTPS)
                .build();
        assertEquals(Protocol.HTTPS, httpsEndpoint.getProtocol());
        
        ValuesEndpoint httpEndpoint = ValuesEndpoint.builder("/api/test", mapping)
                .protocol(Protocol.HTTP)
                .build();
        assertEquals(Protocol.HTTP, httpEndpoint.getProtocol());
        
        ValuesEndpoint grpcEndpoint = ValuesEndpoint.builder("/api/test", mapping)
                .protocol(Protocol.GRPC)
                .build();
        assertEquals(Protocol.GRPC, grpcEndpoint.getProtocol());
    }

    @Test
    @DisplayName("Builder should allow setting all HTTP methods")
    void testBuilderWithAllHttpMethods() {
        ResponseMapping mapping = ResponseMapping.builder("data").build();
        
        ValuesEndpoint getEndpoint = ValuesEndpoint.builder("/api/test", mapping)
                .method(HttpMethod.GET)
                .build();
        assertEquals(HttpMethod.GET, getEndpoint.getMethod());
        
        ValuesEndpoint postEndpoint = ValuesEndpoint.builder("/api/test", mapping)
                .method(HttpMethod.POST)
                .build();
        assertEquals(HttpMethod.POST, postEndpoint.getMethod());
    }
}