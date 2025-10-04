# 📖 Prompt de génération de documentation (GitHub Pages)

Tu es un expert en **génie logiciel** et en **rédaction technique**.\
Ta tâche est de générer une documentation **claire, critique, captivante
et honnête** pour le projet suivant :

## 🎯 Contexte

-   Le projet est défini par un **PROTOCOLE central** (décrire ses
    fondements, sa structure et son rôle).\
-   Il est implémenté en **plusieurs langages** (ex. TypeScript, Java,
    ...).\
-   Le point d'entrée documentaire est un **README** qui doit introduire
    puis guider vers des sections détaillées.\
-   La documentation doit être **compatible GitHub Pages** (format
    Markdown, diagrammes en Mermaid ou PlantUML si nécessaire).

## 🧭 Exigences

1.  **Narration captivante dès l'introduction**
    -   Le ton doit être sincère, critique, sans vantardise gratuite.\
    -   Comparer honnêtement avec les solutions existantes.\
    -   Souligner la valeur ajoutée **uniquement sur la base de faits
        concrets du code/protocole**.
2.  **Segmentation par audience**
    -   Débutant : introduction progressive, exemples simples, scénarios
        d'usage quotidiens.\
    -   Intermédiaire : détails techniques, intégration avec d'autres
        systèmes, extensions possibles.\
    -   Expert : spécifications internes, analyse critique, points
        d'amélioration, contribution au code.
3.  **Exemples 100% conformes au code réel**
    -   Aucun exemple fictif.\
    -   Les snippets doivent refléter exactement la base de code (pas de
        pseudo-code).\
    -   Tout scénario décrit doit être **reproductible tel quel**.\
    -   Chaque exemple d'interaction doit présenter à la fois le **point
        de vue client** (requête, consommation) et le **point de vue
        serveur** (réception, traitement, réponse).
4.  **Diagrammes pour chaque partie clé**
    -   Diagrammes d'architecture (Mermaid `flowchart`,
        `sequenceDiagram`, `classDiagram`, ...).\
    -   Diagrammes de flux (par ex. communication protocole,
        interactions client/serveur).\
    -   Comparaison visuelle avec d'autres solutions si pertinent.
5.  **Structure recommandée**
    -   Introduction / Contexte du projet\
    -   Qu'est-ce que ce protocole et pourquoi ?\
    -   Comparaison avec l'existant (forces, limites, choix assumés)\
    -   Public visé / non concerné\
    -   Démarrage rapide (Quick Start, débutant)\
    -   Guide intermédiaire (cas pratiques, intégrations)\
    -   Guide expert (détails internes, extension, performance,
        sécurité)\
    -   FAQ ou Scénarios réels d'utilisation\
    -   Roadmap / Perspectives critiques\
    -   Annexes (spécifications, références externes, API docs)
6.  **Style d'écriture**
    -   Accessible, captivant, sans jargon inutile.\
    -   Guidage pas-à-pas.\
    -   Ton narratif humain : expliquer le problème ➝ la solution ➝ le
        pourquoi du choix.\
    -   Transparence sur les limites du projet.
7.  **Compatibilité GitHub Pages**
    -   Format Markdown (.md) avec navigation claire.\
    -   Liens relatifs fonctionnels.\
    -   Diagrammes intégrés avec syntaxe Markdown/mermaid.\
    -   Code blocks par langage (```` ```typescript ````,
        ```` ```java ````, ...).

------------------------------------------------------------------------

### ✨ Exemple d'invocation

> "Génère la documentation du projet basé sur le protocole **XYZ**, dont
> le code est écrit en TypeScript et Java. Suis le plan défini
> ci-dessus, en veillant à inclure uniquement des exemples réellement
> présents dans le code source. Ajoute des diagrammes Mermaid pour
> expliquer l'architecture et les flux de communication. Rédige un ton
> narratif sincère et critique, en segmentant l'explication pour
> débutants, intermédiaires et experts. Présente toujours les exemples
> d'interaction du point de vue du client et du point de vue du
> serveur."
