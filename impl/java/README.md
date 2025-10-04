# Dynamic Input Field Specification Protocol - Java Implementation

Une implémentation Java fidèle au protocole **Dynamic Input Field Specification Protocol v1.0**.

## 📋 Vue d'ensemble

Cette bibliothèque Java implémente le protocole de spécification de champs d'entrée dynamiques, permettant de :
- Définir des spécifications de champs d'entrée à l'exécution
- Comprendre les contraintes et sources de valeurs sans codage en dur
- Activer les champs de formulaire intelligents avec auto-complétion et validation
- Supporter la sélection de valeurs recherchables et paginées
- Maintenir l'interopérabilité entre langages

## 🚀 Installation

### Maven

```xml
<dependency>
    <groupId>io.github.cyfko</groupId>
    <artifactId>input-field-spec-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'io.github.cyfko:input-field-spec-java:1.0.0'
```

## 📖 Guide d'utilisation

### Exemple 1: Champ texte simple

Basé sur l'exemple 1 de la spécification du protocole :

```java
import io.github.cyfko.inputspec.*;
import io.github.cyfko.inputspec.validation.*;
import java.util.Arrays;

// Créer une contrainte de validation
ConstraintDescriptor valueConstraint = new ConstraintDescriptor("value");
valueConstraint.setMin(3);
valueConstraint.setMax(20);
valueConstraint.setPattern("^[a-zA-Z0-9_]+$");
valueConstraint.setDescription("Username (3-20 alphanumeric characters)");
valueConstraint.setErrorMessage("Username must be 3-20 characters, alphanumeric with underscores");

// Créer la spécification du champ
InputFieldSpec usernameField = new InputFieldSpec(
    "Username",                    // displayName
    DataType.STRING,              // dataType
    false,                        // expectMultipleValues
    true,                         // required
    Arrays.asList(valueConstraint) // constraints
);
usernameField.setDescription("User's unique identifier");

// Valider une valeur
FieldValidator validator = new FieldValidator();
ValidationResult result = validator.validate(usernameField, "john_doe");

if (result.isValid()) {
    System.out.println("Validation réussie !");
} else {
    result.getErrors().forEach(error -> 
        System.out.println("Erreur: " + error.getMessage())
    );
}
```

### Exemple 2: Champ numérique avec plage

Basé sur l'exemple 2 de la spécification :

```java
// Contrainte pour un champ numérique
ConstraintDescriptor valueConstraint = new ConstraintDescriptor("value");
valueConstraint.setMin(0);
valueConstraint.setDescription("Price value");
valueConstraint.setErrorMessage("Price must be greater than 0");
valueConstraint.setDefaultValue(0);

InputFieldSpec priceField = new InputFieldSpec(
    "Price",
    DataType.NUMBER,
    false,
    true,
    Arrays.asList(valueConstraint)
);
priceField.setDescription("Price filter range");

// Validation
ValidationResult result = validator.validate(priceField, 25.99);
```

### Exemple 3: Champ email avec pattern

Basé sur l'exemple 3 de la spécification :

```java
// Contrainte avec pattern regex et format
ConstraintDescriptor valueConstraint = new ConstraintDescriptor("value");
valueConstraint.setPattern("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,}$");
valueConstraint.setFormat("email");
valueConstraint.setDescription("Valid email address");
valueConstraint.setErrorMessage("Please provide a valid email address");

InputFieldSpec emailField = new InputFieldSpec(
    "Email Address",
    DataType.STRING,
    false,
    true,
    Arrays.asList(valueConstraint)
);
emailField.setDescription("Contact email address");

// Validation
ValidationResult result = validator.validate(emailField, "user@example.com");
```

### Exemple 4: Champ de sélection statique

Basé sur l'exemple 4 de la spécification :

```java
import java.util.Arrays;

// Créer les valeurs d'énumération
List<ValueAlias> enumValues = Arrays.asList(
    new ValueAlias("active", "Active"),
    new ValueAlias("inactive", "Inactive"),
    new ValueAlias("pending", "Pending")
);

ConstraintDescriptor valueConstraint = new ConstraintDescriptor("value");
valueConstraint.setDescription("Item status");
valueConstraint.setErrorMessage("Please select a status");
valueConstraint.setEnumValues(enumValues);

InputFieldSpec statusField = new InputFieldSpec(
    "Status",
    DataType.STRING,
    false,
    true,
    Arrays.asList(valueConstraint)
);
statusField.setDescription("Filter by status");

// Validation
ValidationResult result = validator.validate(statusField, "active");
```

### Exemple 5: Champ de sélection avec endpoint recherchable

Basé sur l'exemple 5 de la spécification :

```java
// Configuration du mapping de réponse
ResponseMapping responseMapping = new ResponseMapping("data");
responseMapping.setPageField("page");
responseMapping.setPageSizeField("pageSize");
responseMapping.setTotalField("total");
responseMapping.setHasNextField("hasNext");

// Configuration des paramètres de requête
RequestParams requestParams = new RequestParams();
requestParams.setPageParam("page");
requestParams.setLimitParam("limit");
requestParams.setSearchParam("search");
requestParams.setDefaultLimit(50);

// Configuration de l'endpoint
ValuesEndpoint valuesEndpoint = new ValuesEndpoint("/api/users", responseMapping);
valuesEndpoint.setMethod("GET");
valuesEndpoint.setSearchField("name");
valuesEndpoint.setPaginationStrategy(PaginationStrategy.PAGE_NUMBER);
valuesEndpoint.setCacheStrategy(CacheStrategy.SHORT_TERM);
valuesEndpoint.setDebounceMs(300);
valuesEndpoint.setMinSearchLength(2);
valuesEndpoint.setRequestParams(requestParams);

ConstraintDescriptor valueConstraint = new ConstraintDescriptor("value");
valueConstraint.setDescription("User to assign task to");
valueConstraint.setErrorMessage("Please select a user");
valueConstraint.setValuesEndpoint(valuesEndpoint);

InputFieldSpec assigneeField = new InputFieldSpec(
    "Assigned To",
    DataType.STRING,
    false,
    true,
    Arrays.asList(valueConstraint)
);
assigneeField.setDescription("Assign task to user");
```

### Exemple 6: Multi-sélection avec recherche

Basé sur l'exemple 6 de la spécification :

```java
// Configuration pour endpoint sans pagination
ResponseMapping responseMapping = new ResponseMapping("tags");

RequestParams requestParams = new RequestParams();
requestParams.setSearchParam("q");

ValuesEndpoint valuesEndpoint = new ValuesEndpoint("/api/tags", responseMapping);
valuesEndpoint.setSearchField("name");
valuesEndpoint.setPaginationStrategy(PaginationStrategy.NONE);
valuesEndpoint.setCacheStrategy(CacheStrategy.LONG_TERM);
valuesEndpoint.setDebounceMs(200);
valuesEndpoint.setMinSearchLength(1);
valuesEndpoint.setRequestParams(requestParams);

ConstraintDescriptor valueConstraint = new ConstraintDescriptor("value");
valueConstraint.setMin(1); // minimum 1 élément dans le tableau
valueConstraint.setMax(5); // maximum 5 éléments dans le tableau
valueConstraint.setDescription("Select 1 to 5 relevant tags");
valueConstraint.setErrorMessage("You must select between 1 and 5 tags");
valueConstraint.setValuesEndpoint(valuesEndpoint);

InputFieldSpec tagsField = new InputFieldSpec(
    "Tags",
    DataType.STRING,
    true, // expectMultipleValues = true pour multi-sélection
    true,
    Arrays.asList(valueConstraint)
);
tagsField.setDescription("Select relevant tags for content");

// Validation d'un tableau de valeurs
List<String> selectedTags = Arrays.asList("java", "spring", "rest");
ValidationResult result = validator.validate(tagsField, selectedTags);
```

### Exemple 7: Champ de date

Basé sur l'exemple 7 de la spécification :

```java
ConstraintDescriptor valueConstraint = new ConstraintDescriptor("value");
valueConstraint.setFormat("iso8601");
valueConstraint.setDescription("Creation date");
valueConstraint.setErrorMessage("Please provide a valid date");

InputFieldSpec dateField = new InputFieldSpec(
    "Created Date",
    DataType.DATE,
    false,
    false, // champ optionnel
    Arrays.asList(valueConstraint)
);
dateField.setDescription("Filter by creation date");

// Validation avec date ISO 8601
ValidationResult result = validator.validate(dateField, "2025-10-04T10:30:00Z");
```

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