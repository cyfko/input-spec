## Dynamic Input Field Specification Protocol – Java (v2.0)

Implémentation Java strictement conforme à **Dynamic Input Field Specification Protocol v2.0**.

### ✅ Points clés v2
- Contraintes atomiques (`ConstraintDescriptor`) : un type unique (`pattern`, `minLength`, `maxValue`, `range`, `minDate`, etc.)
- `valuesEndpoint` (INLINE / remote) + modes `CLOSED` ou `SUGGESTIONS`
- Pipeline : REQUIRED → TYPE → MEMBERSHIP → CONSTRAINTS
- `InputSpec` expose `protocolVersion`
- `minLength` / `maxLength` = taille collection (jamais longueur string)
- `range` accepte `step` (numérique)
- `ValidationOptions.shortCircuit()` pour arrêter au premier échec
- Types inconnus & `custom` ignorés (forward compatibility)

### Installation (snapshot)
Maven:
```xml
<dependency>
    <groupId>io.github.cyfko</groupId>
    <artifactId>input-spec</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

### Conteneur racine
```java
InputSpec spec = InputSpec.builder()
    .addField(InputFieldSpec.builder()
            .displayName("Age")
            .dataType(DataType.NUMBER)
            .required(false)
            .expectMultipleValues(false)
            .build())
    .build();
```

### Contrainte pattern
```java
ConstraintDescriptor pattern = ConstraintDescriptor.builder()
    .name("usernamePattern")
    .type(ConstraintType.PATTERN)
    .params(Map.of("regex","^[a-zA-Z0-9_]{3,20}$"))
    .errorMessage("3-20 alphanum + underscore")
    .build();

InputFieldSpec username = InputFieldSpec.builder()
    .displayName("Username")
    .dataType(DataType.STRING)
    .required(true)
    .constraints(List.of(pattern))
    .build();
```

### Longueur sur collections
```java
ConstraintDescriptor minLen = ConstraintDescriptor.builder()
    .name("minLen")
    .type(ConstraintType.MIN_LENGTH)
    .params(Map.of("value",2))
    .build();

InputFieldSpec tags = InputFieldSpec.builder()
    .displayName("Tags")
    .dataType(DataType.STRING)
    .expectMultipleValues(true)
    .constraints(List.of(minLen))
    .build();
```

### Domaine fermé INLINE
```java
ValuesEndpoint domain = ValuesEndpoint.builder()
    .protocol(ValuesEndpoint.Protocol.INLINE)
    .mode(ValuesEndpoint.Mode.CLOSED)
    .items(List.of(new ValueAlias("A","Label A")))
    .build();
```

### Plage avec step
```java
ConstraintDescriptor range = ConstraintDescriptor.builder()
    .name("evenRange")
    .type(ConstraintType.RANGE)
    .params(Map.of("min",0,"max",10,"step",2))
    .build();
```

### Short-circuit
```java
ValidationResult res = new FieldValidator().validate(username, "%%%", ValidationOptions.shortCircuit());
```

### Pipeline de validation
| Étape | Description |
|-------|-------------|
| 1 REQUIRED | Présence si `required=true` |
| 2 TYPE | Typage de base + multiplicité |
| 3 MEMBERSHIP | Appartenance (mode CLOSED) |
| 4 CONSTRAINTS | Application séquentielle |

### Conformité protocole
| Aspect | Statut |
|--------|--------|
| `protocolVersion` dans `InputSpec` | ✅ |
| Pipeline ordonné | ✅ |
| Domaines INLINE fermés / suggestions | ✅ |
| Longueur uniquement collections | ✅ |
| `range` + `step` | ✅ |
| Short‑circuit | ✅ |
| Inconnus / custom ignorés | ✅ |

### Tests
```
./mvnw test
```

### Contribution
1. Respecter la spécification
2. Ajouter des tests
3. Documenter les changements

---
**Version lib** : 2.0.0-SNAPSHOT | **Protocole** : v2.0 | Mise à jour : Octobre 2025

## 🏗️ Architecture

### Types de base

- **DataType** : Énumération des types de données supportés (STRING, NUMBER, DATE, BOOLEAN)
- **PaginationStrategy** : Stratégies de pagination (NONE, PAGE_NUMBER)
- **CacheStrategy** : Stratégies de cache (NONE, SESSION, SHORT_TERM, LONG_TERM)
- **ValueAlias** : Représente une option de valeur avec sa valeur et son libellé

### Configuration

- **ConstraintDescriptor** : Décrit une contrainte de validation
- **RequestParams** : Configuration des paramètres de requête
- **ResponseMapping** : Configuration du mapping de réponse
- **ValuesEndpoint** : Configuration d'un endpoint de valeurs

### Validation

- **FieldValidator** : Validateur principal qui exécute les contraintes dans l'ordre
- **ValidationResult** : Résultat de validation avec statut et erreurs
- **ValidationError** : Erreur de validation spécifique

### API

- **FieldsResponse** : Réponse pour GET /api/fields
- **FieldResponse** : Réponse pour GET /api/fields/{fieldName}
- **ValuesResponse** : Réponse pour les endpoints de valeurs

## 🔄 Résolution de valeurs

```java
import io.github.cyfko.inputspec.client.*;

// Créer un client HTTP (implémentation par défaut)
HttpClient httpClient = new DefaultHttpClient();

// Créer un provider de cache
CacheProvider cacheProvider = new MemoryCacheProvider();

// Créer le résolveur de valeurs
ValuesResolver resolver = new ValuesResolver(httpClient, cacheProvider);

// Options de récupération
FetchValuesOptions options = new FetchValuesOptions();
options.setPage(1);
options.setSearch("john");
options.setLimit(20);

// Résoudre les valeurs
FetchValuesResult result = resolver.resolveValues(valuesEndpoint, options);

List<ValueAlias> values = result.getValues();
boolean hasNext = result.isHasNext();
Integer total = result.getTotal();
```

## 🧪 Tests

L'implémentation inclut des tests complets basés sur les exemples de la spécification du protocole :

```bash
# Exécuter les tests
./mvnw test

# Avec couverture
./mvnw test jacoco:report
```

## 📝 Conformité au protocole

Cette implémentation suit fidèlement la spécification du protocole v1.0 :

- ✅ **Contraintes contextuelles** : min/max s'adaptent au type et à la multiplicité
- ✅ **Ordre d'exécution** : Les contraintes sont exécutées dans l'ordre du tableau
- ✅ **Validation séquentielle** : Vérification niveau champ puis contraintes
- ✅ **Sources de valeurs flexibles** : Enum statiques et endpoints recherchables
- ✅ **Support de pagination** : Stratégies multiples avec recherche
- ✅ **Optimisation performance** : Stratégies de cache et debouncing
- ✅ **Messages d'erreur clairs** : Messages spécifiques par contrainte
- ✅ **Métadonnées auto-contenues** : Tout le nécessaire dans une réponse

## 🤝 Contribution

Les contributions sont les bienvenues ! Veuillez :

1. Suivre la spécification du protocole
2. Inclure des tests complets
3. Documenter les nouvelles fonctionnalités
4. Maintenir la compatibilité avec la spécification

## 📄 Licence

Ce projet est sous licence MIT - voir le fichier [LICENSE](../../../LICENSE) pour les détails.

## 🔗 Liens

- **Spécification du protocole** : [PROTOCOL_SPECIFICATION.md](../../../PROTOCOL_SPECIFICATION.md)
- **Implémentation TypeScript** : [TypeScript Implementation](../../typescript/)
- **Repository GitHub** : [cyfko/input-spec](https://github.com/cyfko/input-spec)

---

**Version** : 1.0.0 | **Protocole** : v1.0 | **Mise à jour** : Octobre 2025