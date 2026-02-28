---
sidebar_position: 1
id: agents-and-forms
title: Agents IA & Formulaires
---

# Pourquoi le MCP a besoin d'InputSpec

Le **Model Context Protocol (MCP)** est un standard ouvert introduit par Anthropic qui permet aux modèles d'IA de se connecter de manière sécurisée à des sources de données locales ou distantes et à des outils.

S'il est relativement simple d'exposer des données en lecture seule (comme une requête en base de données) à une IA, permettre à un agent IA **d'interagir de manière mutable avec une application** (comme réserver une chambre d'hôtel, commander une pizza ou créer un ticket JIRA) est notoirement complexe.

## Le Problème avec les "Outils" IA

Lorsque vous exposez une fonction ou un "Outil" à un LLM (Grand Modèle de Langage), vous demandez au LLM de générer un payload JSON qui correspond au schéma de votre fonction.

Cependant, la logique métier est rarement aussi simple qu'un schéma plat.
- "La date de départ doit être postérieure à la date d'arrivée."
- "Si le paiement par carte a été choisi, le CVV est requis."
- "Le type de chambre doit être STANDARD, DELUXE ou SUITE."

Si vous donnez uniquement au LLM un schéma OpenAPI générique, il va devoir deviner le payload, le soumettre, obtenir un `400 Bad Request` concernant une erreur de validation qu'il ne comprend pas, et réessayer jusqu'à ce qu'il hallucine ou entre dans une boucle infinie.

## La Solution InputSpec

InputSpec a été conçu dès le départ pour résoudre ce problème précis. En séparant la représentation structurelle d'un formulaire (le payload JSON `DIFSP`) de la logique métier backend, nous avons créé le langage parfait pour les LLMs.

Puisque le JSON InputSpec codifie explicitement les contraintes métier en règles standardisées et lisibles par machine :

1. **Auto-Correction** : Le LLM lit les contraintes (`"min": 18`, `"FUTURE"`) *avant* de générer le payload.
2. **Validation Déterministe** : Le LLM sait exactement ce que l'application attend. Il n'a pas besoin de deviner si le numéro de téléphone nécessite un formatage international ; la contrainte `"regex"` le lui indique.
3. **Sources de Valeurs** : Si un champ est un menu déroulant (une source de valeur `"REST"`), l'agent IA sait qu'il doit d'abord récupérer les options disponibles avant de tenter de remplir le formulaire, éliminant totalement les erreurs de type "Option Invalide".

## Le Spring Boot Starter

Pour combler le fossé entre Java et MCP, nous avons créé `input-spec-spring-boot-starter`.

Lorsque cette librairie est dans votre classpath, elle :
1. Scanne votre application à la recherche de méthodes `@FormHandler`.
2. Enregistre ces formulaires nativement auprès du `spring-ai-mcp-server`.
3. Auto-génère les descriptions d'Outils MCP en utilisant vos descriptions `@FieldMeta`.

Vos formulaires backend sont soudainement exposés comme des Outils MCP parfaitement documentés et hautement contraints que Claude, ChatGPT, ou tout autre agent peut exécuter nativement.

Consultez la [Démo Spring Boot](./spring-boot-demo) pour voir cela en action.
