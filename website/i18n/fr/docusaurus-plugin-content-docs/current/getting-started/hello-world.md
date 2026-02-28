---
sidebar_position: 1
id: hello-world
title: Hello World
---

# Formulaire Hello World

Construisons votre tout premier formulaire InputSpec. Nous allons créer un simple formulaire "Nous contacter", le servir via Spring Boot et l'exposer via l'API REST.

## 1. Ajouter les Dépendances

Ajoutez le processeur InputSpec et le starter à votre `pom.xml`. Le processeur génère la spec JSON à la compilation, et le starter la sert à l'exécution.

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
                    <!-- Génération à la compilation -->
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

## 2. Définir le Formulaire

Créez un simple POJO Java. Nous utilisons `@FormSpec` pour définir le formulaire, `@FieldMeta` pour ajouter des informations contextuelles lisibles, et les annotations de validation standard Jakarta (`@NotBlank`, `@Email`) pour définir les règles métier.

```java
import io.github.cyfko.inputspec.FormSpec;
import io.github.cyfko.inputspec.FieldMeta;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@FormSpec(
    id = "contact-form",
    displayName = "Nous contacter",
    description = "Envoyez-nous un message !",
    submitUri = "/api/forms/contact-form/submit"
)
public class ContactForm {

    @NotBlank
    @FieldMeta(displayName = "Votre Nom", description = "Comment devons-nous vous appeler ?")
    private String name;

    @NotBlank
    @Email
    @FieldMeta(displayName = "Adresse E-mail")
    private String email;

    @NotBlank
    @Size(min = 10, max = 500)
    @FieldMeta(displayName = "Message", description = "Minimum 10 caractères")
    private String message;

    // Getters et Setters...
}
```

Lorsque vous compilez votre projet (`mvn compile`), le processeur d'annotations génère silencieusement `META-INF/difsp/contact-form.json` contenant la représentation protocolaire de votre formulaire.

## 3. Gérer la Soumission

Maintenant, disons à Spring Boot quoi faire lorsqu'un utilisateur soumet ce formulaire. Grâce à `input-spec-spring-boot-starter`, vous n'avez pas besoin d'écrire un `@RestController` ou de gérer la désérialisation JSON manuellement.

Créez simplement un bean avec une méthode annotée de `@FormHandler`, prenant votre `ContactForm` en paramètre !

```java
import io.github.cyfko.inputspec.spring.FormHandler;
import io.github.cyfko.inputspec.spring.SubmitResponse;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class ContactService {

    @FormHandler("contact-form")
    public SubmitResponse handleMessage(ContactForm form) {
        
        System.out.println("Message reçu de : " + form.getEmail());
        System.out.println("Message : " + form.getMessage());

        // Traitement (sauvegarde BDD, envoi email, etc.)

        return SubmitResponse.ok(Map.of(
            "status", "Livré",
            "thankYou", "Merci " + form.getName() + ", nous vous recontacterons !"
        ));
    }
}
```

**Et voilà !** Vous avez un pipeline de formulaire complet.

## 4. Tester les Endpoints

Démarrez votre application Spring Boot. Le starter expose automatiquement les endpoints REST.

**Découvrir le formulaire :**
```bash
curl http://localhost:8080/api/forms
```
*Retourne la liste des formulaires disponibles.*

**Obtenir la Spec JSON (pour votre frontend) :**
```bash
curl http://localhost:8080/api/forms/contact-form
```
*Retourne le `contact-form.json` complet décrivant les champs et contraintes.*

**Soumettre le formulaire :**
```bash
curl -X POST http://localhost:8080/api/forms/contact-form/submit \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice", "email": "email-invalide", "message": "Salut !"}'
```
*Retourne une erreur `400 Bad Request` avec des erreurs de validation structurées car l'email est invalide et le message est trop court.*

InputSpec s'occupe de tout le travail fastidieux. Votre frontend ne fait que rendre le JSON, et votre backend manipule des objets Java purs et valides.
