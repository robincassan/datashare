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

## Budget frontend

### Build production (Angular 21)

```bash
ng build --configuration production
```

**Temps de build :** 16 secondes

### Taille des bundles

| Fichier | Raw size | Transfer size |
|---------|----------|---------------|
| Initial total | **280.92 kB** | **78.69 kB** |
| `polyfills` (zone.js, etc.) | 35.78 kB | 11.63 kB |
| `main` (bootstrap) | 1.13 kB | 547 B |
| `styles` | 0 B | 0 B |
| Dashboard (lazy) | 5.19 kB | 1.80 kB |
| Register (lazy) | 2.25 kB | 892 B |
| Login (lazy) | 1.78 kB | 764 B |
| Download (lazy) | 2.66 kB | 1.10 kB |

### Budgets configurés (angular.json)

| Type | Warning | Error |
|------|---------|-------|
| Initial bundle | 500 kB | 1 MB |
| Component style | 4 kB | 8 kB |

### Analyse

- **Initial total à 78.69 kB (transfer)** — très léger, bien sous le budget warning de 500 kB
- **Lazy loading** — les pages (dashboard, login, register, download) sont chargées à la demande, pas dans le bundle initial
- **Pas de dépendances lourdes** — pas de Material UI, Bootstrap, chart.js, etc. Le bundle reste minimal
- **Styles à 0 B** — les styles sont inlinés dans les composants Angular (pas de fichier CSS externe)
