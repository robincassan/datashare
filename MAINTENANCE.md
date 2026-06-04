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

### 5. Logs

**Backend :** `backend/logs/` (ou console)
**PostgreSQL :** `SELECT * FROM pg_stat_activity;` (connexions actives)

### 6. Déploiement rapide

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
