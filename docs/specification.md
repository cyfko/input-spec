---
layout: default
title: "Spécification protocole"
permalink: /specification/
redirect_from:
	- /PROTOCOL_SPECIFICATION.html
nav_order: 95
description: "Copie rendue navigable de PROTOCOL_SPECIFICATION.md pour éviter les 404"
---

# Spécification du protocole

Cette page référence le contenu de `PROTOCOL_SPECIFICATION.md` situé à la racine du dépôt afin d'offrir une URL stable dans la navigation latérale sans provoquer d'erreur 404.

> Source unique de vérité : le fichier racine `PROTOCOL_SPECIFICATION.md`. En cas de divergence, ce dernier prévaut. Mettez les deux à jour dans une même PR.

{% capture spec_content %}
{% include_relative ../PROTOCOL_SPECIFICATION.md %}
{% endcapture %}
{{ spec_content }}
