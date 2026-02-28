package io.github.cyfko.inputspec.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * InputSpec MCP Demo Application.
 *
 * <p>A hotel booking system that demonstrates:</p>
 * <ul>
 *   <li>Defining forms with {@code @FormSpec} annotations</li>
 *   <li>Processing submissions with {@code @FormHandler}</li>
 *   <li>Exposing forms to AI agents via MCP tools</li>
 * </ul>
 *
 * <p><b>Run:</b> {@code mvn spring-boot:run}</p>
 * <p><b>REST:</b> {@code GET http://localhost:8080/api/forms}</p>
 * <p><b>MCP:</b> Connect any MCP client to {@code http://localhost:8080/mcp}</p>
 */
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
