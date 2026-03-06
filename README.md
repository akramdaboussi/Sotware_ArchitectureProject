# Système d'Authentification - Software Architecture - Groupe 11
Belhout Oussama
Daboussi Akram

---

## Table des matières

1. [Présentation du projet](#présentation-du-projet)
2. [Outils et frameworks](#outils-et-frameworks)
3. [Installation et Démarrage](#installation-et-démarrage)
4. [Structure du projet](#structure-du-projet)
5. [Architecture du système](#architecture-du-système)
6. [Fonctionnalités implémentées](#fonctionnalités-implémentées)
7. [API REST](#api-rest)
8. [Exemples curl / Postman](#exemples-curl--postman)
9. [Schéma de base de données](#schéma-de-base-de-données)
10. [Logging](#logging)

---

## Présentation du projet

Ce projet implémente un système d'authentification moderne suivant les bonnes pratiques d'architecture logicielle :

- **Architecture en couches** : séparation claire Controller → Service → Repository
- **Communication asynchrone** : découplage via RabbitMQ (Event-Driven Architecture)
- **Authentification stateless** : tokens JWT sans état côté serveur
- **Sécurité renforcée** : hachage des mots de passe, tokens de vérification sécurisés
- **Permissions granulaires** : contrôle d'accès fin par permissions

---

## Outils et frameworks

| Composant | Technologie | Version |
|-----------|-------------|---------|
| **Framework** | Spring Boot | 3.4.1 |
| **Sécurité** | Spring Security + JWT | - |
| **Base de données** | H2 (fichier) | - |
| **ORM** | JPA / Hibernate | - |
| **Messagerie** | RabbitMQ (AMQP) | 3.x |
| **Email** | JavaMailSender + MailHog | - |
| **Build** | Maven | - |
| **Frontend** | HTML/CSS/JS vanilla | - |

---

## Installation et Démarrage

### Prérequis

- **Java 17+** : `java -version`
- **Maven** : inclus via `./mvnw` (Maven Wrapper)
- **Docker** : `docker --version` et `docker-compose --version`

### Installation Docker (WSL)

```bash
# Mettre à jour les paquets
sudo apt update && sudo apt upgrade -y

# Installer Docker
sudo apt install -y docker.io docker-compose

# Ajouter l'utilisateur au groupe docker (évite d'utiliser sudo)
sudo usermod -aG docker $USER

# Redémarrer le terminal ou appliquer les changements
newgrp docker

# Vérifier l'installation
docker --version
docker-compose --version
```

### Démarrage

```bash
# 1. Démarrer les services externes (RabbitMQ + MailHog)
docker-compose up -d

# 2. Lancer l'application Spring Boot
./mvnw spring-boot:run
```

### Commandes utiles

```bash
# Arrêter les services Docker
docker-compose down

# Arrêter et supprimer les volumes (reset complet RabbitMQ/MailHog)
docker-compose down -v

# Lancer les tests unitaires
./mvnw test

# Compiler le projet (génère target/archi-0.0.1-SNAPSHOT.jar)
./mvnw clean package

# Lancer le JAR compilé (sans Maven)
java -jar target/archi-0.0.1-SNAPSHOT.jar

# Reset de la base de données H2
rm -rf data/

# Voir les logs des containers Docker
docker-compose logs -f rabbitmq
docker-compose logs -f mailhog
```

### URLs des services

| Service | URL | Identifiants |
|---------|-----|--------------|
| **Application** | http://localhost:8080 | - |
| **Console H2** | http://localhost:8080/h2-console | JDBC: `jdbc:h2:file:./data/authdb`, User: `sa` |
| **MailHog** (emails) | http://localhost:8025 | - |
| **RabbitMQ** (admin) | http://localhost:15672 | guest / guest |

### Compte administrateur

Un **super admin** est créé automatiquement au premier démarrage avec toutes les permissions :

| Champ | Valeur |
|-------|--------|
| **Email** | `admin@admin.com` |
| **Mot de passe** | `admin123` |
| **Permissions** | Toutes (14 permissions) |

Ce compte permet de :
- Voir tous les utilisateurs (`GET /api/admin/users`)
- Ajouter des permissions (`POST /api/admin/add-permission`)
- Retirer des permissions (`POST /api/admin/remove-permission`)

---

## Structure du projet

```
src/main/java/com/softwarearchi/archi/
├── ArchiApplication.java          # Point d'entrée Spring Boot
│
├── config/
│   ├── SecurityConfig.java        # Configuration Spring Security + filtre JWT
│   ├── RabbitMQConfig.java        # Exchanges, queues, bindings RabbitMQ
│   └── DataInitializer.java       # Création des permissions par défaut au démarrage
│
├── controllers/
│   ├── AuthController.java        # Endpoints /api/auth/*
│   └── AdminController.java       # Endpoints /api/admin/* (protégés)
│
├── services/
│   ├── AuthService.java           # Logique d'authentification et vérification
│   ├── UserService.java           # Gestion des utilisateurs et mots de passe
│   └── NotificationService.java   # Consumer RabbitMQ, envoi d'emails
│
├── repository/
│   ├── UserRepository.java        # Accès table users
│   ├── PermissionRepository.java  # Accès table permissions
│   ├── VerificationTokenRepository.java  # Accès table verification_tokens
│   └── JwtTokenRepository.java    # Accès table jwt_tokens (révocation)
│
├── models/
│   ├── User.java                  # Entité JPA utilisateur
│   ├── Permission.java            # Entité JPA permission
│   ├── VerificationToken.java     # Entité JPA token de vérification
│   └── JwtToken.java              # Entité JPA token JWT (stockage en BDD)
│
├── events/
│   └── UserRegisteredEvent.java   # DTO événement inscription
│
└── utils/
    └── JwtUtil.java               # Génération et validation JWT

src/main/resources/
├── application.properties         # Configuration Spring
└── static/
    ├── index.html                 # Interface utilisateur
    ├── styles.css                 # Styles avec thème glassmorphism
    └── app.js                     # Logique frontend + animation canvas
```

---

## Architecture du système

### Vue d'ensemble

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Client    │───▶│  Controller │───▶│   Service    │───▶│ Repository  │
│  (Browser)  │     │  (REST API) │     │  (Métier)   │     │   (JPA)     │
└─────────────┘     └─────────────┘     └─────────────┘     └──────┬──────┘
                                               │                   │
                                               ▼                   ▼
                                        ┌─────────────┐     ┌─────────────┐
                                        │  RabbitMQ   │     │  H2 Database│
                                        │  (Events)   │     │  (Stockage) │
                                        └──────┬──────┘     └─────────────┘
                                               │
                                               ▼
                                        ┌─────────────┐     ┌─────────────┐
                                        │ Notification│───▶│   MailHog   │
                                        │  Service    │     │  (SMTP Dev) │
                                        └─────────────┘     └─────────────┘
```

### Principe des couches

| Couche | Responsabilité | Fichiers |
|--------|----------------|----------|
| **Controller** | Réception HTTP, validation des entrées, formatage des réponses | `AuthController`, `AdminController` |
| **Service** | Logique métier, orchestration, règles de gestion | `AuthService`, `UserService`, `NotificationService` |
| **Repository** | Accès aux données via JPA, requêtes SQL | `UserRepository`, `RoleRepository`, `VerificationTokenRepository` |
| **Config** | Configuration Spring, sécurité, messagerie | `SecurityConfig`, `RabbitMQConfig`, `DataInitializer` |

**Pourquoi cette séparation ?**
- **Testabilité** : chaque couche peut être testée indépendamment
- **Maintenabilité** : modifications localisées sans impact global
- **Réutilisabilité** : les services peuvent être appelés par différents controllers

---

## Fonctionnalités implémentées

### 1. Authentification JWT (avec stockage en BDD)

L'authentification utilise des **JSON Web Tokens** avec **stockage en base de données** pour permettre la révocation :

**Principe** :
- À la connexion, le serveur génère un JWT signé contenant l'ID utilisateur et ses rôles
- Le token est **sauvegardé en base de données** (table `jwt_tokens`)
- Le client stocke ce token et l'envoie dans l'en-tête `Authorization: Bearer <token>`
- Le serveur valide la signature **ET vérifie que le token existe en base** (non révoqué)

**Avantages du stockage en BDD** :
- **Vrai logout** : le token est marqué comme révoqué en base
- **Gestion des sessions** : possibilité de voir et révoquer toutes les sessions actives
- **Sécurité renforcée** : possibilité de forcer la déconnexion d'un utilisateur

**Configuration** (`JwtUtil.java`) :
- Algorithme : HS256 (HMAC-SHA256)
- Durée de validité : 24 heures
- Claims inclus : email (subject), userId, permissions

**Flux de validation** :
```
1. Requête avec Authorization: Bearer <token>
   └─▶ JwtAuthFilter extrait le token
   └─▶ Valide la signature JWT (JwtUtil)
   └─▶ Vérifie en base : token existe ET non révoqué (AuthService.isTokenValid)
   └─▶ Si valide → authentification réussie

2. POST /logout
   └─▶ AuthService.logout(token)
   └─▶ Marque le token comme revoked=true en base
   └─▶ Les requêtes suivantes avec ce token seront rejetées
```

### 2. Vérification d'email asynchrone

Le système implémente une vérification d'email découplée via RabbitMQ :

**Flux complet** :
```
1. POST /register
   └─▶ AuthService crée l'utilisateur (verified=false)
   └─▶ Génère tokenId (public) + tokenClear (secret)
   └─▶ Stocke tokenId + hash(tokenClear) en base
   └─▶ Publie UserRegisteredEvent sur RabbitMQ

2. RabbitMQ
   └─▶ Exchange "auth.events" route vers queue "notification.user-registered"
   └─▶ En cas d'échec répété → Dead Letter Queue (DLQ)

3. NotificationService (@RabbitListener)
   └─▶ Consomme l'événement
   └─▶ Construit le lien : /verify?tokenId=xxx&t=secret
   └─▶ Envoie l'email via JavaMailSender → MailHog

4. GET /verify?tokenId=xxx&t=secret
   └─▶ AuthService récupère le token par tokenId
   └─▶ Vérifie expiration (15 min) et hash BCrypt
   └─▶ Passe l'utilisateur en verified=true
   └─▶ Supprime le token (usage unique)
```

**Sécurité du token** :
- `tokenId` : identifiant public visible dans l'URL
- `tokenClear` : secret envoyé par email, jamais stocké en clair
- `tokenHash` : hash BCrypt stocké en base → même si la DB est compromise, les tokens sont inutilisables

**Pourquoi asynchrone ?**
- Si l'envoi d'email échoue, l'inscription n'est pas bloquée
- Le système reste réactif même si MailHog est lent
- Les messages échoués sont conservés dans la DLQ pour retry manuel

### 3. Contrôle d'accès par permissions

Le système implémente un contrôle d'accès granulaire par permissions :

**Permissions disponibles** :

| Catégorie | Permission | Description |
|-----------|------------|-------------|
| **Base** | `READ_PROFILE` | Lire son propre profil |
| | `EDIT_PROFILE` | Modifier son propre profil |
| | `DELETE_ACCOUNT` | Supprimer son propre compte |
| **Gestion** | `READ_USERS` | Voir la liste des utilisateurs |
| | `MANAGE_USERS` | Modifier des utilisateurs |
| | `DELETE_USERS` | Supprimer des utilisateurs |
| | `MANAGE_PERMISSIONS` | Attribuer des permissions |
| **Admin** | `ADMIN` | Accès administrateur complet |

**Permissions par défaut** :
- À l'inscription, un utilisateur reçoit : `READ_PROFILE`, `EDIT_PROFILE`, `DELETE_ACCOUNT`
- Les autres permissions doivent être attribuées par un admin/utilisateur avec `MANAGE_PERMISSIONS`

**Matrice des permissions** :
| Endpoint | Permission requise |
|----------|-------------------|
| `/api/auth/register` | Aucune (public) |
| `/api/auth/login` | Aucune (public) |
| `/api/auth/me` (GET) | Authentifié |
| `/api/auth/me` (DELETE) | `DELETE_ACCOUNT` ou `ADMIN` |
| `/api/admin/users` (GET) | `READ_USERS` ou `ADMIN` |
| `/api/admin/users/{id}` (DELETE) | `DELETE_USERS` ou `ADMIN` |
| `/api/admin/add-permission` | `MANAGE_PERMISSIONS` ou `ADMIN` |
| `/api/admin/remove-permission` | `MANAGE_PERMISSIONS` ou `ADMIN` |

**Implémentation** :
- Les permissions sont stockées en base (table `permissions`) et liées aux utilisateurs (table `user_permissions`)
- Le JWT contient la liste des permissions de l'utilisateur
- `AdminController` vérifie la permission appropriée avant chaque opération

### 4. Hachage sécurisé des mots de passe

Deux algorithmes de hachage sont utilisés selon le contexte :

| Usage | Algorithme | Justification |
|-------|------------|---------------|
| Mots de passe utilisateur | SHA-256 + Base64 | Simple, intégré à Java |
| Tokens de vérification | BCrypt | Résistant aux attaques par force brute |

> **Note** : En production, il faudrait utiliser BCrypt pour les mots de passe également.

---

## API REST

### Endpoints publics (sans authentification)

```http
POST /api/auth/register
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "password": "secret123",
  "phoneNumber": "0612345678"
}

→ 201 Created { "token": "eyJhbG...", "email": "john@example.com" }
```

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "secret123"
}

→ 200 OK { "token": "eyJhbG...", "message": "Login successful" }
```

```http
GET /api/auth/verify?tokenId=abc123&t=secretToken

→ 200 OK { "message": "Email successfully verified!" }
```

### Endpoints authentifiés (JWT requis)

```http
GET /api/auth/me
Authorization: Bearer eyJhbG...

→ 200 OK { "id": 1, "email": "john@example.com", "verified": true, ... }
```

```http
POST /api/auth/logout
Authorization: Bearer eyJhbG...

→ 200 OK { "message": "Logout successful" }
```

```http
DELETE /api/auth/me
Authorization: Bearer eyJhbG...

→ 200 OK { "message": "Account deleted successfully" }
```

### Endpoints admin (permissions spécifiques requises)

```http
GET /api/admin/users
Authorization: Bearer eyJhbG...

→ 200 OK [{ "id": 1, "email": "...", "permissions": ["READ_PROFILE", "EDIT_PROFILE"] }, ...]
```

```http
POST /api/admin/add-permission
Authorization: Bearer eyJhbG...
Content-Type: application/json

{ "email": "john@example.com", "permission": "MANAGE_USERS" }

→ 200 OK { "message": "Permission MANAGE_USERS added to john@example.com" }
```

```http
POST /api/admin/remove-permission
Authorization: Bearer eyJhbG...
Content-Type: application/json

{ "email": "john@example.com", "permission": "MANAGE_USERS" }

→ 200 OK { "message": "Permission MANAGE_USERS removed from john@example.com" }
```

```http
DELETE /api/admin/users/5
Authorization: Bearer eyJhbG...

→ 200 OK { "message": "User deleted successfully" }
```

---

## Exemples curl / Postman

### Inscription

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "password": "secret123",
    "phoneNumber": "0612345678"
  }'
```

### Connexion

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "john@example.com", "password": "secret123"}'
```

### Connexion admin

```bash
# Se connecter avec le super admin créé automatiquement
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@admin.com", "password": "admin123"}'

# Réponse : {"token": "eyJhbG...", "message": "Login successful"}
# Utiliser ce token pour les requêtes /api/admin/*
```

### Récupérer le profil (avec JWT)

```bash
# Remplacer <TOKEN> par le JWT reçu lors du login
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <TOKEN>"
```

### Liste des utilisateurs (admin)

```bash
curl -X GET http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer <TOKEN_ADMIN>"
```

### Ajouter une permission (admin)

```bash
curl -X POST http://localhost:8080/api/admin/add-permission \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN_ADMIN>" \
  -d '{"email": "john@example.com", "permission": "MANAGE_USERS"}'
```

### Retirer une permission (admin)

```bash
curl -X POST http://localhost:8080/api/admin/remove-permission \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN_ADMIN>" \
  -d '{"email": "john@example.com", "permission": "MANAGE_USERS"}'
```

### Supprimer un utilisateur (admin)

```bash
curl -X DELETE http://localhost:8080/api/admin/users/5 \
  -H "Authorization: Bearer <TOKEN_ADMIN>"
```

### Supprimer son propre compte (auto-résiliation)

```bash
curl -X DELETE http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <TOKEN>"
```

### Vérification email

```bash
# Récupérer tokenId et t depuis l'email reçu dans MailHog
curl -X GET "http://localhost:8080/api/auth/verify?tokenId=abc123&t=secretToken"
```

### Collection Postman

Pour importer dans Postman, créer une collection avec les requêtes suivantes :

| Requête | Méthode | URL | Body/Headers |
|---------|---------|-----|-------------|
| Register | POST | `{{base_url}}/api/auth/register` | JSON body |
| Login | POST | `{{base_url}}/api/auth/login` | JSON body |
| Me | GET | `{{base_url}}/api/auth/me` | Header: `Authorization: Bearer {{token}}` |
| Delete Account | DELETE | `{{base_url}}/api/auth/me` | Header: `Authorization: Bearer {{token}}` |
| Users | GET | `{{base_url}}/api/admin/users` | Header: `Authorization: Bearer {{token}}` |
| Delete User | DELETE | `{{base_url}}/api/admin/users/:id` | Header: `Authorization: Bearer {{token}}` |
| Add Permission | POST | `{{base_url}}/api/admin/add-permission` | JSON body + Auth header |
| Remove Permission | POST | `{{base_url}}/api/admin/remove-permission` | JSON body + Auth header |

**Variables d'environnement Postman** :
- `base_url` : `http://localhost:8080`
- `token` : JWT reçu après login (à mettre à jour manuellement ou via script)

---

## Schéma de base de données

```sql
-- Table des utilisateurs
users (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  email         VARCHAR(255) UNIQUE NOT NULL,
  password      VARCHAR(255) NOT NULL,  -- Hash SHA-256
  first_name    VARCHAR(255) NOT NULL,
  last_name     VARCHAR(255) NOT NULL,
  phone_number  VARCHAR(255),
  enabled       BOOLEAN NOT NULL DEFAULT TRUE,
  verified      BOOLEAN NOT NULL DEFAULT FALSE,
  created_at    TIMESTAMP,
  updated_at    TIMESTAMP
)

-- Table des permissions
permissions (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  name        VARCHAR(255) UNIQUE NOT NULL,  -- Ex: ADMIN, MANAGE_USERS
  description VARCHAR(255)
)

-- Association utilisateurs-permissions (Many-to-Many)
user_permissions (
  user_id       BIGINT REFERENCES users(id),
  permission_id BIGINT REFERENCES permissions(id),
  PRIMARY KEY (user_id, permission_id)
)

-- Tokens de vérification email
verification_tokens (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  token_id    VARCHAR(255) UNIQUE NOT NULL,  -- ID public
  token_hash  VARCHAR(255) NOT NULL,         -- Hash BCrypt du secret
  user_id     BIGINT NOT NULL,
  expires_at  TIMESTAMP NOT NULL             -- Expiration 15 min
)

-- Tokens JWT stockés pour révocation
jwt_tokens (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  token       VARCHAR(512) UNIQUE NOT NULL,  -- Le JWT complet
  email       VARCHAR(255) NOT NULL,
  user_id     BIGINT NOT NULL,
  created_at  TIMESTAMP NOT NULL,
  expires_at  TIMESTAMP NOT NULL,
  revoked     BOOLEAN NOT NULL DEFAULT FALSE -- True si logout effectué
)
```

---

## Logging

Le système utilise SLF4J avec des tags par couche pour faciliter le debug :

| Tag | Source | Exemple |
|-----|--------|---------|
| `[CONTROLLER]` | AuthController | Requêtes HTTP entrantes |
| `[CONTROLLER-ADMIN]` | AdminController | Opérations admin |
| `[SERVICE-AUTH]` | AuthService | Login, register, verify |
| `[SERVICE-USER]` | UserService | CRUD utilisateurs |
| `[NOTIFICATION]` | NotificationService | Envoi d'emails |
| `[INIT]` | DataInitializer | Création des permissions au démarrage |

**Exemple de trace d'inscription réussie** :
```
INFO  [CONTROLLER] Registration request received
INFO  [SERVICE-AUTH] Starting registration for email: john@example.com
INFO  [SERVICE-USER] Creating user: john@example.com
INFO  [SERVICE-USER] User saved with ID: 1
INFO  [SERVICE-AUTH] Published UserRegisteredEvent for email: john@example.com
INFO  [SERVICE-AUTH] JWT generated, expires at: 2026-02-06T18:51:47
INFO  [CONTROLLER] Registration successful
INFO  [NOTIFICATION] Received UserRegistered event: abc-123
INFO  [NOTIFICATION] Verification email sent successfully to: john@example.com
```

**Exemple de login échoué (mauvais mot de passe)** :
```
INFO  [CONTROLLER] Login request received
INFO  [SERVICE-AUTH] Login attempt for email: john@example.com
DEBUG [SERVICE-USER] Verifying password
DEBUG [SERVICE-USER] Password verification failed
WARN  [SERVICE-AUTH] Login failed: Invalid password
WARN  [CONTROLLER] Login failed
```

**Exemple de login échoué (utilisateur inexistant)** :
```
INFO  [CONTROLLER] Login request received
INFO  [SERVICE-AUTH] Login attempt for email: unknown@example.com
WARN  [SERVICE-AUTH] Login failed: User not found
WARN  [CONTROLLER] Login failed
```

