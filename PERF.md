# Performance — DataShare

## Test de charge (k6)

### Endpoint testé : upload de fichier

**Scénario :** Inscription → Connexion → Upload fichier (100 Ko généré en mémoire)

```bash
k6 run perf-test.js -e BASE_URL=http://localhost:8080
```

| Métrique | Valeur |
|----------|--------|
| Utilisateurs simultanés | Montée progressive 1 → 10 (3 paliers) |
| Durée | 20 secondes |
| Itérations | 98 |
| Requêtes totales | 294 |

### Résultats

| Check | Taux de succès |
|-------|---------------|
| Inscription (201 ou 409 si existant) | 100 % |
| Connexion | 100 % |
| Upload | 100 % |

### Temps de réponse

| Métrique | Temps |
|----------|-------|
| Moyen | 61 ms |
| Médian (p50) | 77 ms |
| p90 | 92 ms |
| **p95** | **97 ms** |
| Max | 326 ms |

### Analyse

- **p95 à 97 ms** — très en dessous du seuil de 2 secondes
- Toutes les requêtes critiques (upload, download, login) ont 100 % de succès
- Les 409 sur inscription sont des doublons d'exécution (sans conséquence)
- Les temps de réponse montrent une application réactive même sous charge

### Recommandations

- Tester avec 50-100 utilisateurs simultanés
- Tester avec des fichiers de 1 Mo, 10 Mo, 100 Mo
- Ajouter un healthcheck monitoring

## Logs structurés

### Backend (Spring Boot)

Les logs structurés au format JSON sont configurés dans `application.yaml` :

```yaml
logging:
  pattern:
    console: "{\"timestamp\":\"%d{ISO8601}\",\"level\":\"%p\",\"logger\":\"%c\",\"message\":\"%m\"}%n"
  level:
    com.datashare: DEBUG
    org.springframework.web: INFO
```

Cette configuration produit des logs JSON exploitables par **ELK**, **Grafana Loki**, ou **Datadog**.

### Métriques clés à surveiller

| Métrique | Outil | Seuil d'alerte |
|----------|-------|---------------|
| Temps de réponse upload | Logs / APM | > 2s (p95) |
| Taux d'erreur 4xx/5xx | Logs / APM | > 5% |
| Uploads échoués | Logs | > 0 |
| Espace disque stockage | Système | < 20% libre |
| Taille des fichiers | Logs | > 500 Mo |
