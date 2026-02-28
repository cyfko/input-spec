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

## Enregistrer un Handler Backend (Spring Boot)

Pour exécuter cette logique de validation sur le backend, il vous suffit de définir un composant Spring et d'annoter une méthode avec `@FormValidator`, en passant la propriété `customKey` comme valeur.

> [!TIP]
> La méthode doit accepter en paramètre votre POJO annoté `@FormSpec`. Retournez `Optional<String>` pour indiquer un message d'erreur. Si l'Optional est vide, la validation est considérée comme un succès.

```java
import io.github.cyfko.inputspec.validation.FormValidator;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserValidationService {

    private final UserRepository userRepository;

    public UserValidationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Correspond au customKey déclaré dans la @CrossConstraint
    @FormValidator("checkUniqueUsername")
    public Optional<String> validateConstraint(RegistrationForm form) {
        if (form.getUsername() == null) {
            return Optional.empty(); // Laisser le @NotNull standard gérer l'absence de valeur
        }
        
        if (userRepository.existsByUsername(form.getUsername())) {
            return Optional.of("Ce nom d'utilisateur est déjà pris."); // Message d'erreur
        }
        
        return Optional.empty(); // Valide
    }
}
```

## Validation Globale de Formulaire

Parfois, les règles de validation sont tellement complexes ou transversales qu'elles ne s'appliquent pas à des champs spécifiques en particulier, mais au payload global du formulaire dans son ensemble. Vous pouvez exécuter une logique métier globale en enregistrant un validateur au niveau du formulaire.

Pour le faire, utilisez `@FormValidator` mais fournissez l'**ID du Formulaire** (Form ID) comme valeur, et retournez une `Map<String, String>` où les clés représentent les champs spécifiques causant une erreur, et les valeurs représentent leurs messages.

```java
@Service
public class BookingValidationService {

    // Correspond à l'ID du formulaire défini : @FormSpec(id = "booking-form")
    @FormValidator("booking-form")
    public Map<String, String> validateEntireForm(BookingForm form) {
        Map<String, String> errors = new HashMap<>();

        // Exécute des appels API complexes ou des calculs impliquant plusieurs champs
        if (form.getDiscountCode() != null && !isEligible(form.getUserId(), form.getDiscountCode())) {
            errors.put("discountCode", "Vous n'êtes pas éligible à ce code promotionnel.");
        }

        return errors; // Retourne une Map vide si le formulaire est valide
    }
}
```

## Le Pipeline d'Exécution en 3 Phases

InputSpec impose un pipeline d'exécution très strict pour empêcher vos API complexes d'être appelées sur des données massivement invalides. Vos méthodes ne sont invoquées que lorsqu'il est complètement sûr de le faire.

1.  **Phase 1 (Validation Standard)** : Exécute les vérifications fondamentales (longueur, réquis, comparaisons croisées standards). Elle échoue immédiatement si une de ces règles basiques est brisée (fail-fast).
2.  **Phase 2 (Validation de Contraintes Custom)** : Seulement si la Phase 1 est un succès éclatant, elle évalue vos contraintes croisées de type `CUSTOM` (méthodes renvoyant `Optional<String>`). Elle collecte toutes les erreurs détectées et avorte si elle en trouve au moins une.
3.  **Phase 3 (Validation Globale)** : Exclusivement si les Phases 1 et 2 n'ont repéré aucune anomalie, le validateur global `@FormValidator` (méthode renvoyant `Map<String, String>`) se met en action pour accomplir les logiques métiers transversales.

Si toutes les phases sont réussies, le payload validé est officiellement délégué au contrôleur qui connecte votre `@FormHandler`.
