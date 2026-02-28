---
sidebar_position: 2
id: spring-boot-demo
title: Démo Spring Boot
---

# MCP : Démo Spring Boot

Vous voulez voir InputSpec propulser un Agent IA ? Nous avons construit une démo complète, prête à l'emploi dans le répertoire `/examples/spring-boot-mcp-demo`.

La démo simule un **Système de Réservation d'Hôtel**. Elle utilise InputSpec pour définir le formulaire de réservation, et le serveur MCP de Spring AI pour l'exposer à des agents externes.

## 1. Le Formulaire

Le `BookingForm` est un simple POJO enrichi avec la validation Jakarta standard (pour la vérification de longueur basique), des Enums Java (pour la sélection de valeurs "inline"), et une `@CrossConstraint` d'InputSpec garantissant que les dates sont logiques.

```java
@FormSpec(
    id = "hotel-booking",
    displayName = "Réservation d'Hôtel",
    submitUri = "/api/forms/hotel-booking/submit",
    crossConstraints = {
        @CrossConstraint(
            type = CrossConstraint.Type.FIELD_COMPARISON,
            operator = CrossConstraint.Operator.GREATER_THAN,
            fields = {"checkOut", "checkIn"},
            errorMessage = "La date de départ doit être après l'arrivée."
        )
    }
)
public class BookingForm {
    // champs...
}
```

## 2. Le Gestionnaire (Handler)

La logique métier accepte simplement le `BookingForm` fortement-typé.

```java
@Service
public class BookingHandler {

    @FormHandler("hotel-booking")
    public SubmitResponse reserve(BookingForm form) {
        String bookingId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return SubmitResponse.ok(Map.of("status", "CONFIRME", "bookingId", bookingId));
    }
}
```

## 3. Lancer la Démo

1. Clonez le dépôt GitHub.
2. Naviguez vers le dossier de démo : `cd examples/spring-boot-mcp-demo`
3. Démarrez l'application : `mvn spring-boot:run`

L'application Spring Boot démarrera sur `http://localhost:8080`.

Puisque le `spring-ai-starter-mcp-server-webmvc` est inclus, le serveur expose automatiquement l'endpoint MCP SSE (Server-Sent Events) standard à `/mcp/message`.

## 4. Connecter "Claude Desktop"

Vous pouvez désormais configurer votre agent d'IA pour consommer cette application ! Par exemple, si vous utilisez l'application Claude Desktop, vous pouvez ajouter ceci à votre `claude_desktop_config.json` :

```json
{
  "mcpServers": {
    "hotel-booking-service": {
      "command": "npx",
      "args": [
        "-y",
        "@modelcontextprotocol/inspector",
        "http://localhost:8080/mcp/message"
      ]
    }
  }
}
```

Redémarrez Claude, et tapez simplement :

> "Je veux réserver une chambre Deluxe pour Alice Smith à partir du 1er avril 2026 pour 4 nuits."

Claude va :
1. Découvrir l'outil `inputspec_submit_hotel_booking`.
2. Lire les exigences JSON (comprenant que `DELUXE` est une valeur Enum, évaluant les contraintes).
3. Appeler l'outil automatiquement.
4. Vous répondre avec votre `bookingId` fraîchement généré !
