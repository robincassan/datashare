# Documentation technique — DataShare

## 1. Présentation

DataShare est une plateforme de partage de fichiers sécurisé. Elle permet à un utilisateur de s'inscrire, uploader un fichier, et générer un lien de téléchargement partageable, avec expiration et protection par mot de passe optionnelles.

## 2. Stack technique

| Couche | Technologie | Version |
|--------|------------|---------|
| Backend | Spring Boot | 3.5 |
| Frontend | Angular | 21 |
| Base de données | PostgreSQL | 16 |
| Authentification | JWT (jjwt) | 0.12.6 |
| Build backend | Maven | 3.9+ |
| Build frontend | Angular CLI / esbuild | — |
| Tests backend | JUnit 5 + Mockito | — |
| Tests frontend | Vitest | 4.x |
| Tests E2E | Cypress | 15.x |

## 3. Architecture globale

```
┌──────────────┐      HTTP/JSON      ┌──────────────┐      JDBC      ┌────────────┐
│   Frontend   │ ──────────────────→  │   Backend    │ ────────────→  │ PostgreSQL │
│   Angular 21 │ ←──────────────────  │ Spring Boot  │ ←────────────  │            │
│  :4200       │      JWT token       │  :8080       │                │  :5432     │
└──────────────┘                      └──────┬───────┘                └────────────┘
                                             │
                                             │ stockage
                                             ▼
                                      ┌──────────────┐
                                      │   uploads/    │
                                      │ (fichiers sur │
                                      │    disque)    │
                                      └──────────────┘
```

### Organisation du projet

```
datashare/
├── backend/                          # API REST Spring Boot
│   └── src/main/java/com/datashare/
│       ├── BackendApplication.java   # Point d'entrée
│       ├── AuthController.java       # Endpoints /api/auth
│       ├── AuthRequests.java         # DTOs RegisterRequest, LoginRequest
│       ├── FileController.java       # Endpoints /api/files
│       ├── FileResponse.java         # DTO de réponse fichier
│       ├── SecurityConfig.java       # Configuration Spring Security + JWT filter
│       ├── JwtService.java           # Génération et validation JWT
│       ├── UserService.java          # Logique d'authentification
│       ├── FileService.java          # Logique des fichiers (upload, download, nettoyage)
│       ├── StorageService.java       # Stockage sur disque
│       ├── User.java                 # Entité JPA (table users)
│       ├── UserRepository.java       # Accès base utilisateurs
│       ├── FileEntity.java           # Entité JPA (table files)
│       └── FileRepository.java       # Accès base fichiers
├── frontend/                         # Application Angular 21
│   └── src/app/
│       ├── app.ts / app.html         # Composant racine
│       ├── app.routes.ts             # Configuration des routes
│       ├── auth.interceptor.ts       # Injection JWT dans les requêtes HTTP
│       ├── services/
│       │   ├── auth.service.ts       # Appels API /api/auth
│       │   └── file.service.ts       # Appels API /api/files
│       └── pages/
│           ├── login/                # Page de connexion
│           ├── register/             # Page d'inscription
│           ├── dashboard/            # Upload + historique
│           └── download/             # Téléchargement via lien
└── docker-compose.yml                # Infrastructure PostgreSQL
```

## 4. Backend — Architecture par couches

### 4.1 Couche Contrôleur (`@RestController`)

Les contrôleurs exposent les endpoints REST et reçoivent les requêtes HTTP.

**AuthController** (`/api/auth`)
| Méthode | Endpoint | Rôle |
|---------|----------|------|
| POST | `/api/auth/register` | Création de compte |
| POST | `/api/auth/login` | Connexion (retourne JWT) |

**FileController** (`/api/files`)
| Méthode | Endpoint | Rôle |
|---------|----------|------|
| POST | `/api/files/upload` | Upload d'un fichier |
| GET | `/api/files` | Liste des fichiers de l'utilisateur |
| DELETE | `/api/files/{id}` | Suppression d'un fichier |
| GET | `/api/files/{token}` | Infos d'un fichier (lien public) |
| POST | `/api/files/{token}/download` | Téléchargement du fichier |

### 4.2 Couche Service (`@Service`)

**UserService** — Gère l'authentification :
- `register(email, password)` — vérifie l'unicité, hash le mot de passe (BCrypt), sauvegarde
- `login(email, password)` — vérifie les identifiants, retourne l'utilisateur

**FileService** — Gère le cycle de vie des fichiers :
- `upload(file, password, expiresAt, userId)` — valide, sauvegarde en base + sur disque
- `getUserFiles(userId)` — historique de l'utilisateur
- `deleteFile(fileId, userId)` — suppression (vérifie le propriétaire)
- `getFileByToken(token)` — recherche par token de téléchargement
- `getFilePath(token, password)` — vérifie le mot de passe et retourne le chemin
- `cleanupExpiredFiles()` — tâche planifiée (@Scheduled, quotidien à 3h)

**StorageService** — Stockage physique :
- `save(file, fileId)` — copie le fichier uploadé vers `uploads/{uuid}`
- `getPath(fileId)` — retourne le chemin absolu
- `delete(fileId)` — supprime le fichier du disque

**JwtService** — Gestion des tokens JWT :
- `generateToken(userId)` — crée un token signé avec expiration
- `extractUserId(token)` — extrait l'utilisateur depuis le token
- `isTokenValid(token)` — vérifie la signature et l'expiration

### 4.3 Couche Repository (`@Repository`)

Interfaces Spring Data JPA qui génèrent automatiquement les requêtes SQL :

**UserRepository**
- `findByEmail(email)` — recherche par email (login)

**FileRepository**
- `findByUserIdOrderByCreatedAtDesc(userId)` — historique
- `findByExpiresAtBefore(dateTime)` — fichiers expirés (nettoyage)
- `findByDownloadToken(token)` — recherche par token
- `findByIdAndUserId(id, userId)` — vérification propriétaire

### 4.4 Entités JPA

**User** (table `users`)
| Champ | Type | Contrainte |
|-------|------|-----------|
| id | String (UUID) | @Id, auto-généré |
| email | String | @Column(unique), obligatoire |
| password | String | Hash BCrypt, obligatoire |
| createdAt | LocalDateTime | Rempli automatiquement |

**FileEntity** (table `files`)
| Champ | Type | Contrainte |
|-------|------|-----------|
| id | String (UUID) | @Id, auto-généré = nom sur le disque |
| fileName | String | Nom d'origine du fichier |
| fileSize | Long | Taille en octets |
| mimeType | String | Type MIME (image/png, etc.) |
| password | String | Hash BCrypt (null si non protégé) |
| expiresAt | LocalDateTime | Date d'expiration |
| downloadToken | String | Token unique pour le lien public |
| userId | String | Propriétaire du fichier |
| createdAt | LocalDateTime | Rempli automatiquement |

### 4.5 Sécurité (Spring Security)

```
Requête HTTP → SecurityFilterChain
                ├── /api/auth/** → permis à tous
                ├── GET /api/files/{token} → permis à tous
                ├── /api/files/{token}/download → permis à tous
                └── autres → authentification requise
                        │
                        ▼
                JwtAuthenticationFilter (OncePerRequestFilter)
                        │
                        ├── Extrait le header "Authorization: Bearer <token>"
                        ├── Valide le token via JwtService
                        ├── Extrait le userId
                        └── Ajoute userId dans les attributs de la requête
```

Le filtre JWT est ajouté avant `UsernamePasswordAuthenticationFilter` et ne s'applique pas aux endpoints `/api/auth/*` (inscription et connexion).

CORS : ouvert à toutes les origines en développement.

BCryptPasswordEncoder est utilisé pour le hash des mots de passe (12 rounds par défaut).

## 5. Frontend

### 5.1 Routing

```typescript
/login          → LoginComponent       (lazy)
/register       → RegisterComponent    (lazy)
/dashboard      → DashboardComponent   (lazy, nécessite JWT)
/download/:token→ DownloadComponent    (lazy, public)
/               → redirige vers /login
```

Toutes les routes sont en lazy loading. Angular ne charge le code d'une page que lorsque l'utilisateur y navigue.

### 5.2 Services

**AuthService** — appelle `/api/auth` :
- `register(email, password)` → POST /api/auth/register
- `login(email, password)` → POST /api/auth/login → stocke le JWT dans localStorage
- `getToken()` → lit le JWT depuis localStorage
- `isLoggedIn()` → vérifie si un token existe
- `logout()` → supprime le JWT

**FileService** — appelle `/api/files` :
- `upload(formData)` → POST /api/files/upload
- `list()` → GET /api/files
- `delete(id)` → DELETE /api/files/{id}
- `getInfo(token)` → GET /api/files/{token}
- `download(token, password?)` → POST /api/files/{token}/download

### 5.3 Intercepteur HTTP

`authInterceptor` ajoute automatiquement le header `Authorization: Bearer <token>` à chaque requête HTTP. Si le token n'existe pas, la requête part sans header (endpoints publics).

### 5.4 Pages

**Login** — formulaire email + mot de passe, appelle AuthService.login, redirige vers /dashboard
**Register** — formulaire email + mot de passe (≥ 8 caractères), appel AuthService.register
**Dashboard** — upload de fichier (avec mot de passe et expiration optionnels), liste des fichiers, copie du lien, suppression
**Download** — récupère les infos du fichier par token, demande le mot de passe si nécessaire, télécharge le fichier

## 6. Base de données

### Schéma relationnel

```
┌─────────────────────┐          ┌──────────────────────────┐
│       users         │          │         files            │
├─────────────────────┤          ├──────────────────────────┤
│ id        (PK, UUID)│          │ id           (PK, UUID)  │
│ email     (UNIQUE)  │          │ file_name                │
│ password  (BCrypt)  │          │ file_size                │
│ created_at          │          │ mime_type                │
└─────────────────────┘          │ password     (BCrypt)    │
                                 │ expires_at               │
                                 │ download_token (UNIQUE)  │
                                 │ user_id      (FK → users)│
                                 │ created_at               │
                                 └──────────────────────────┘
```

Les tables sont créées automatiquement par Hibernate (`ddl-auto: update`). Les colonnes `id` utilisent `GenerationType.UUID` — chaque enregistrement reçoit un UUID unique.

### Stockage des fichiers

Les fichiers sont stockés sur le disque dans `backend/uploads/` avec l'UUID comme nom. La base de données fait le lien entre l'UUID (nom sur le disque) et le nom d'origine, la taille, le type, etc. Cette séparation évite les collisions de noms et les attaques par chemin.

## 7. Flux détaillés

### 7.1 Inscription

```
Client                    Backend                     PostgreSQL
  │                         │                            │
  │── POST /api/auth/register                          │
  │   {email, password}     │                            │
  │                         ├── Vérifie email unique ──→ │
  │                         ├── Hash BCrypt (12 rounds)  │
  │                         ├── Création User ──────────→│
  │                         │                            │
  │←── 201 {id, email}      │                            │
```

### 7.2 Connexion (JWT)

```
Client                    Backend                     PostgreSQL
  │                         │                            │
  │── POST /api/auth/login                              │
  │   {email, password}     │                            │
  │                         ├── findByEmail(email) ────→ │
  │                         ├── passwordEncoder.matches()│
  │                         ├── jwtService.generateToken │
  │                         │                            │
  │←── 200 {token: "jwt"}   │                            │
  │                         │                            │
  │── Stocke le token dans                               │
  │    localStorage                                      │
```

### 7.3 Upload

```
Client                    Backend                     Disque
  │                         │                            │
  │── POST /api/files/upload                            │
  │   Authorization: Bearer jwt                          │
  │   (multipart)          │                            │
  │                         ├── JwtFilter → userId       │
  │                         ├── FileService.upload()     │
  │                         │   ├── Valide taille (1 Go) │
  │                         │   ├── Hash mot de passe    │
  │                         │   ├── Génère downloadToken │
  │                         │   ├── FileEntity ──────→ DB│
  │                         │   └── storageService.save()│
  │                         │        └── Copie fichier ─→│
  │                         │                            │
  │←── 201 {id, fileName,                               │
  │         downloadLink}   │                            │
```

### 7.4 Téléchargement via lien

```
Destinataire            Backend                     Disque
  │                         │                            │
  │── GET /api/files/{token}                             │
  │                         ├── findByDownloadToken()     │
  │                         ├── Vérifie expiration       │
  │                         │                            │
  │←── 200 {fileName,                                    │
  │         hasPassword}    │                            │
  │                         │                            │
  │── POST /api/files/{token}/download                   │
  │   {password?}           │                            │
  │                         ├── Vérifie mot de passe     │
  │                         ├── Lit fichier depuis disque│
  │                         │        └── Lit fichier ───→│
  │                         │                            │
  │←── 200 (fichier en                               │
  │         attachement)    │                            │
```

### 7.5 Nettoyage automatique (quotidien)

```
@Scheduled(cron = "0 0 3 * * *")
FileService.cleanupExpiredFiles()
  │
  ├── fileRepository.findByExpiresAtBefore(now)
  │       → requête SQL: WHERE expires_at < ?
  │
  ├── Pour chaque fichier expiré :
  │   ├── storageService.delete(fileId) → supprime du disque
  │   └── fileRepository.delete(file)   → supprime de la base
  │
  └── Log du résultat
```

Cette tâche s'exécute tous les jours à 3h du matin.

## 8. Tests

### Backend (43 tests, 85% couverture JaCoCo)

| Test | Type | Description |
|------|------|-------------|
| AuthControllerTest | Intégration | Inscription, connexion, doublons |
| FileControllerTest | Intégration | Upload, historique, suppression, accès anonyme |
| FileServiceTest | Unitaire | Upload, validation, expiration, mot de passe |
| UserServiceTest | Unitaire | Register, login, email déjà utilisé |
| JwtServiceTest | Unitaire | Génération et validation des tokens |
| BackendApplicationTests | Smoke | Contexte Spring Boot se charge |

### Frontend (54 tests, 93% couverture Vitest)

| Test | Description |
|------|-------------|
| AuthService | Register, login, token storage |
| FileService | Upload, list, delete, download |
| LoginComponent | Connexion, validation, erreurs |
| RegisterComponent | Inscription, validation |
| DashboardComponent | Upload, liste, suppression, copie lien |
| DownloadComponent | Récupération infos, téléchargement |
| AuthInterceptor | Injection du header JWT |

### E2E (7 scénarios Cypress)

Inscription, connexion, upload, téléchargement via lien, mot de passe incorrect, fichier expiré.

## 9. Performances

| Métrique | Valeur |
|----------|--------|
| Bundle initial frontend | 78.69 kB (transfer) |
| Budget Angular (warning/error) | 500 kB / 1 MB |
| Temps de réponse API (p95) | 97 ms |
| Upload max | 1 Go |
| Expiration max | 7 jours |
| Nettoyage automatique | Quotidien (3h) |

## 10. Maintenance

Les procédures de maintenance sont détaillées dans [MAINTENANCE.md](../MAINTENANCE.md) :
- Mise à jour des dépendances (Maven / npm)
- Sauvegarde et restauration PostgreSQL
- Gestion de l'espace disque (nettoyage automatique des fichiers expirés)
- Résolution des problèmes courants (port utilisé, upload échoue)

## 11. Utilisation de l'IA dans le développement

### Rôle de l'IA

L'IA (Claude Code) a été utilisée comme **co-pilote de développement** sur l'**US01 — Upload de fichier**, ainsi que pour des tâches transverses tout au long du projet.

### US01 confiée à l'IA

L'US01 a été développée intégralement par l'IA :

| Fichier | Rôle |
|---------|------|
| `FileEntity.java` | Entité JPA (nom, taille, type MIME, mot de passe hashé, expiration, token) |
| `FileRepository.java` | Accès base de données (recherche par utilisateur, token, id) |
| `StorageService.java` | Stockage et suppression des fichiers sur le disque |
| `FileService.java` | Logique métier (validation, hash, parsing date, rollback) |
| `FileController.java` | Endpoint REST `POST /api/files/upload` |

### Méthode de travail avec l'IA

Le développement s'est fait par **itérations courtes** : l'IA proposait une implémentation, je la validais, testais, et demandais des ajustements si nécessaire. Les prompts étaient formulés en français, sous forme de spécifications fonctionnelles (ex : "implémente l'upload de fichier avec validation de taille, mot de passe optionnel et date d'expiration").

### Supervision et vérifications humaines

Chaque livrable de l'IA a été contrôlé :

| Vérification | Exemple |
|-------------|---------|
| **Architecture** | Séparation FileService (métier) / StorageService (disque) validée |
| **Sécurité** | Correction du SecurityContext Spring (le filtre JWT ne remplissait pas l'authentification) |
| **Contrat d'interface** | Création du record `FileResponse` pour ne pas exposer l'entité JPA complète |
| **Codes HTTP** | Correction 400 → 409 pour email déjà utilisé |
| **CORS** | Passage de `setAllowedOrigins` à `setAllowedOriginPatterns` |

### Utilisation transverse

Au-delà de l'US01, l'IA a assisté sur :
- **Tests** : génération des tests unitaires JUnit/Vitest et analyse des rapports de couverture
- **Débogage** : analyse des logs d'erreur (dépendances, configuration, CORS)
- **Infrastructure** : création du docker-compose.yml, script de déploiement setup.sh
- **Qualité** : exécution OWASP Dependency Check, configuration des budgets de performance Angular

## 12. Documentation d'API

### Authentification

#### `POST /api/auth/register` — Création de compte

**Corps (JSON) :**
```json
{
  "email": "user@example.com",
  "password": "monMotDePasse123"
}
```

**Contraintes :** email valide, password ≥ 8 caractères.

**Réponses :**
| Code | Description |
|------|-------------|
| 201 | Compte créé — retourne `{id, email}` |
| 409 | Email déjà utilisé |

#### `POST /api/auth/login` — Connexion

**Corps (JSON) :**
```json
{
  "email": "user@example.com",
  "password": "monMotDePasse123"
}
```

**Réponses :**
| Code | Description |
|------|-------------|
| 200 | Token JWT — retourne `{token: "jwt..."}` |
| 401 | Identifiants incorrects |

### Fichiers (authentification requise)

Tous les endpoints ci-dessous nécessitent le header : `Authorization: Bearer <token>`

#### `POST /api/files/upload` — Upload d'un fichier

**Paramètres (multipart/form-data) :**
| Paramètre | Type | Obligatoire | Description |
|-----------|------|-------------|-------------|
| file | File | Oui | Fichier à uploader (max 1 Go) |
| password | String | Non | Mot de passe (min 6 car.) |
| expiresAt | String | Non | Date ISO (max 7 jours) |

**Réponse (201) :**
```json
{
  "id": "uuid",
  "fileName": "document.pdf",
  "fileSize": 1024000,
  "expiresAt": "2026-06-10T00:00:00Z",
  "downloadLink": "http://localhost:8080/api/files/token/download"
}
```

#### `GET /api/files` — Historique des fichiers

**Réponse (200) :** tableau d'objets :
```json
[{
  "id": "uuid",
  "fileName": "document.pdf",
  "fileSize": 1024000,
  "expiresAt": "2026-06-10T00:00:00Z",
  "status": "ACTIVE",
  "downloadToken": "token-unique"
}]
```

#### `DELETE /api/files/{id}` — Suppression

**Réponses :** 204 (succès), 404 (introuvable), 401 (non authentifié)

### Fichiers (publics)

#### `GET /api/files/{token}` — Informations du fichier

**Réponse (200) :**
```json
{
  "fileName": "document.pdf",
  "fileSize": 1024000,
  "expiresAt": "2026-06-10T00:00:00Z",
  "hasPassword": true,
  "expired": false
}
```

#### `POST /api/files/{token}/download` — Téléchargement

**Header :** `Content-Type: application/json`

**Corps (optionnel si protégé) :**
```json
{
  "password": "mot-de-passe"
}
```

**Réponse :** Fichier en attachement (Content-Disposition: attachment).
