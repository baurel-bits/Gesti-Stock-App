# 📦 Stock API

API REST de gestion de stock développée avec Spring Boot 3.4 et Java 21.

## 📋 Table des matières

- [Fonctionnalités](#fonctionnalités)
- [Architecture](#architecture)
- [Prérequis](#prérequis)
- [Installation](#installation)
- [Lancement](#lancement)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Règles de gestion](#règles-de-gestion)
- [Tests](#tests)
- [Documentation Swagger](#documentation-swagger)

---

## ✨ Fonctionnalités

| Module | Description | US |
|--------|-------------|-----|
| **Authentification** | Inscription, connexion, JWT | US-01, US-02, US-03 |
| **Catégories** | CRUD complet, suppression logique | US-04 |
| **Produits** | CRUD, recherche/filtrage, alerte stock bas | US-05, US-06 |
| **Stock** | Entrées/sorties, historique filtrable, traçabilité | US-07, US-08 |
| **Commandes** | Multi-lignes, validation avec déclenchement stock | US-09, US-10 |

## 🏗️ Architecture

```
src/main/java/com/stock/api/
├── config/          # Configuration (Security, Swagger, CORS)
├── controller/      # Controllers REST
├── dto/             # Data Transfer Objects (Request/Response)
├── entity/          # Entités JPA
├── exception/       # Gestion centralisée des erreurs
├── repository/      # Repositories Spring Data JPA
├── security/        # JWT (Service, Filter, UserDetailsService)
└── service/         # Logique métier
```

## 📦 Prérequis

- **Java 21** ou supérieur
- **Maven 3.8+** (ou utiliser le wrapper `./mvnw`)
- **PostgreSQL 15+** (ou H2 pour les tests)

## 🚀 Installation

```bash
# 1. Cloner le dépôt
git clone https://github.com/votre-org/stock-api.git
cd stock-api

# 2. Configurer la base de données
# Modifier src/main/resources/application-postgresql.properties
# avec vos paramètres de connexion

# 3. Compiler le projet
./mvnw clean install
```

## ▶️ Lancement

```bash
# Lancer avec le profil PostgreSQL (défaut)
./mvnw spring-boot:run

# Lancer avec le profil test (H2 en mémoire)
./mvnw spring-boot:run -Dspring-boot.run.profiles=test
```

L'application démarre sur **http://localhost:8080**

## ⚙️ Configuration

### Variables d'environnement

| Variable | Description | Défaut |
|----------|-------------|--------|
| `spring.datasource.url` | URL de connexion PostgreSQL | `jdbc:postgresql://localhost:5432/stock_db` |
| `spring.datasource.username` | Utilisateur PostgreSQL | `stock_user` |
| `spring.datasource.password` | Mot de passe PostgreSQL | `stock_password` |
| `jwt.secret` | Clé secrète pour JWT (Base64) | Valeur par défaut |
| `jwt.expiration` | Durée de vie du token (ms) | `86400000` (24h) |

### Fichiers de configuration

| Fichier | Profil | Description |
|---------|--------|-------------|
| `application.properties` | Défaut | Config principale |
| `application-postgresql.properties` | `postgresql` | Config PostgreSQL |
| `application-test.properties` | `test` | Config H2 pour les tests |

## 📡 API Endpoints

### Authentification (public)

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/api/auth/register` | Inscription |
| `POST` | `/api/auth/login` | Connexion → JWT |

### Catégories (🔒 authentifié)

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/api/categories` | Liste paginée |
| `GET` | `/api/categories/{id}` | Détail |
| `POST` | `/api/categories` | Créer |
| `PUT` | `/api/categories/{id}` | Modifier |
| `DELETE` | `/api/categories/{id}` | Supprimer (logique) |

### Produits (🔒 authentifié)

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/api/products` | Liste avec filtres (`categoryId`, `name`) |
| `GET` | `/api/products/{id}` | Détail |
| `GET` | `/api/products/low-stock` | Produits en stock bas |
| `POST` | `/api/products` | Créer |
| `PUT` | `/api/products/{id}` | Modifier |
| `DELETE` | `/api/products/{id}` | Supprimer (logique) |

### Mouvements de stock (🔒 authentifié)

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/api/stock-movements` | Enregistrer un mouvement (ENTRY/EXIT) |
| `GET` | `/api/stock-movements/product/{id}` | Historique par produit |
| `GET` | `/api/stock-movements/filters` | Historique filtré (`productId`, `type`, `fromDate`, `toDate`) |

### Commandes (🔒 authentifié)

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/api/orders` | Créer (multi-lignes) |
| `GET` | `/api/orders` | Liste avec filtres (`status`, `createdById`) |
| `GET` | `/api/orders/{id}` | Détail |
| `POST` | `/api/orders/{id}/validate` | Valider → déclenche sorties de stock |
| `POST` | `/api/orders/{id}/cancel` | Annuler |

## 📏 Règles de gestion

| Règle | Description |
|-------|-------------|
| **RG-01** | La quantité en stock est **jamais négative** |
| **RG-02** | Une sortie de stock est **rejetée si la quantité est insuffisante** |
| **RG-04** | La suppression des produits et catégories est **logique** (soft delete) |
| **RG-05** | Un utilisateur a toujours **au moins un rôle actif** |

## 🧪 Tests

```bash
# Lancer tous les tests
./mvnw test

# Tests unitaires uniquement
./mvnw test -Dtest="*Test"

# Tests d'un module spécifique
./mvnw test -Dtest="CategoryServiceTest"
```

**75 tests** couvrant :
- Authentification (inscription, connexion, JWT)
- Règles de gestion critiques (RG-01, RG-02, RG-04)
- Entités (Product, Order)
- Services (Category, Stock, Order, Auth)

## 📖 Documentation Swagger

Une fois l'application lancée, accédez à :

- **Swagger UI** : http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON** : http://localhost:8080/v3/api-docs

### Utiliser le bouton Authorize

1. Exécutez `POST /api/auth/register` ou `POST /api/auth/login`
2. Copiez le token JWT retourné
3. Cliquez sur **Authorize** dans Swagger UI
4. Collez le token (sans le préfixe "Bearer ")
5. Testez les endpoints protégés

## 📬 Postman

Importez la collection `stock-api.postman_collection.json` dans Postman.

La collection inclut :
- Variables d'environnement automatiques
- Sauvegarde automatique du token après inscription/connexion
- Tous les endpoints de l'API
- Scénarios de test (stock insuffisant, etc.)

## 📄 Licence

MIT License
