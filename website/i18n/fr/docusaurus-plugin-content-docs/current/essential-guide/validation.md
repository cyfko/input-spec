---
sidebar_position: 2
id: validation
title: Validation & Contraintes
---

# Validation & Contraintes

L'un des plus grands points de friction dans la création d'applications web est la duplication de la logique de validation : l'écrire une fois dans le frontend (HTML/JavaScript), et l'écrire à nouveau dans le backend (Java).

InputSpec résout cela en détournant les annotations standards de **Jakarta Bean Validation** (`@NotNull`, `@Size`, `@Min`, etc.) que vous utilisez probablement déjà.

## Comment ça marche

Lorsque le processeur d'annotations InputSpec s'exécute, il inspecte vos champs. S'il trouve une annotation de validation Jakarta, il traduit cette règle sémantique dans le protocole JSON déclaratif (le `DIFSP`).

### Le Flux
1. Vous ajoutez `@Size(min = 5)` à un champ Java.
2. InputSpec génère du JSON contenant `{"type": "MIN_LENGTH", "value": 5}`.
3. Votre librairie frontend lit le JSON et applique `minLength="5"` sur l'input HTML, empêchant l'utilisateur de saisir des données invalides.
4. Lors de la soumission, le backend Spring Boot exécute *exactement la même* validation `@Size` pour garantir l'intégrité des données avant la sauvegarde.

## Contraintes Supportées

InputSpec traduit automatiquement les contraintes standards suivantes :

### Nullité & Présence
*   `@NotNull`, `@NotEmpty`, `@NotBlank` ➡️ Définit `required: true` dans la spec JSON.

### Nombres
*   `@Min(N)` / `@DecimalMin(N)` ➡️ Contrainte `MIN(N)`.
*   `@Max(N)` / `@DecimalMax(N)` ➡️ Contrainte `MAX(N)`.

### Chaînes & Collections
*   `@Size(min=A, max=B)` ➡️ Contraintes `MIN_LENGTH(A)` et `MAX_LENGTH(B)`.
*   `@Pattern(regexp="...")` ➡️ Contrainte `PATTERN("...")`. Le frontend peut utiliser cette regex directement pour la validation de motif HTML5.
*   `@Email` ➡️ Contrainte `EMAIL`.

### Dates
*   `@Past` / `@PastOrPresent` ➡️ Contrainte `PAST`.
*   `@Future` / `@FutureOrPresent` ➡️ Contrainte `FUTURE`.

---

## Exemple

```java
public class RegistrationForm {

    @NotBlank
    @Size(min = 3, max = 20)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    @FieldMeta(displayName = "Nom d'utilisateur")
    private String username;

    @Min(18)
    @FieldMeta(displayName = "Âge")
    private int age;

    @Future
    @FieldMeta(displayName = "Date d'emménagement souhaitée")
    private LocalDate moveInDate;
}
```

Ce code Java garantit que le formulaire frontend généré restreindra la longueur du `username`, appliquera un motif regex pour bloquer les caractères spéciaux, s'assurera que l'input `age` a un `min="18"`, et empêchera la sélection de dates passées dans le datepicker `moveInDate`.

Le tout depuis une seule source de vérité.

## Au-delà du standard Jakarta

Parfois, la validation d'un champ unique ne suffit pas. Et si la "Date de départ" doit être strictement postérieure à la "Date d'arrivée" ?

Pour ces scénarios, InputSpec propose les [Contraintes Croisées](../advanced-guide/cross-constraints) et les [Handlers de Validation Personnalisés](../advanced-guide/custom-handlers).
