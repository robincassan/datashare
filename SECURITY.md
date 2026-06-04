# Sécurité — DataShare

## Scan de vulnérabilités

### Frontend (npm audit)

Exécuté le 03/06/2026 :

```
$ npm audit
found 0 vulnerabilities
```

**Résultat :** ✅ Aucune vulnérabilité sur les 35 dépendances frontend.

Pour re-exécuter :
```bash
cd frontend
npm audit
```

### Backend (OWASP Dependency Check)

Exécuté le 03/06/2026 — **Échec partiel** ⚠️

```console
$ mvn org.owasp:dependency-check-maven:check
...
ERROR: NVD Returned Status Code: 429
Caused by: NvdApiException: NVD Returned Status Code: 429
BUILD FAILURE
```

L'API NVD (National Vulnerability Database) applique désormais un rate limiting strict sans clé API.  
Sur les ~355 000 CVE à télécharger, seules 20 000 ont été récupérées avant le blocage.

**Solution :** créer une clé API gratuite sur https://nvd.nist.gov/developers/request-an-api-key, puis l'ajouter dans `~/.m2/settings.xml` :

```xml
<settings>
  <servers>
    <server>
      <id>nvd-api</id>
      <username>votre-email@example.com</username>
      <password>votre-clé-api</password>
    </server>
  </servers>
</settings>
```

Réexécuter ensuite :

```bash
mvn org.owasp:dependency-check-maven:check -DnvdApiKey=votre-clé-api
```

**Dépendances du projet (pom.xml) :**

| Dépendance | Version |
|-----------|---------|
| Spring Boot (parent) | 3.5.14 |
| jjwt-api / jjwt-impl / jjwt-jackson | 0.12.6 |
| PostgreSQL JDBC | (gérée par Spring Boot) |
| Lombok | (gérée par Spring Boot) |

> Les versions ci-dessus sont récentes (2025-2026). Un scan OWASP complet avec clé API NVD est recommandé avant mise en production.

### Recommandations CI

- Ajouter `npm audit` dans le pipeline frontend
- Ajouter `mvn org.owasp:dependency-check-maven:check` dans le pipeline backend
- Activer Dependabot ou Renovate sur le dépôt GitHub

## Mesures de sécurité implémentées

### Authentification

- **JWT** (jjwt 0.12.6) — tokens signés avec clé secrète HMAC-SHA384
- **BCrypt** pour le hash des mots de passe
- **Validation des entrées** : email valide, mot de passe ≥ 8 caractères

### Upload de fichiers

- Taille max configurée à 1 Go
- Mot de passe optionnel (≥ 6 caractères)
- Expiration des liens (max 7 jours)
- Nettoyage des chemins fichiers (protection Path Traversal)

### Transport et stockage

- HTTP en développement, HTTPS recommandé en production
- Fichiers stockés sur disque, hors contexte web
- Mots de passe jamais stockés en clair
- JWT contient uniquement l'ID utilisateur
