# Migration v1 → v2

> **TL;DR Migration** : 1) Remplacer `enumValues` par `valuesEndpoint.values` (INLINE + `mode: CLOSED`). 2) Promouvoir tout `valuesEndpoint` enfoui dans une contrainte au niveau racine. 3) Aplatir chaque propriété (`minValue`, `pattern`, etc.) en contrainte atomique `{ name, type, params }`. 4) Supprimer toute structure composite et ajouter `mode` explicite si domaine fermé.

Cette page décrit comment migrer les spécifications et implémentations clientes du modèle **v1 (legacy)** vers le modèle **v2 (courant)**.

> Le support runtime de v1 est conservé seulement via un adaptateur de compatibilité dans l'implémentation TypeScript. Il sera retiré en 3.0.0.

## 1. Principes clés de la v2

| Sujet | v1 (legacy) | v2 (actuel) | Action migration |
|-------|-------------|-------------|------------------|
| Structure des contraintes | Composite possibles (objet libre) | Contraintes atomiques (`type`, `params`) | Aplatir chaque contrainte en entrée distincte |
| Domaine de valeurs | `enumValues` OU bloc `valuesEndpoint` dans une contrainte composite | Champ de premier niveau `valuesEndpoint` ou `values` inline | Extraire et déplacer au niveau champ |
| Sélecteur de valeurs | Champs ad-hoc (`url`, `searchParam`, etc.) souvent incohérents | Schéma normalisé (`protocol`, `uri`, `searchField`, `paginationStrategy`, `requestParams`, `mode`) | Renommer et structurer |
| Mode de domaine | Implicite / non défini | `mode`: `CLOSED` ou `SUGGESTIONS` | Déterminer selon besoin métier |
| Enum statique | `enumValues` tableau brut | `valuesEndpoint` de type INLINE (`{"values": [...]}`) | Envelopper dans `valuesEndpoint.values` |
| Nom d'erreur | Arbitrage libre | `constraintName` standardisé (souvent = `name`) | Harmoniser noms | 
| Étapes validation | Non spécifiées formellement | Pipeline déterministe (required → type → membership → constraints) | Aucun changement de logique, mais clarifier si ordre dépendant |

## 2. Transformation des contraintes

### v1 Exemple composite
```json
{
  "displayName": "Age",
  "dataType": "NUMBER",
  "constraints": [{
    "name": "rangeAndRequired",
    "minValue": 18,
    "maxValue": 99,
    "required": true
  }]
}
```

### v2 Équivalent atomique
```json
{
  "displayName": "Age",
  "dataType": "NUMBER",
  "required": true,
  "constraints": [
    { "name": "min", "type": "minValue", "params": { "value": 18 } },
    { "name": "max", "type": "maxValue", "params": { "value": 99 } }
  ]
}
```

### Règles pratiques
- Extraire chaque propriété logique (`minValue`, `maxValue`, `pattern`, etc.) en **contrainte séparée**.
- Conserver un `name` stable servant d'identifiant d'erreur.
- Mettre les paramètres sous `params` (ou le champ attendu par le type) pour uniformité.
- Le champ `required` reste un booléen racine (si présent dans composite, le remonter).

## 3. Migration des sources de valeurs

### v1 (enumValues)
```json
{
  "displayName": "Statut",
  "dataType": "STRING",
  "enumValues": [
    { "value": "OPEN", "label": "Ouvert" },
    { "value": "CLOSED", "label": "Fermé" }
  ]
}
```

### v2 (INLINE valuesEndpoint)
```json
{
  "displayName": "Statut",
  "dataType": "STRING",
  "valuesEndpoint": {
    "values": [
      { "value": "OPEN", "label": "Ouvert" },
      { "value": "CLOSED", "label": "Fermé" }
    ],
    "mode": "CLOSED"
  }
}
```

### v1 (endpoint dans contrainte)
```json
{
  "displayName": "Assigné à",
  "dataType": "STRING",
  "constraints": [{
    "name": "user_selector",
    "valuesEndpoint": {
      "uri": "/api/users",
      "searchField": "name" 
    }
  }]
}
```

### v2 (endpoint promu)
```json
{
  "displayName": "Assigné à",
  "dataType": "STRING",
  "valuesEndpoint": {
    "protocol": "HTTPS",
    "uri": "/api/users",
    "searchField": "name",
    "paginationStrategy": "PAGE_NUMBER",
    "mode": "CLOSED"
  }
}
```

## 4. Choix du `mode` de domaine
- `CLOSED`: seules les valeurs retournées / listées sont valides (validation MEMBERSHIP strict).
- `SUGGESTIONS`: valeurs retournées proposent une aide mais d'autres valeurs peuvent être saisies (validation membership souple côté client, stricte côté serveur selon politique).

## 5. Points de vigilance
| Sujet | Détail | Action |
|-------|--------|--------|
| Regex d'origine | Certaines composites mélangeaient pattern + length | Séparer en deux contraintes (`pattern`, `minLength`/`maxLength`) |
| Messages d'erreur | Inline ou implicites | Centraliser via `errorMessage` facultatif par contrainte |
| Datation | Formats mixtes (timestamp, ISO) | Normaliser selon conventions implémentation cible |
| Types numériques | Chaînes converties dynamiquement (TS) | S'appuyer sur coercion TS ou valider avant en Java |
| minLength/maxLength single (Java) | Implémentation Java actuelle ignore longueur pour simple valeur | Ajuster code ou documenter (voir notes implémentation) |

## 6. Script / adaptation automatisée (pseudo)
```ts
function migrateField(fieldV1) {
  const { enumValues, constraints = [], ...rest } = fieldV1;
  const atomic = [];
  for (const c of constraints) {
    for (const [k,v] of Object.entries(c)) {
      if (["minValue","maxValue","minLength","maxLength","pattern"].includes(k)) {
        atomic.push({ name: k, type: k, params: { value: v, regex: v } });
      }
    }
  }
  return {
    ...rest,
    valuesEndpoint: enumValues ? { values: enumValues, mode: 'CLOSED' } : promoteEndpoint(constraints),
    constraints: atomic
  };
}
```

## 7. Checklist migration
- [ ] Identifier tous les usages de `enumValues`
- [ ] Promouvoir endpoints contenus dans des composites
- [ ] Aplatir contraintes multi-propriétés
- [ ] Ajouter `mode` explicite (`CLOSED` par défaut pour anciennes listes fermées)
- [ ] Vérifier cohérence de noms (`name` stable)
- [ ] Ajouter messages d'erreur si attendus côté UI
- [ ] Mettre à jour tests unitaires / snapshots

## 8. FAQ migration
**Que faire si mon client dépend encore de v1 ?**  
Utiliser l'adaptateur legacy TS (court terme) ou migrer immédiatement — aucune nouvelle fonctionnalité n'arrivera sur v1.

**Puis-je garder `enumValues` ?**  
Toléré seulement via adaptateur, sinon remplacer.

**Et si mon endpoint ne supporte pas encore la pagination ?**  
Fixer `paginationStrategy": "NONE"` et ajouter plus tard.

## 9. Prochaine étape
Consulter `docs/IMPLEMENTATION_NOTES.md` pour détails sur les extensions et divergences connues.
