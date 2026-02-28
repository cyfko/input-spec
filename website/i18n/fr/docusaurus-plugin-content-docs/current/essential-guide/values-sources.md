---
sidebar_position: 3
id: values-sources
title: Sources de Valeurs (Sélecteurs)
---

# Sources de Valeurs

Tous les champs ne sont pas de simples textes libres. Souvent, vous souhaitez que l'utilisateur sélectionne parmi une liste prédéfinie d'options, comme choisir un Pays, un Type de Chambre, ou une catégorie.

Dans InputSpec, nous appelons cela les **Sources de Valeurs**. Le protocole fournit deux façons de fournir ces options au frontend : `INLINE` et `REST`.

## 1. INLINE (Listes Statiques)

Si la liste d'options est petite et change rarement, vous devriez les intégrer directement dans la définition du formulaire en utilisant `INLINE`.

### L'approche Enums Auto-Magique (Recommandée)

InputSpec v3 a introduit le mappage automatique des Enums. Si votre champ est entièrement défini par une `Enum` Java, déclarez-le simplement ! Le compilateur fait le reste.

```java
public enum RoomType {
    STANDARD,
    DELUXE,
    SUITE
}

public class BookingForm {

    @NotNull
    @FieldMeta(
        displayName = "Type de Chambre",
        description = "Sélectionnez votre catégorie de chambre préférée"
    )
    private RoomType roomType; // <--- La magie opère ici
}
```

InputSpec détecte automatiquement l'Enum et intègre une liste `INLINE CLOSED` dans le JSON. Il formatera même automatiquement les étiquettes (ex: `DELUXE` devient "Deluxe").

Puisqu'il s'agit d'une liste fermée, le validateur backend rejettera rigoureusement toute soumission tentant de faire passer "PENTHOUSE".

### L'approche explicite `@ValuesSource`

Si vous ne voulez pas utiliser d'Enum, ou si vous avez besoin de redéfinir explicitement les libellés, vous pouvez le configurer via l'annotation `@FieldMeta` :

```java
import io.github.cyfko.inputspec.ValuesSource;
import io.github.cyfko.inputspec.Inline;

public class TaskForm {

    @FieldMeta(
        displayName = "Priorité",
        valuesSource = @ValuesSource(
            protocol = "INLINE",
            mode = ValuesSource.ValuesMode.CLOSED,
            items = {
                @Inline(value = "P1", label = "Critique - Corriger immédiatement"),
                @Inline(value = "P2", label = "Élevée - Prochain Sprint"),
                @Inline(value = "P3", label = "Basse - Backlog")
            }
        )
    )
    private String priority;
}
```

## 2. REST (Listes Dynamiques)

Si votre liste d'options est immense (ex: toutes les villes du monde), ou si elle change constamment en fonction de l'état de la base de données (ex: Projets Actifs assignés à l'utilisateur), les intégrer "inline" est impossible.

Vous devez les récupérer dynamiquement en utilisant le protocole `REST`.

```java
public class AssignmentForm {

    @FieldMeta(
        displayName = "Assigner à l'utilisateur",
        valuesSource = @ValuesSource(
            protocol = "REST",
            mode = ValuesSource.ValuesMode.CLOSED,
            uri = "/api/v1/users?active=true"
        )
    )
    private String assignedUserId;
}
```

### Comment fonctionnent les Sources de Valeurs REST :
1. Lorsque le frontend rend le formulaire, il voit le protocole `REST` et l'`uri`.
2. Le frontend effectue une requête HTTP `GET` vers `/api/v1/users?active=true`.
3. Le frontend s'attend à ce que la réponse soit un tableau d'objets contenant `value` et `label` (ex: `[{"value": "u123", "label": "Alice"}, ...]`).
4. Le frontend peuple le menu déroulant avec ces résultats.

## Modes : CLOSED vs. SUGGESTIONS

Les deux protocoles `INLINE` et `REST` nécessitent de déclarer un `mode` :

*   **`CLOSED` (Fermé)** : L'utilisateur *doit* sélectionner exactement l'une des options fournies. Le validateur backend imposera activement cela en vérifiant la valeur soumise par rapport à la liste des valeurs autorisées exactes. (Comme un menu déroulant `<select>` strict).
*   **`SUGGESTIONS`** : La liste n'agit que comme une auto-complétion utile. L'utilisateur peut sélectionner une option, *ou* il peut taper son propre texte libre. Le validateur backend autorisera tout texte qui respecte les autres contraintes standards (comme `@Size`). (Comme un `<input datalist>`).
