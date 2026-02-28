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

Puisque InputSpec génère les schémas du protocole *à la compilation*, il ne peut pas exécuter automatiquement la logique générique d'un `@ConstraintValidator` Jakarta. Vous devez donc déclarer vos règles de validation personnalisées en utilisant une **Contrainte Croisée Personnalisée** (Custom Cross-Constraint), qui peut s'appliquer à un ou plusieurs champs.

```java
import io.github.cyfko.inputspec.FormSpec;
import io.github.cyfko.inputspec.CrossConstraint;
import io.github.cyfko.inputspec.protocol.CrossConstraintType;

@FormSpec(
    id = "registration"
)
@CrossConstraint(
    name = "uniqueUser",
    type = CrossConstraintType.CUSTOM,
    customKey = "checkUniqueUsername", // La clé de routage pour le backend
    fields = {"username"},
    errorMessage = "Ce nom d'utilisateur est déjà pris"
)
public class RegistrationForm {
    private String username;
}
```

## Le JSON Généré

Lorsque le processeur InputSpec rencontre cette annotation, il exporte une contrainte croisée `CUSTOM` dans le fichier JSON :

```json
{
  "name": "uniqueUser",
  "type": "custom",
  "fields": ["username"],
  "params": {
    "key": "checkUniqueUsername"
  },
  "errorMessage": "Ce nom d'utilisateur est déjà pris"
}
```

Le frontend sait qu'il ne peut pas valider une règle `custom` localement. Il attendra simplement que l'utilisateur clique sur "Soumettre" pour voir si le serveur la rejette.

## Enregistrer un Handler Backend

Pour sécuriser véritablement cette logique, lorsque le JSON est soumis au backend, le `FormSpecValidator` doit savoir comment exécuter la logique personnalisée.

Vous faites cela en enregistrant un callback qui correspond au `customKey` :

```java
import io.github.cyfko.inputspec.validation.FormSpecValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Optional;

@Configuration
public class InputSpecConfig {

    private final UserRepository userRepository;

    public InputSpecConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public FormSpecValidator formSpecValidator() {
        FormSpecValidator validator = new FormSpecValidator();
        
        // Enregistre la logique spécifique pour notre clé
        validator.registerCustomCrossHandler("checkUniqueUsername", (fieldValues, params) -> {
            Object value = fieldValues.get("username");
            if (value == null) return Optional.empty(); // Laisser @NotNull s'en charger
            
            String username = String.valueOf(value);
            if (userRepository.existsByUsername(username)) {
                return Optional.of("Ce nom d'utilisateur est déjà pris."); // Retourne un message d'erreur
            }
            return Optional.empty(); // Valide
        });

        return validator;
    }
}
```

## Le Flux de Travail

1.  **Frontend** : Lit le JSON. Voit `type: "CUSTOM"`. Le frontend sait qu'il ne peut pas valider cela immédiatement. Il attend que l'utilisateur clique sur "Soumettre".
2.  **Soumission** : Le JSON arrive au backend via `/api/forms/registration`.
3.  **Validation Principale** : `FormSpecValidator` exécute les règles standards (longueur, e-mail, etc.).
4.  **Validation Personnalisée** : `FormSpecValidator` atteint la règle `CUSTOM`. Il cherche le lambda associé dans son registre en utilisant la chaîne `customKey` (`checkUniqueUsername`). Il exécute la requête en base de données.
5.  **Rejet** : Si le lambda retourne `Optional.of("Erreur")`, la requête est immédiatement annulée, renvoyant un `400 Bad Request` avec le message d'erreur précis attaché à la contrainte `uniqueUser`. Votre méthode `@FormHandler` *n'est jamais appelée*.

### Pourquoi est-ce préférable à la validation Spring habituelle ?

Dans une application Spring Boot classique, vous écririez un `@ConstraintValidator` standard. Cela fonctionne très bien.

Cependant, enregistrer explicitement le handler personnalisé avec InputSpec garantit que le pipeline de validation reste centralisé au sein du `FormSpecValidator`. Cela assure que vos soumissions de formulaires, qu'elles proviennent d'un navigateur web ou d'un **Agent IA (MCP)**, passent par exactement le même pipeline de règles rigoureux et prévisible avant d'instancier vos POJOs.
