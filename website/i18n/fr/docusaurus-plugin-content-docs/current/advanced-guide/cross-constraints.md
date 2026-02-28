---
sidebar_position: 1
id: cross-constraints
title: Contraintes Croisées
---

# Contraintes Croisées

Bien que la validation Jakarta soit excellente pour les règles sur un champ unique (`@Min`, `@NotNull`), de nombreuses règles métier dépendent de la relation *entre* les champs.

*   "La date de départ doit être postérieure à la date d'arrivée."
*   "Si 'Méthode de Paiement' est Carte de Crédit, alors 'Numéro de Carte' est requis."
*   "Vous pouvez spécifier un 'Téléphone' OU un 'E-mail', mais vous devez fournir au moins l'un des deux."

InputSpec gère ces scénarios nativement en utilisant les **Contraintes Croisées** (Cross-Constraints).

## L'Annotation `@CrossConstraint`

Les contraintes croisées sont définies au **niveau de la classe** (et non au niveau du champ), en utilisant l'annotation `@CrossConstraint` à l'intérieur de `@FormSpec`.

InputSpec supporte quatre types de contraintes croisées :

### 1. Comparaison de Champs (`FIELD_COMPARISON`)

Compare les valeurs de deux champs en utilisant des opérateurs standards (`EQUALS`, `NOT_EQUALS`, `GREATER_THAN`, `LESS_THAN`, etc.).

```java
import io.github.cyfko.inputspec.FormSpec;
import io.github.cyfko.inputspec.CrossConstraint;

@FormSpec(
    id = "booking-form",
    crossConstraints = {
        @CrossConstraint(
            type = CrossConstraint.Type.FIELD_COMPARISON,
            fields = {"checkOut", "checkIn"}, // Le champ de sortie est premier, la référence est seconde
            operator = CrossConstraint.Operator.GREATER_THAN,
            errorMessage = "La date de départ doit être strictement postérieure à l'arrivée."
        )
    }
)
public class BookingForm {
    private LocalDate checkIn;
    private LocalDate checkOut;
}
```

### 2. Dépendance (`DEPENDS_ON`)

L'obligation ou la visibilité d'un champ bascule en fonction de la valeur d'un autre champ.

```java
@FormSpec(
    id = "payment-form",
    crossConstraints = {
        @CrossConstraint(
            type = CrossConstraint.Type.DEPENDS_ON,
            fields = {"creditCardNumber", "paymentMethod"}, // Champ cible, Champ source
            expectedValue = "CREDIT_CARD",
            errorMessage = "Le numéro de carte est requis lors d'un paiement par carte."
        )
    }
)
public class PaymentForm {
    private String paymentMethod; // ex: "PAYPAL" ou "CREDIT_CARD"
    private String creditCardNumber;
}
```

### 3. Mutuellement Exclusifs (`MUTUALLY_EXCLUSIVE`)

Garantit qu'au maximum *un seul* des champs spécifiés peut être renseigné.

```java
@FormSpec(
    id = "contact-preferences",
    crossConstraints = {
        @CrossConstraint(
            type = CrossConstraint.Type.MUTUALLY_EXCLUSIVE,
            fields = {"homePhone", "mobilePhone"},
            errorMessage = "Veuillez fournir SOIT un téléphone fixe, SOIT un mobile, pas les deux."
        )
    }
)
public class ContactPreferences {
    private String homePhone;
    private String mobilePhone;
}
```

### 4. Au Moins Un (`AT_LEAST_ONE`)

Garantit qu'*au minimum un* des champs spécifiés est renseigné.

```java
@FormSpec(
    id = "contact-methods",
    crossConstraints = {
        @CrossConstraint(
            type = CrossConstraint.Type.AT_LEAST_ONE,
            fields = {"email", "phoneNumber"},
            errorMessage = "Vous devez fournir soit une adresse e-mail soit un numéro de téléphone pour que nous puissions vous joindre."
        )
    }
)
public class ContactMethods {
    private String email;
    private String phoneNumber;
}
```

## Comment c'est exécuté

Lorsque vous déclarez une contrainte croisée :
1. Elle est exportée dans le fichier `[form-id].json` sous le tableau `"crossConstraints"`.
2. Une librairie frontend intelligente peut utiliser ce tableau pour afficher/masquer dynamiquement des champs (ex: cacher `creditCardNumber` si `paymentMethod` != "CREDIT_CARD") ou afficher un texte rouge sous les champs si les dates sont inversées.
3. Le backend Spring Boot (`input-spec-spring-boot-starter`) **exécute automatiquement ces règles** conjointement avec vos annotations Jakarta avant même d'atteindre votre logique. Si l'utilisateur (ou l'IA) a contourné l'UI et envoyé un payload JSON invalide, une erreur `400 Bad Request` est toujours levée, garantissant l'intégrité backend.
