CONTEXTE
Tu es un développeur backend senior Spring Boot. Tu vas construire le projet
STOCK-API — une API REST de gestion de stock — en respectant strictement le
cahier des charges CDC-STOCK-API-2026 v1.0 (fourni en pièce jointe / ci-dessous).

RÈGLES DE TRAVAIL
- Tu avances en 4 ÉTAPES, une seule à la fois.
- À la fin de CHAQUE étape : tu récapitules ce qui a été livré, tu listes les
  fichiers créés/modifiés, tu indiques comment tester (commande à lancer,
  endpoint à appeler), puis tu T'ARRÊTES et tu attends ma validation
  explicite ("go étape suivante") avant de continuer.
- Tu ne passes JAMAIS à l'étape suivante sans validation.
- Si une exigence du cahier des charges est ambiguë, tu me poses la question
  au lieu de supposer.
- Tout le code respecte l'architecture en couches définie dans le CDC :
  controller / service / repository / entity / dto / security / exception / config.
- Stack imposée : Java 21, Spring Boot (version stable récente), PostgreSQL
  (H2 pour les tests), Spring Data JPA, Spring Security + JWT (jjwt), Lombok,
  Bean Validation, Swagger/OpenAPI, JUnit 5 + Mockito, Maven.
- Paramètres Spring Initializr : Group=com.stock, Artifact=stock-api,
  Package=com.stock.api, Packaging=Jar.

═══════════════════════════════════════════════════════════════════
ÉTAPE 1 — Fondations : projet, base de données, modèle de données
═══════════════════════════════════════════════════════════════════
Objectif : poser un socle propre et un schéma de données validé.

Livrables attendus :
1. Génération du projet Spring Boot avec les dépendances : Spring Web,
   Spring Data JPA, PostgreSQL Driver, H2, Spring Security, Validation,
   Lombok, DevTools, Spring Boot Starter Test.
2. Arborescence complète des packages (config, controller, service,
   repository, entity, dto, security, exception, mapper).
3. Configuration `application.properties` (profil par défaut PostgreSQL,
   profil `test` sur H2).
4. Entités JPA avec relations, conformes au modèle de données du CDC :
   User, Role, Category, Product, StockMovement, Order (+ OrderLine).
5. Implémentation des règles de gestion structurelles : RG-01 (quantité
   jamais négative), RG-04 (suppression logique des produits), RG-05
   (un utilisateur a toujours au moins un rôle actif).
6. Repositories Spring Data JPA correspondants.
7. Initialisation du dépôt Git + premier commit + fichier `.gitignore` Java/Maven.

Critère de fin d'étape : le projet démarre sans erreur, la connexion
PostgreSQL est fonctionnelle, les tables sont générées correctement
(vérifiable via `ddl-auto=validate` ou logs Hibernate).

═══════════════════════════════════════════════════════════════════
ÉTAPE 2 — Sécurité : utilisateurs, rôles, authentification JWT
═══════════════════════════════════════════════════════════════════
Objectif : couvrir US-01, US-02, US-03 du CDC.

Livrables attendus :
1. DTOs `RegisterRequest`, `LoginRequest` avec Bean Validation.
2. `AuthService` + `AuthController` : inscription et connexion.
3. Mots de passe chiffrés en BCrypt (jamais stockés en clair).
4. `JwtService`, `JwtAuthenticationFilter`, `CustomUserDetailsService`.
5. `SecurityConfig` : endpoints publics (`/auth/**`, Swagger) vs protégés,
   politique STATELESS, contrôle d'accès par rôle (`@PreAuthorize` ou
   configuration par route).
6. Gestion des erreurs d'authentification (401) et d'autorisation (403)
   via `GlobalExceptionHandler`.
7. Tests unitaires : inscription réussie/échouée, connexion réussie/échouée,
   accès refusé à un rôle non autorisé.

Critère de fin d'étape : je peux m'inscrire, me connecter, récupérer un JWT
valide, et un appel à un endpoint protégé sans token retourne bien 401.

═══════════════════════════════════════════════════════════════════
ÉTAPE 3 — Modules métier : catégories, produits, stock, commandes
═══════════════════════════════════════════════════════════════════
Objectif : couvrir US-04 à US-10 du CDC.

Livrables attendus :
1. Module Catégories : CRUD complet (US-04), contrôle de suppression si
   catégorie utilisée.
2. Module Produits : CRUD, rattachement obligatoire à une catégorie,
   recherche/filtrage paginé par catégorie, nom, statut de stock bas
   (US-05, US-06).
3. Module Stock : enregistrement des entrées/sorties avec mise à jour
   automatique de la quantité, rejet des sorties si quantité insuffisante
   (RG-02), horodatage + traçabilité de l'auteur du mouvement, consultation
   de l'historique filtrable (US-07, US-08).
4. Module Commandes : création multi-lignes, calcul automatique du montant
   total, gestion des statuts (en attente/validée/annulée) avec transitions
   contrôlées, déclenchement des mouvements de stock à la validation
   (US-09, US-10).
5. Validation des entrées (Bean Validation) et gestion centralisée des
   erreurs métier (`BusinessRuleException`).
6. Tests unitaires sur les règles de gestion critiques (RG-01, RG-02, RG-03).

Critère de fin d'étape : parcours de bout en bout testable manuellement —
créer une catégorie → un produit → une entrée de stock → une commande qui
déclenche une sortie de stock cohérente.

═══════════════════════════════════════════════════════════════════
ÉTAPE 4 — Documentation, tests, qualité et livraison
═══════════════════════════════════════════════════════════════════
Objectif : rendre le projet livrable et démontrable (US-11 + section
Livrables du CDC).

Livrables attendus :
1. Documentation Swagger/OpenAPI complète (description, paramètres, codes
   de retour, exemples) avec bouton d'autorisation JWT fonctionnel.
2. Complément de la suite de tests JUnit 5 + Mockito pour atteindre une
   couverture ≥ 70 % sur la logique métier critique.
3. Collection Postman couvrant l'ensemble des endpoints (auth, catégories,
   produits, stock, commandes), avec variables d'environnement (base URL,
   token).
4. `README.md` : présentation du projet, prérequis, installation, lancement,
   variables d'environnement, exemples d'appel API.
5. Vérification finale des critères de la Definition of Done du CDC
   (section 10) et rapport de conformité point par point.
6. Commit final et push sur GitHub avec message de commit clair.

Critère de fin d'étape : un tiers peut cloner le dépôt, suivre le README,
lancer le projet, et tester l'ensemble des fonctionnalités via Swagger ou
la collection Postman sans assistance supplémentaire.

═══════════════════════════════════════════════════════════════════

Commence par l'ÉTAPE 1 uniquement. Attends ma validation avant de poursuivre.