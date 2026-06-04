# Plan de tests — DataShare

## Plan de tests — Fonctionnalités critiques

| US | Fonctionnalité | Type de test | Critères d'acceptation | Statut |
|----|---------------|-------------|------------------------|--------|
| US03 | Création de compte | Unitaire / Intégration / E2E | Email valide ; mdp ≥ 8 car. ; email unique → 409 ; ok → 201 | ✅ |
| US04 | Connexion utilisateur | Unitaire / Intégration / E2E | Email + mdp corrects → JWT ; mdp incorrect → 401 | ✅ |
| US01 | Upload de fichier | Unitaire / Intégration / E2E | Fichier ≤ 1 Go ; mdp ≥ 6 car. ; expiration ≤ 7 jours | ✅ |
| US02 | Téléchargement via lien | Unitaire / Intégration / E2E | Lien valide → fichier téléchargé ; mdp requis si protégé | ✅ |
| US05 | Historique fichiers | Unitaire / Intégration / E2E | Liste fichier utilisateur connecté uniquement | ✅ |
| US06 | Suppression fichier | Unitaire / Intégration / E2E | Suppression complète ; absent après suppression | ✅ |

## Tests exécutables

### Backend — Tests unitaires et intégration (JUnit 5 + Mockito)

```bash
cd backend
mvn clean verify
```

**43 tests — tous passés** ✅

| Classe de test | Tests | Type |
|---------------|-------|------|
| AuthControllerTest | 6 | Intégration (MockMvc) |
| FileControllerTest | 12 | Intégration (MockMvc) |
| FileServiceTest | 15 | Unitaire |
| UserServiceTest | 5 | Unitaire |
| JwtServiceTest | 6 | Unitaire |
| BackendApplicationTests | 1 | Smoke |

### Frontend — Tests unitaires (Vitest)

```bash
cd frontend
ng test --coverage
```

**54 tests — tous passés** ✅

| Fichier de test | Tests | Couverture |
|----------------|-------|-----------|
| auth.service.spec.ts | 7 | 100% |
| file.service.spec.ts | 6 | 100% |
| auth.interceptor.spec.ts | 2 | 100% |
| app.spec.ts | 2 | 100% |
| login.spec.ts | 4 | 87.5% |
| register.spec.ts | 5 | 86.84% |
| dashboard.spec.ts | 16 | 91.5% |
| download.spec.ts | 12 | 96.49% |

### End-to-End — E2E (Cypress)

```bash
cd frontend
# Backend (port 8080) + Frontend (port 4200) doivent être en cours
npx cypress run
```

**7 tests — 100% de succès** ✅

| Scénario | Statut |
|----------|--------|
| Création de compte | ✅ |
| Échec création (email existant) | ✅ |
| Connexion | ✅ |
| Échec connexion (mauvais mot de passe) | ✅ |
| Upload de fichier avec mot de passe | ✅ |
| Consultation historique fichiers | ✅ |
| Suppression de fichier | ✅ |
## Rapport de couverture

**Objectif ≥ 70% atteint :**

- **Backend : 85.17%** (JaCoCo — instruction)
  - Rapport détaillé : [`backend/target/site/jacoco/index.html`](backend/target/site/jacoco/index.html)
- **Frontend : 92.61%** (Vitest)
  - Rapport détaillé : [`frontend/coverage/frontend/index.html`](frontend/coverage/frontend/index.html)
