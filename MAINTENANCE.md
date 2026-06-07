# Maintenance — DataShare

## Prérequis

- **Java 21+** (backend)
- **Node.js 24+** (frontend)
- **PostgreSQL 16+** (base de données)
- **Maven 3.9+** (build backend)

## Procédures de maintenance

### 1. Dépendances

**Backend (Maven)**
```bash
cd backend
# Vérifier les mises à jour disponibles
mvn versions:display-dependency-updates

# Mettre à jour les dépendances
mvn versions:use-latest-releases
```

**Frontend (npm)**
```bash
cd frontend
# Vérifier les mises à jour
npm outdated

# Voir les vulnérabilités
npm audit

# Mettre à jour
npm update
```

### Fréquence et risques des mises à jour

| Dépendance | Fréquence de vérification | Risque de régression |
|------------|--------------------------|---------------------|
| Spring Boot (backend) | Mensuelle | **Élevé** — changement d'API, configuration, compatibilité Jakarta |
| Spring Security | Mensuelle | **Élevé** — failles de sécurité, modifications du flux d'auth |
| PostgreSQL | Trimestrielle | **Faible** — rétrocompatible, mais vérifier la compatibilité driver JDBC |
| Angular (frontend) | Mensuelle | **Moyen** — breaking changes entre versions majeures (ex: 20 → 21) |
| Dépendances npm | Mensuelle (npm audit) | **Faible à moyen** — correctifs de sécurité safe, majeures à vérifier |
| TypeScript | Trimestrielle | **Moyen** — nouvelles règles de typage, code existant peut ne plus compiler |

**Règles générales :**
- **Patch** (`x.y.z` → `x.y.z+1`) : appliquer sans crainte, correctif de bug uniquement
- **Mineur** (`x.y` → `x.y+1`) : tester rapidement, nouvelles fonctionnalités mais rétrocompatible
- **Majeur** (`x` → `x+1`) : tester complètement, breaking changes possibles

**Conduite à tenir :**
1. Consulter le changelog avant chaque mise à jour majeure
2. Exécuter la suite de tests complète après mise à jour (`mvn clean verify` + `ng test`)
3. En cas de breaking change, prévoir une migration dédiée (ne pas mélanger avec d'autres tâches)

### 2. Tests

**Backend**
```bash
cd backend
mvn clean verify          # Tests unitaires + intégration + JaCoCo coverage
```

**Frontend**
```bash
cd frontend
ng test --coverage         # Tests unitaires Vitest + coverage
npx cypress run            # Tests E2E (backend + frontend doivent tourner)
```

### 3. Base de données

**Sauvegarde :**
```bash
pg_dump -U postgres datashare > backup/datashare_$(date +%Y-%m-%d).sql
```

**Restauration :**
```bash
psql -U postgres datashare < backup/datashare_2026-01-01.sql
```

### 4. Maintenance corrective

**Problème courant — Port déjà utilisé :**
```bash
# WSL / Linux
sudo lsof -ti:8080 | xargs kill -9
sudo lsof -ti:4200 | xargs kill -9

# Windows PowerShell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

**Problème courant — Base de données :**
```bash
# Vérifier que PostgreSQL est en cours
systemctl status postgresql
# ou sur Windows
Get-Service postgresql*
```

**Problème courant — Upload échoue :**
- Vérifier la taille max du fichier : éditer `backend/src/main/resources/application.yaml` et regarder `spring.servlet.multipart.max-file-size` (actuellement 1024MB). Le fichier uploadé ne doit pas dépasser cette limite.
- Vérifier que le mot de passe fait au moins 6 caractères si le fichier est protégé
- Vérifier l'espace disque libre (là où les fichiers sont stockés)
- Vérifier les logs backend

### 5. Gestion de l'espace disque

Les fichiers uploadés sont stockés dans `backend/uploads/`. Chaque fichier a une expiration (max 7 jours). Après expiration, il n'est plus téléchargeable.

**Nettoyage automatique :** une tâche planifiée (`@Scheduled`) tourne chaque nuit à 3h du matin et supprime automatiquement les fichiers expirés du disque et de la base de données. Aucune intervention manuelle nécessaire.

**Nettoyage manuel si nécessaire :**
```bash
# Lister les fichiers expirés
docker exec datashare-db psql -U postgres -d datashare -c "SELECT id, file_name FROM files WHERE expires_at < NOW();"

# Supprimer les fichiers expirés
docker exec datashare-db psql -U postgres -d datashare -c "DELETE FROM files WHERE expires_at < NOW();"
```

### 6. Logs

**Backend :** `backend/logs/` (ou console)
**PostgreSQL :** `SELECT * FROM pg_stat_activity;` (connexions actives)

### 7. Déploiement rapide

```bash
# 1. Démarrer PostgreSQL
# 2. Backend
cd backend && mvn spring-boot:run &

# 3. Frontend
cd frontend && ng serve &

# 4. Accès
# Frontend : http://localhost:4200
# API : http://localhost:8080
```
