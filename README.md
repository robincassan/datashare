# DataShare

Plateforme de partage de fichiers sécurisé avec authentification JWT, expiration des liens et protection par mot de passe.

## Architecture

```
datashare/
├── backend/           # API REST Spring Boot (Java 21)
├── frontend/          # Application Angular 21
├── scripts/           # Scripts de déploiement
├── docs/              # Documentation technique
├── docker-compose.yml
├── TESTING.md         # Plan de tests et couverture
├── SECURITY.md        # Scan de vulnérabilités
├── PERF.md            # Test de performance
└── MAINTENANCE.md     # Procédures de maintenance
```

## Prérequis

- **Java 21+** et **Maven 3.9+**
- **Node.js 24+** et **npm 11+**
- **Docker Desktop** (ou PostgreSQL 16+)
- **Angular CLI 21+** : `npm install -g @angular/cli`

## Installation

### 1. Base de données

```bash
docker compose up -d
```

### 2. Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

L'API est accessible sur `http://localhost:8080`.

### 3. Frontend

```bash
cd frontend
npm install
ng serve
```

L'application est accessible sur `http://localhost:4200`.

### Installation rapide (tout-en-un)

```bash
bash scripts/setup.sh
```

## Utilisation

1. **Inscription** — Créez un compte avec email + mot de passe (≥ 8 caractères)
2. **Connexion** — Connectez-vous avec vos identifiants
3. **Upload** — Sélectionnez un fichier (max 1 Go), optionnel : mot de passe + date d'expiration
4. **Téléchargement** — Partagez le lien généré. Si protégé par mot de passe, le destinataire devra le saisir
5. **Historique** — Consultez et supprimez vos fichiers

## Tests

```bash
# Backend (43 tests — unitaires + intégration)
cd backend && mvn clean verify

# Frontend (54 tests — Vitest)
cd frontend && ng test --coverage

# End-to-End (7 scénarios — Cypress)
cd frontend && npx cypress run
```

Couverture : Backend **85%** (JaCoCo), Frontend **93%** (Vitest).

## API

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/auth/register` | Création de compte |
| POST | `/api/auth/login` | Connexion (retourne un JWT) |
| POST | `/api/files/upload` | Upload d'un fichier |
| GET | `/api/files/download/{token}` | Téléchargement via lien |
| GET | `/api/files` | Historique des fichiers |
| DELETE | `/api/files/{id}` | Suppression d'un fichier |

## Technologies

- **Backend** : Spring Boot 3.5, Spring Security, JPA/Hibernate, JWT (jjwt 0.12.6), PostgreSQL
- **Frontend** : Angular 21, TypeScript, Vitest, Cypress
- **Base de données** : PostgreSQL 16 (Docker)
