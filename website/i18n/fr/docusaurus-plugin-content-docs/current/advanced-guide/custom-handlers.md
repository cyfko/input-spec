---
sidebar_position: 2
id: custom-handlers
title: Handlers Personnalisés
---

# Handlers de Validation Personnalisés

InputSpec gère automatiquement les règles de format standard (e-mail, longueur, min/max) et les comparaisons entre champs. Mais que se passe-t-il si votre validation nécessite de vérifier une base de données, d'appeler une API externe ou d'appliquer une règle cryptographique propriétaire ?

Par exemple :
*   "Le nom d'utilisateur doit être unique dans la base de données."
*   "Le code promotionnel doit être actif dans Stripe."
*   "L'IBAN doit correspondre au code pays spécifié."

Ces règles ne peuvent pas être résolues statiquement par un simple interpréteur JSON. Elles nécessitent un contexte côté serveur. Pour cela, InputSpec vous permet d'enregistrer des **Handlers Personnalisés**.

## Définir des Contraintes Personnalisées en Java

Tout d'abord, définissez votre règle personnalisée en utilisant les modèles d'annotations standards de Jakarta Bean Validation.

```java
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UniqueUsernameValidator.class) // Validateur Jakarta standard
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueUsername {
    String message() default "Ce nom d'utilisateur est déjà pris";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

Et définissez le formulaire :

```java
public class RegistrationForm {
    @UniqueUsername
    private String username;
}
```

## La passerelle vers le JSON InputSpec

Lorsque le processeur InputSpec rencontre `@UniqueUsername`, il ne sait pas ce que c'est nativement. Il exporte alors par défaut une contrainte de type `CUSTOM` dans le fichier JSON :

```json
{
  "type": "CUSTOM",
  "namespace": "com.example.validators.UniqueUsername",
  "message": "Ce nom d'utilisateur est déjà pris"
}
```

Cependant, le frontend n'a aucune idée de la façon de valider `com.example.validators.UniqueUsername`.

## Enregistrer un Handler Backend

Pour sécuriser véritablement cette logique, lorsque le JSON est soumis au backend, le `FormSpecValidator` doit savoir comment exécuter la logique personnalisée.

Vous faites cela en enregistrant un callback ("handler") :

```java
import io.github.cyfko.inputspec.validation.FormSpecValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InputSpecConfig {

    private final UserRepository userRepository;

    public InputSpecConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public FormSpecValidator formSpecValidator() {
        FormSpecValidator validator = new FormSpecValidator();
        
        // Enregistre la logique spécifique pour notre namespace
        validator.registerCustomHandler("com.example.validators.UniqueUsername", (value, context) -> {
            if (value == null) return; // Laisser @NotNull s'en charger
            
            String username = (String) value;
            if (userRepository.existsByUsername(username)) {
                context.addError("username", "Ce nom d'utilisateur est déjà pris.");
            }
        });

        return validator;
    }
}
```

## Le Flux de Travail

1.  **Frontend** : Lit le JSON. Voit `type: "CUSTOM"`. Le frontend sait qu'il ne peut pas valider cela immédiatement. Il attend que l'utilisateur clique sur "Soumettre".
2.  **Soumission** : Le JSON arrive au backend via `/api/forms/registration`.
3.  **Validation Principale** : `FormSpecValidator` exécute les règles standards (longueur, e-mail, etc.).
4.  **Validation Personnalisée** : `FormSpecValidator` atteint la règle `CUSTOM`. Il cherche le lambda associé dans son registre en utilisant la chaîne `namespace`. Il exécute la requête en base de données.
5.  **Rejet** : Si le lambda ajoute une erreur au contexte, la requête est immédiatement annulée, renvoyant un `400 Bad Request` avec le message d'erreur précis sur `username`. Votre méthode `@FormHandler` *n'est jamais appelée*.

### Pourquoi est-ce préférable à la validation Spring habituelle ?

Dans une application Spring Boot classique, vous écririez un `@ConstraintValidator` standard. Cela fonctionne très bien.

Cependant, enregistrer explicitement le handler personnalisé avec InputSpec garantit que le pipeline de validation reste centralisé au sein du `FormSpecValidator`. Cela assure que vos soumissions de formulaires, qu'elles proviennent d'un navigateur web ou d'un **Agent IA (MCP)**, passent par exactement le même pipeline de règles rigoureux et prévisible avant d'instancier vos POJOs.
