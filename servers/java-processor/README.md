<div align="center">
  <img src="https://raw.githubusercontent.com/cyfko/input-spec/main/website/static/img/logo.png" alt="InputSpec Logo" width="120" />
</div>

# InputSpec Processor

The compile-time Annotation Processor for the InputSpec Java ecosystem.

This module hooks directly into the `javac` compiler API to parse classes annotated with `@FormSpec` and `@FieldMeta`, transforming your backend POJOs and Jakarta Validation annotations into statically generated Dynamic Input Field Specification Protocol (DIFSP) JSON files.

## 🚀 How it Works

When you compile your Java project, the `input-spec-processor` automatically:
1. Discovers classes marked with `@FormSpec`.
2. Inspects their fields, evaluating Java Enums and standard Jakarta validation annotations (`@NotNull`, `@Pattern`, `@Min`, `@Max`, `@Size`).
3. Generates a strict `[form-id].json` file containing the form's layout and rules.
4. Generates a boilerplate `[form-id].properties` skeleton file in `META-INF/difsp/i18n` with deterministic localization keys for your frontend to translate.

By doing this at compile-time, we eliminate the need for heavy runtime reflection to figure out form schemas, and ensure that your JSON representations are always 100% physically synchronized with your Java deployment.

## 📦 Installation

This dependency should only run during the compilation phase, not at runtime. If you are using Maven, add it to your `annotationProcessorPaths` via the compiler plugin:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <annotationProcessorPaths>
            <!-- Other processors like Lombok go here -->
            <path>
                <groupId>io.github.cyfko</groupId>
                <artifactId>input-spec-processor</artifactId>
                <version>1.0.0-SNAPSHOT</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

*(If you are using Gradle, use the `annotationProcessor` configuration).*

## 🔗 Documentation

For comprehensive guides, installation instructions, and advanced examples, please visit the [official documentation](https://cyfko.github.io/input-spec/).
