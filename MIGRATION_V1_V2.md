# Migration Guide: v1 → v2

Ce document explique les différences majeures entre la version 1 (legacy) et la version 2 du protocole, et fournit des conseils pour migrer vos spécifications et implémentations.

---

## Principales différences

| Concept v1         | Exemple v1                                      | Remplacement v2                                 |
|--------------------|------------------------------------------------|-------------------------------------------------|
| `enumValues`       | `"enumValues": [{"value":"A","label":"A"}]` | `valuesEndpoint.protocol = INLINE` + `items`    |
| Contraintes mixtes | min+max+pattern dans un seul objet              | Plusieurs descripteurs atomiques (un par règle) |
| `valuesEndpoint` dans une contrainte | imbriqué dans constraint | Champ de niveau racine `valuesEndpoint`         |
| Champs scalaires   | `pattern`, `min`, `max`                         | `constraints[].type` + `params`                 |
| `format`           | Champ passif                                   | `formatHint` au niveau du champ                 |
| Longueur tableau   | `min:1, max:10` polymorphe                      | Contraintes explicites ou `range`/`minValue`/`maxValue` sur la longueur |

---

## Stratégie de migration automatisée (suggestion)

1. Remonter le premier `valuesEndpoint` (ou transformer `enumValues` en endpoint INLINE) au niveau du champ.
2. Si plusieurs endpoints sont trouvés → erreur de spécification (à résoudre manuellement).
3. Pour chaque contrainte legacy : créer un descripteur atomique par règle scalaire (`pattern`, `min`, `max`, etc.).
4. Supprimer totalement `enumValues`.
5. Déplacer la valeur `format` (si présente) vers le champ `formatHint`.
6. Ajouter un marqueur de version ou servir les deux versions via négociation de contenu si besoin de transition.

---

## Compatibilité

Les implémentations peuvent offrir un mode transitoire acceptant v1 et v2 jusqu’à la dépréciation complète.

---

Pour la spécification legacy complète, voir `APPENDIX_LEGACY.md`.
