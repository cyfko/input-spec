---
sidebar_position: 1
id: intro
title: Pourquoi InputSpec ?
---

# Pourquoi InputSpec ?

Bienvenue dans **InputSpec**, l'approche déclarative, fortement-typée et orientée-protocole pour construire, valider et partager des formulaires entre votre backend, votre frontend et les agents d'Intelligence Artificielle.

## Le Problème

Dans le développement web moderne, les formulaires sont une source notoire de duplication et de couplage fort. Prenez un formulaire d'inscription standard. En général, vous devez :

1. **Écrire l'UI :** Construire le formulaire HTML/React/Vue, lier manuellement les labels, les placeholders et les messages d'erreur.
2. **Écrire la validation Frontend :** Écrire la logique JavaScript pour s'assurer que l'email est valide et que le mot de passe est assez long.
3. **Écrire le POJO/DTO Backend :** Créer une classe Java représentant le payload.
4. **Écrire la validation Backend :** Ajouter des annotations Jakarta (`@NotNull`, `@Email`) pour recréer exactement les mêmes règles que vous venez d'écrire en JS.
5. **Écrire la documentation de l'API :** Documenter les endpoints pour que l'équipe frontend sache quoi envoyer.

Si un chef de produit demande de passer la longueur minimale du mot de passe de 8 à 12 caractères, vous devez trouver et mettre à jour cette règle à **cinq endroits différents**. Cela mène inévitablement à une dérive de la validation, où le frontend accepte des données rejetées par le backend.

## La Solution InputSpec

**Codez une fois, générez partout.**

InputSpec renverse ce paradigme en utilisant votre modèle de domaine backend fortement-typé comme **unique source de vérité**. 

En annotant simplement vos POJO Java avec des contraintes de validation Jakarta standard et quelques décorateurs InputSpec, un processeur d'annotations génère automatiquement une spécification JSON technostique à la compilation.

### Comment ça marche

1. **Déclarez :** Vous écrivez votre classe Java et annotez les champs avec `@NotNull`, `@Min`, `@Email`.
2. **Compilez :** Le processeur d'annotations InputSpec génère un fichier `[form-id].json` respectant le Protocole de Spécification Dynamique de Champ de Saisie (DIFSP).
3. **Consommez :** Votre frontend (React, Vue, Flutter) rend dynamiquement le formulaire en lisant le JSON. Plus aucune saisie codée en dur.
4. **Soumettez :** Le starter Spring Boot lie automatiquement la soumission à votre POJO et le valide avant même qu'il n'atteigne votre logique métier.

## Prêt pour l'IA (Model Context Protocol)

Les formulaires sont le principal moyen d'interaction avec les logiciels. Mais qu'en est-il des agents d'IA ?

InputSpec s'intègre nativement au **Model Context Protocol (MCP)**. Parce que vos formulaires sont entièrement décrits dans un protocole JSON structuré, les agents IA (comme Claude ou des GPTs personnalisés) peuvent **découvrir, comprendre et remplir de manière autonome les formulaires de votre application** au nom de l'utilisateur, sans requérir de *prompt engineering* avancé.

Prêt à voir cela en action ? Rendez-vous sur le guide [Hello World](./getting-started/hello-world).
