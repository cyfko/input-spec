package io.github.cyfko.inputspec.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import javax.tools.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for {@link FormSpecProcessor}.
 *
 * Compiles a sample {@code @FormSpec}-annotated source file using the Java
 * Compiler API with the processor on the annotation-processor path, then
 * validates the generated JSON and bundle skeleton.
 */
class FormSpecProcessorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Path outputDir;

    @BeforeEach
    void setUp() throws Exception {
        outputDir = Files.createTempDirectory("difsp-processor-test");
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up temp files
        try (var walk = Files.walk(outputDir)) {
            walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Full integration test
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Processes BookingForm and generates correct JSON + bundle")
    void processesBookingForm() throws Exception {
        // Compile the sample source
        boolean compiled = compileSample("BookingForm.java.txt");
        assertTrue(compiled, "Sample source must compile successfully");

        // ── Verify generated JSON ──────────────────────────────────────────
        Path jsonPath = outputDir.resolve("META-INF/difsp/booking-form.json");
        assertTrue(Files.exists(jsonPath), "JSON spec must be generated");

        JsonNode root = MAPPER.readTree(jsonPath.toFile());

        // Form-level
        assertEquals("booking-form", root.path("id").asText());

        // displayName is an i18n node
        JsonNode dn = root.path("displayName");
        assertEquals("Booking Form", dn.path("default").asText());
        assertEquals("booking-form.displayName", dn.path("i18nKey").asText());

        // Fields array
        JsonNode fields = root.path("fields");
        assertTrue(fields.isArray(), "fields must be an array");
        assertTrue(fields.size() >= 5, "Should have at least 5 fields");

        // ── assigneeId field ───────────────────────────────────────────────
        JsonNode assignee = findField(fields, "assigneeId");
        assertNotNull(assignee, "assigneeId field must exist");
        assertEquals("STRING", assignee.path("dataType").asText());
        assertTrue(assignee.path("required").asBoolean(), "assigneeId must be required (@NotNull)");

        // valuesEndpoint
        JsonNode ve = assignee.path("valuesEndpoint");
        assertFalse(ve.isMissingNode(), "assigneeId must have a valuesEndpoint");
        assertEquals("HTTPS", ve.path("protocol").asText());
        assertEquals("CLOSED", ve.path("mode").asText());
        assertEquals("/api/users", ve.path("uri").asText());
        assertEquals("PAGE_NUMBER", ve.path("paginationStrategy").asText());
        assertEquals("SESSION", ve.path("cacheStrategy").asText());

        // responseMapping
        assertEquals("data", ve.path("responseMapping").path("dataField").asText());
        assertEquals("total", ve.path("responseMapping").path("totalField").asText());

        // searchParamsSchema
        JsonNode schema = ve.path("searchParamsSchema");
        assertFalse(schema.isMissingNode(), "searchParamsSchema must be generated");
        assertEquals("object", schema.path("type").asText());
        assertTrue(schema.path("properties").has("name"),
            "searchParamsSchema must contain 'name' property");
        assertEquals("string", schema.path("properties").path("name").path("type").asText());

        // ── startDate field ────────────────────────────────────────────────
        JsonNode startDate = findField(fields, "startDate");
        assertNotNull(startDate, "startDate field must exist");
        assertEquals("DATE", startDate.path("dataType").asText());
        assertTrue(startDate.path("required").asBoolean());

        // Should have a minDate constraint (from @FutureOrPresent)
        JsonNode startConstraints = startDate.path("constraints");
        assertTrue(startConstraints.isArray());
        boolean hasMinDate = false;
        for (JsonNode c : startConstraints) {
            if ("minDate".equals(c.path("type").asText())) {
                assertEquals("$NOW", c.path("params").path("iso").asText());
                hasMinDate = true;
            }
        }
        assertTrue(hasMinDate, "startDate must have a minDate constraint from @FutureOrPresent");

        // ── roomType field (INLINE) ────────────────────────────────────────
        JsonNode roomType = findField(fields, "roomType");
        assertNotNull(roomType, "roomType field must exist");
        JsonNode roomVe = roomType.path("valuesEndpoint");
        assertEquals("INLINE", roomVe.path("protocol").asText());
        assertEquals("CLOSED", roomVe.path("mode").asText());

        JsonNode items = roomVe.path("items");
        assertTrue(items.isArray() && items.size() == 4, "Should have 4 inline items");
        assertEquals("SINGLE", items.get(0).path("value").asText());
        assertEquals("Single Room", items.get(0).path("label").path("default").asText());

        // ── guests field (NUMBER with @Min/@Max) ───────────────────────────
        JsonNode guests = findField(fields, "guests");
        assertNotNull(guests, "guests field must exist");
        assertEquals("NUMBER", guests.path("dataType").asText());

        JsonNode guestConstraints = guests.path("constraints");
        boolean hasMinValue = false, hasMaxValue = false;
        for (JsonNode c : guestConstraints) {
            if ("minValue".equals(c.path("type").asText())) {
                assertEquals(1, c.path("params").path("value").asInt());
                hasMinValue = true;
            }
            if ("maxValue".equals(c.path("type").asText())) {
                assertEquals(10, c.path("params").path("value").asInt());
                hasMaxValue = true;
            }
        }
        assertTrue(hasMinValue, "guests must have minValue constraint from @Min");
        assertTrue(hasMaxValue, "guests must have maxValue constraint from @Max");

        // ── promoCode field (@Pattern + @Size) ─────────────────────────────
        JsonNode promoCode = findField(fields, "promoCode");
        assertNotNull(promoCode, "promoCode field must exist");
        JsonNode promoConstraints = promoCode.path("constraints");
        boolean hasPattern = false, hasMaxLength = false;
        for (JsonNode c : promoConstraints) {
            if ("pattern".equals(c.path("type").asText())) {
                assertFalse(c.path("params").path("regex").asText().isEmpty());
                hasPattern = true;
            }
            if ("maxLength".equals(c.path("type").asText())) hasMaxLength = true;
        }
        assertTrue(hasPattern, "promoCode must have pattern constraint from @Pattern");
        assertTrue(hasMaxLength, "promoCode must have maxLength constraint from @Size(max=20)");

        // ── CrossConstraints ───────────────────────────────────────────────
        JsonNode crossConstraints = root.path("crossConstraints");
        assertTrue(crossConstraints.isArray() && crossConstraints.size() == 2,
            "Should have 2 cross-constraints");

        JsonNode dateRange = findByName(crossConstraints, "dateRange");
        assertNotNull(dateRange, "dateRange cross-constraint must exist");
        assertEquals("fieldComparison", dateRange.path("type").asText());
        assertEquals("gt", dateRange.path("params").path("operator").asText());
        assertEquals("endDate", dateRange.path("fields").get(0).asText());
        assertEquals("startDate", dateRange.path("fields").get(1).asText());

        JsonNode oneDiscount = findByName(crossConstraints, "oneDiscountOnly");
        assertNotNull(oneDiscount, "oneDiscountOnly cross-constraint must exist");
        assertEquals("mutuallyExclusive", oneDiscount.path("type").asText());
        assertEquals(1, oneDiscount.path("params").path("max").asInt());

        // ── submitEndpoint ─────────────────────────────────────────────────
        JsonNode submit = root.path("submitEndpoint");
        assertFalse(submit.isMissingNode(), "submitEndpoint must be present");
        assertEquals("/api/bookings", submit.path("uri").asText());
        assertEquals("POST", submit.path("method").asText());

        // ── Verify bundle skeleton ─────────────────────────────────────────
        Path bundlePath = outputDir.resolve("META-INF/difsp/i18n/booking-form.properties");
        assertTrue(Files.exists(bundlePath), "Bundle skeleton must be generated");

        String bundleContent = Files.readString(bundlePath, StandardCharsets.UTF_8);
        assertTrue(bundleContent.contains("booking-form.displayName = Booking Form"));
        assertTrue(bundleContent.contains("booking-form.fields.assigneeId.displayName"));
        assertTrue(bundleContent.contains("booking-form.fields.roomType.displayName"));
        assertTrue(bundleContent.contains("booking-form.crossConstraints.dateRange.errorMessage"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Compiler helper
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Compiles a sample source file from the test resources directory using
     * the Java Compiler API with our processor on the annotation-processor path.
     */
    private boolean compileSample(String resourceName) throws Exception {
        // Read the sample source from test resources
        URL resource = getClass().getClassLoader().getResource("samples/" + resourceName);
        assertNotNull(resource, "Test resource samples/" + resourceName + " must exist");
        String source = Files.readString(Path.of(resource.toURI()), StandardCharsets.UTF_8);

        // Write to a temp .java file (compiler needs .java extension)
        String className = resourceName.replace(".java.txt", "");
        Path sourceDir = outputDir.resolve("sources");
        Path packageDir = sourceDir.resolve("io/github/cyfko/inputspec/samples");
        Files.createDirectories(packageDir);
        Path sourceFile = packageDir.resolve(className + ".java");
        Files.writeString(sourceFile, source);

        // Set up the compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "System Java compiler must be available");

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager =
                compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {

            // Classpath: include the API jar and processor jar
            String classpath = System.getProperty("java.class.path");

            List<String> options = List.of(
                "-d", outputDir.toString(),
                "-s", outputDir.toString(),
                "-classpath", classpath,
                "-processor", "io.github.cyfko.inputspec.processor.FormSpecProcessor"
            );

            Iterable<? extends JavaFileObject> compilationUnits =
                fileManager.getJavaFileObjects(sourceFile.toFile());

            JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics, options, null, compilationUnits);

            boolean success = task.call();

            // Print diagnostics for debugging
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) {
                    System.err.println("COMPILE ERROR: " + d.getMessage(null));
                } else {
                    System.out.println("COMPILE NOTE: " + d.getMessage(null));
                }
            }

            return success;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  JSON helpers
    // ═══════════════════════════════════════════════════════════════════════════

    private JsonNode findField(JsonNode fieldsArray, String name) {
        for (JsonNode f : fieldsArray) {
            if (name.equals(f.path("name").asText())) return f;
        }
        return null;
    }

    private JsonNode findByName(JsonNode array, String name) {
        for (JsonNode n : array) {
            if (name.equals(n.path("name").asText())) return n;
        }
        return null;
    }
}
