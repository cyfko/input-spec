---
sidebar_position: 1
id: forms
title: Définir des Formulaires
---

# Définir des Formulaires

La philosophie centrale d'InputSpec est que votre modèle de domaine Java est l'unique source de vérité pour votre UI.

Pour exposer un objet Java pur en tant que formulaire InputSpec, vous utilisez deux annotations principales : `@FormSpec` au niveau de la classe, et `@FieldMeta` au niveau du champ.

## L'Annotation `@FormSpec`

Cette annotation indique au processeur InputSpec d'analyser la classe et de générer un fichier JSON conforme au Dynamic Input Field Specification Protocol (DIFSP).

```java
import io.github.cyfko.inputspec.FormSpec;

@FormSpec(
    id = "user-profile",
    displayName = "Profil Utilisateur",
    description = "Mettez à jour vos informations.",
    submitUri = "/api/forms/user-profile/submit",
    method = "PUT"
)
public class UserProfileForm {
    // champs...
}
```

### Propriétés clés

*   **`id`** (Requis) : Une chaîne unique identifiant ce formulaire dans le système. Elle devient le nom de fichier (`user-profile.json`) et l'ID utilisé par les handlers.
*   **`displayName`** : Le titre lisible du formulaire, généralement affiché comme un `<h1>` ou titre de carte dans l'UI.
*   **`description`** : Un sous-titre ou texte instructif guidant l'utilisateur.
*   **`submitUri`** : L'endpoint où le frontend ou l'agent IA doit envoyer la charge utile JSON validée.
*   **`method`** (Optionnel, défaut `POST`) : La méthode HTTP à utiliser pour la soumission (`POST`, `PUT`, `PATCH`).

## L'Annotation `@FieldMeta`

Tandis que les annotations de validation standards (`@NotNull`, `@Min`) définissent *comment* un champ se comporte, `@FieldMeta` définit *ce que* le champ est et comment il doit être présenté à l'utilisateur.

```java
import io.github.cyfko.inputspec.FieldMeta;
import jakarta.validation.constraints.NotBlank;

public class UserProfileForm {

    @NotBlank
    @FieldMeta(
        displayName = "Prénom",
        description = "Votre prénom tel qu'il apparaît sur votre carte d'identité.",
        formatHint = "text"
    )
    private String firstName;
    
    @FieldMeta(
        displayName = "Date de Naissance",
        formatHint = "date"
    )
    private java.time.LocalDate dateOfBirth;
}
```

### Propriétés clés

*   **`displayName`** : L'étiquette (label) affichée à côté du champ de saisie dans l'UI.
*   **`description`** : Texte d'aide, info-bulle, ou conseils de remplissage (placeholder).
*   **`formatHint`** : Une indication purement présentationnelle pour le frontend (ex: `text`, `date`, `password`, `email`). Notez que l'inférence du type de donnée réel est dictée par le type de la variable Java (ex: `String`, `Integer`, `LocalDate`).
*   **`valuesSource`** : (Avancé) Utilisé pour attacher des listes de choix, des menus déroulants ou des endpoints d'autocomplétion au champ (voir [Sources de Valeurs](./values-sources)).

### Inférence de Type

InputSpec déduit automatiquement le type de donnée primitif sous-jacent à partir de vos déclarations de champs Java :

*   `String` -> `STRING`
*   `int`, `Integer`, `long`, `Long` -> `INTEGER`
*   `double`, `Double`, `float`, `Float`, `BigDecimal` -> `DECIMAL`
*   `boolean`, `Boolean` -> `BOOLEAN`
*   `LocalDate`, `OffsetDateTime` -> `DATE` ou `DATETIME`

Vous n'avez jamais à spécifier manuellement `type="STRING"` dans les annotations ; le compilateur gère la réflexion pour vous.
