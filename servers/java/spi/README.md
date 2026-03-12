# inputspec-spi

Extension point for the InputSpec annotation processor.

Allows third-party libraries to plug into the `FormSpec` generation pipeline
at compile time — without modifying InputSpec itself and without creating any
coupling between InputSpec and the extending library.

---

## The problem this solves

The default InputSpec processor maps Java fields (or interface methods) to DIFSP
`InputFieldSpec` objects using a straightforward type-driven strategy. This works
perfectly for standard forms.

Some domains, however, require a richer field representation. A search form backed
by a query protocol may need each filterable field expressed as a structured object
with a constrained operator sub-field and a typed value sub-field. A date range
picker may need a two-value array field instead of a scalar. A workflow engine may
need conditional fields that appear only when another field has a specific value.

None of these cases can be expressed by annotating a Java field alone — they require
domain knowledge that InputSpec does not have and should not have.

The SPI lets the library that *does* have that domain knowledge inject its own
generation logic directly into the processor pipeline.

---

## Architecture

```
inputspec-spi  (this module)
    │
    ├── FieldTransformer        SPI interface — field-level hook
    ├── FormContributor         SPI interface — form-level hook
    │
    ├── FieldContext            Read-only view of a field being processed
    ├── FormContext             Read-only view of the assembled form
    │
    ├── DefaultFieldContext     Implementation built by the processor
    └── DefaultFormContext      Implementation built by the processor
```

The processor (`inputspec-processor`) loads all registered implementations via
`ServiceLoader` at the start of each compilation. The SPI module itself has no
runtime footprint — it is used exclusively during annotation processing.

---

## Two extension points

### `FieldTransformer` — field-level

Called once per `@FieldMeta`-annotated element (field or method). Decides whether
to take ownership of that element and, if so, produces the complete
`InputFieldSpec` JSON string for it.

```java
public interface FieldTransformer {

    /** Return true to claim this element. */
    boolean supports(FieldContext context);

    /** The stable ref name for this field (used in FormContext and in the JSON "name"). */
    String fieldRefName(FieldContext context);

    /** Produce the complete InputFieldSpec JSON string. */
    String transform(FieldContext context);
}
```

When `supports()` returns `true`, the processor skips its own default generation
for that element entirely. The returned JSON is written verbatim into the
`fields` array of the FormSpec.

If no transformer claims an element, the default InputSpec pipeline runs as usual.
Existing forms are therefore completely unaffected by the presence of a transformer
on the classpath.

### `FormContributor` — form-level

Called once per `@FormSpec` type, after all fields have been processed. Contributes
additional `InputFieldSpec` and `CrossConstraintDescriptor` JSON objects that do
not correspond to any individual annotated element — they are derived from the set
of all processed fields.

```java
public interface FormContributor {

    /** Return true to activate for this form. */
    boolean supports(FormContext context);

    /** Additional InputFieldSpec JSON objects to append to the fields array. */
    List<String> additionalFields(FormContext context);

    /** Additional CrossConstraintDescriptor JSON objects. */
    List<String> additionalCrossConstraints(FormContext context);
}
```

A contributor typically checks `FormContext.transformedFieldRefs()` to determine
whether its companion `FieldTransformer` processed any fields — and activates only
if it did.

---

## `FieldContext` — what a transformer can see

| Method | Description |
|---|---|
| `element()` | The raw `Element` (field or method) |
| `fieldName()` | Logical camelCase name (`getName()` → `"name"`) |
| `fieldType()` | Declared `TypeMirror` |
| `fieldMeta()` | The `@FieldMeta` mirror, if present |
| `findAnnotation(fqn)` | Finds any annotation by its qualified name |
| `hasAnnotation(fqn)` | Returns true if the annotation is present |
| `annotationStringValue(mirror, attr)` | Reads a String attribute |
| `annotationStringList(mirror, attr)` | Reads a String[] attribute |
| `annotationEnumList(mirror, attr)` | Reads an enum[] attribute as `List<String>` |
| `annotationMirrorList(mirror, attr)` | Reads a nested annotation[] attribute |
| `isEnum()` | True if the field type is a Java enum |
| `enumConstants()` | Enum constant names in declaration order |
| `isMultiValued()` | True for collections and arrays |
| `difspDataType()` | DIFSP type string: `STRING`, `NUMBER`, `DATE`, `BOOLEAN`, `OBJECT` |
| `formatHint()` | `"iso8601"` for date types, empty otherwise |
| `formId()` | The form ID declared in `@FormSpec` |
| `locales()` | Active locales for this form |

The key design decision: all annotation access goes through `findAnnotation(fqn)`
using qualified name strings. A transformer therefore has **zero compile-time
dependency** on the annotations it bridges. The SPI module itself is the only
compile dependency needed.

---

## `FormContext` — what a contributor can see

| Method | Description |
|---|---|
| `formId()` | The form ID |
| `submitUri()` | The submit endpoint URI |
| `submitMethod()` | `POST` or `PUT` |
| `locales()` | Active locales |
| `transformedFieldRefs()` | Ref names of all transformer-claimed fields |
| `allFieldNames()` | Logical names of all fields (transformed + default) |
| `transformedFieldJson(ref)` | The generated JSON for a specific transformed field |
| `defaultPageSize()` | Pagination hint (default: 20) |
| `maxPageSize()` | Max page size (default: 100) |

---

## Writing an extension

### 1. Add the dependency

```xml
<dependency>
    <groupId>io.github.cyfko</groupId>
    <artifactId>inputspec-spi</artifactId>
    <version>2.1.0</version>
    <!-- scope provided: used at compile time only, never at runtime -->
    <scope>provided</scope>
</dependency>
```

### 2. Implement the interfaces

```java
public class MyFieldTransformer implements FieldTransformer {

    // Activate when the element carries the annotation you care about
    private static final String MY_ANNOTATION =
            "com.example.myannotation.MyAnnotation";

    @Override
    public boolean supports(FieldContext ctx) {
        return ctx.hasAnnotation(MY_ANNOTATION);
    }

    @Override
    public String fieldRefName(FieldContext ctx) {
        return ctx.findAnnotation(MY_ANNOTATION)
                .flatMap(m -> ctx.annotationStringValue(m, "value"))
                .filter(s -> !s.isBlank())
                .orElse(ctx.fieldName().toUpperCase());
    }

    @Override
    public String transform(FieldContext ctx) {
        // Build and return a valid DIFSP InputFieldSpec JSON string.
        // ctx.difspDataType(), ctx.isEnum(), ctx.enumConstants(), ctx.fieldMeta()
        // are all available to inform the generation.
        return "{ \"name\": \"" + fieldRefName(ctx) + "\", ... }";
    }
}

public class MyFormContributor implements FormContributor {

    @Override
    public boolean supports(FormContext ctx) {
        return !ctx.transformedFieldRefs().isEmpty();
    }

    @Override
    public List<String> additionalFields(FormContext ctx) {
        // Return synthetic fields derived from the set of transformed field refs.
        return List.of( /* ... */ );
    }

    @Override
    public List<String> additionalCrossConstraints(FormContext ctx) {
        return List.of( /* ... */ );
    }
}
```

### 3. Register via `META-INF/services`

```
src/main/resources/META-INF/services/
├── io.github.cyfko.inputspec.spi.FieldTransformer
│     com.example.mylib.MyFieldTransformer
└── io.github.cyfko.inputspec.spi.FormContributor
      com.example.mylib.MyFormContributor
```

### 4. Declare on the `annotationProcessorPath`

The extension JAR must be on the processor classpath — not in `<dependencies>`.
The `ServiceLoader` inside the InputSpec processor uses its own classloader, which
only sees what is declared in `annotationProcessorPaths`.

```xml
<build>
  <plugins>
    <plugin>
      <artifactId>maven-compiler-plugin</artifactId>
      <configuration>
        <annotationProcessorPaths>
          <!-- InputSpec processor — the one that loads the SPI -->
          <path>
            <groupId>io.github.cyfko</groupId>
            <artifactId>inputspec-processor</artifactId>
            <version>2.1.0</version>
          </path>
          <!-- Your extension — not a processor itself, just a JAR with SPI impls -->
          <path>
            <groupId>com.example</groupId>
            <artifactId>my-inputspec-extension</artifactId>
            <version>1.0.0</version>
          </path>
        </annotationProcessorPaths>
      </configuration>
    </plugin>
  </plugins>
</build>
```

> **Note**: the extension JAR does not need to declare
> `META-INF/services/javax.annotation.processing.Processor`. It is not an
> annotation processor — it is discovered by the InputSpec processor via
> `ServiceLoader`. No `@SupportedAnnotationTypes`, no `AbstractProcessor`.

---

## Isolation guarantees

The SPI is designed so that neither InputSpec nor any extension needs to know about
the other at compile time.

**InputSpec** does not import anything from the extension. It only knows the two SPI
interfaces and calls them via `ServiceLoader`.

**The extension** does not import InputSpec annotation classes. It accesses all
annotations (including `@FieldMeta`) through `FieldContext.findAnnotation(fqn)` using
qualified name strings. Its only compile dependency is `inputspec-spi` itself.

**User code** does not change. Existing `@FormSpec` classes are unaffected. An
extension only activates for elements where `supports()` returns `true`.

---

## Precedence and ordering

- If two transformers both return `true` for the same element, the first one in
  `ServiceLoader` order wins. A `WARNING` diagnostic is emitted at compile time
  naming the conflict.
- Multiple `FormContributor` implementations may activate for the same form. Their
  contributions are appended in `ServiceLoader` order.
- The `ServiceLoader` order is determined by the order of entries in the
  `META-INF/services` file and the order of JARs on the `annotationProcessorPath`.

---

## Compile-time diagnostics

The processor emits the following messages related to SPI activity:

| Kind | Condition |
|---|---|
| `NOTE` | At startup: lists the number of loaded `FieldTransformer`s and `FormContributor`s and their class names |
| `WARNING` | Two transformers claim the same element — first one wins |
| `ERROR` | A transformer or contributor returned invalid JSON |

---

## Module structure

```
inputspec-spi/
└── src/main/java/io/github/cyfko/inputspec/spi/
    ├── FieldTransformer.java       SPI interface
    ├── FormContributor.java        SPI interface
    ├── FieldContext.java           Read-only field view (interface)
    ├── FormContext.java            Read-only form view (interface)
    ├── DefaultFieldContext.java    Implementation (for processor internals)
    └── DefaultFormContext.java     Implementation + Builder (for processor internals)
```

`DefaultFieldContext` and `DefaultFormContext` are part of this module because the
SPI interfaces reference them in their contracts. Extension implementors do not
instantiate them directly — they receive them as `FieldContext` / `FormContext`
arguments from the processor.
