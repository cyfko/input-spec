---
sidebar_position: 1
id: hello-world
title: Hello World
---

# Hello World Form

Let's build your very first InputSpec form. We will create a simple "Contact Us" form, serve it via Spring Boot, and expose it via the REST API.

## 1. Add Dependencies

Add the InputSpec processor and starter to your `pom.xml`. The processor generates the JSON spec at compile time, and the starter serves it at runtime.

```xml
<dependencies>
    <!-- Server Runtime -->
    <dependency>
        <groupId>io.github.cyfko</groupId>
        <artifactId>input-spec-spring-boot-starter</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <!-- Compile-time generation -->
                    <path>
                        <groupId>io.github.cyfko</groupId>
                        <artifactId>input-spec-processor</artifactId>
                        <version>1.0.0-SNAPSHOT</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## 2. Define the Form

Create a pure Java POJO. We use `@FormSpec` to define the form, `@FieldMeta` to add human-readable contextual information, and standard Jakarta validation annotations (`@NotBlank`, `@Email`) to define business rules.

```java
import io.github.cyfko.inputspec.FormSpec;
import io.github.cyfko.inputspec.FieldMeta;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@FormSpec(
    id = "contact-form",
    displayName = "Contact Us",
    description = "Send us a message!",
    submitUri = "/api/forms/contact-form/submit"
)
public class ContactForm {

    @NotBlank
    @FieldMeta(displayName = "Your Name", description = "How should we call you?")
    private String name;

    @NotBlank
    @Email
    @FieldMeta(displayName = "Email Address")
    private String email;

    @NotBlank
    @Size(min = 10, max = 500)
    @FieldMeta(displayName = "Message", description = "Minimum 10 characters")
    private String message;

    // Getters and Setters...
}
```

When you compile your project (`mvn compile`), the annotation processor silently generates `META-INF/difsp/contact-form.json` containing the protocol representation of your form.

## 3. Handle the Submission

Now, let's tell Spring Boot what to do when a user submits this form. Because you are using `input-spec-spring-boot-starter`, you don't need to write a `@RestController` or handle JSON deserialization manually.

Just create a bean with a method annotated with `@FormHandler`, taking your `ContactForm` as a parameter!

```java
import io.github.cyfko.inputspec.spring.FormHandler;
import io.github.cyfko.inputspec.spring.SubmitResponse;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class ContactService {

    @FormHandler("contact-form")
    public SubmitResponse handleMessage(ContactForm form) {
        
        System.out.println("Received message from: " + form.getEmail());
        System.out.println("Message: " + form.getMessage());

        // Process the message (save to DB, send email, etc.)

        return SubmitResponse.ok(Map.of(
            "status", "Delivered",
            "thankYou", "Thanks " + form.getName() + ", we will be in touch!"
        ));
    }
}
```

**That's it!** You have a fully functioning form pipeline. 

## 4. Test the Endpoints

Start your Spring Boot application. The starter automatically exposes REST endpoints for everything.

**Discover the form:**
```bash
curl http://localhost:8080/api/forms
```
*Returns the list of available forms.*

**Get the JSON Spec (for your frontend):**
```bash
curl http://localhost:8080/api/forms/contact-form
```
*Returns the complete `contact-form.json` describing the fields and constraints.*

**Submit the form:**
```bash
curl -X POST http://localhost:8080/api/forms/contact-form/submit \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice", "email": "invalid-email", "message": "Hi!"}'
```
*Returns a `400 Bad Request` with structured validation errors because the email is invalid and the message is too short.*

InputSpec handles all the heavy lifting. Your frontend just renders the JSON, and your backend just handles pure, validated Java objects.
