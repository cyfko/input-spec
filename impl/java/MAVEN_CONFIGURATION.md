# 🚀 Configuration Maven avec Builder Pattern

Le projet Java a été configuré avec le plugin Maven `exec-maven-plugin` pour faciliter l'exécution des exemples avec le builder pattern.

## 📋 Commandes Disponibles

### 🔸 Exemples Basiques (par défaut)
```bash
# Option 1: Utilise le profil par défaut
.\mvnw exec:java

# Option 2: Profil explicite
.\mvnw exec:java -P basic-examples

# Option 3: Script Windows
.\run-examples.bat basic
```

### 🔸 Exemples Avancés
```bash
# Option 1: Profil Maven
.\mvnw exec:java -P advanced-examples

# Option 2: Script Windows  
.\run-examples.bat advanced
```

### 🔸 Compilation et Tests
```bash
# Compiler uniquement
.\mvnw compile

# Lancer les tests
.\mvnw test

# Tout compiler et tester
.\mvnw clean compile test
```

## ⚙️ Configuration Maven

Le `pom.xml` est configuré avec :

1. **Profils Maven** pour sélectionner la classe d'exemple :
   - `basic-examples` (actif par défaut)
   - `advanced-examples` 

2. **Plugin exec-maven-plugin** version 3.1.0 configuré pour utiliser la propriété `${exec.mainClass}` définie par les profils

3. **Script Windows** `run-examples.bat` pour simplifier l'exécution

## 📁 Structure des Exemples

```
src/main/java/io/github/cyfko/inputspec/examples/
├── BasicValidationExamples.java      # 8 exemples de base
├── AdvancedExamples.java             # 4 exemples avancés
└── README.md                         # Documentation détaillée
```

## 🎯 Avantages de Cette Configuration

✅ **Simplicité**: Une seule commande pour exécuter les exemples  
✅ **Flexibilité**: Deux modes (basique/avancé) via profils Maven  
✅ **Documentation**: JSON généré automatiquement pour voir le résultat  
✅ **Builder Pattern**: Code 50% plus court et plus lisible  
✅ **Cross-Platform**: Fonctionne Windows/Linux/macOS avec Maven  

## 💡 Exemples de Sortie

### Exemples Basiques
- Validation username, email, âge
- Mot de passe, priorité, date
- Newsletter, code postal
- Comparaison avant/après builder pattern

### Exemples Avancés  
- Endpoints dynamiques avec cache
- Contraintes multiples sur un champ
- Champs multi-valeurs (compétences)
- Formulaire complet (nom + email + pays)

## 🔧 Personnalisation

Pour ajouter vos propres exemples :

1. Créer une nouvelle classe dans `examples/`
2. Ajouter un profil Maven dans `pom.xml`
3. Mettre à jour `run-examples.bat` 
4. Utiliser le builder pattern pour la lisibilité !

Le builder pattern rend vraiment les exemples plus pratiques et lisibles ! 🎨