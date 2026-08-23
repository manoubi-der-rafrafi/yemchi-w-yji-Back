# Rapport des corrections de sécurité

**Application :** Yemchi w Yji  
**Date du rapport :** 21 août 2026  
**Périmètre examiné :** backend Spring Boot, frontend Angular et deux applications Flutter  
**Objet :** documenter uniquement les vulnérabilités corrigées pendant cette discussion

## 1. Résumé exécutif

Les corrections réalisées ont principalement renforcé :

- l'authentification et la gestion des sessions ;
- la séparation des droits entre client, transporteur et administrateur ;
- la confidentialité des profils et recherches utilisateurs ;
- la protection des commandes contre la modification de champs internes ;
- la vérification de l'adresse email lors de l'inscription ;
- la validation des uploads et la limitation des abus ;
- la protection des données enregistrées dans les clients web et mobiles ;
- la résistance aux erreurs concurrentes et aux dépendances vulnérables.

Les tests automatisés exécutés après ces corrections ont donné **53 tests backend réussis, sans échec**. La compilation Angular a réussi, l'audit npm des dépendances de production a indiqué **0 vulnérabilité**, et les deux projets Flutter ne présentaient aucune erreur d'analyse bloquante.

## 2. Failles corrigées

### SEC-01 — Recherche d'un utilisateur sans authentification et exposition du profil complet

**Risque initial : critique**

La recherche par email pouvait être appelée sans authentification. Les recherches par email et téléphone pouvaient retourner directement l'entité `Utilisateur`, contenant notamment le hash du mot de passe, l'adresse, les documents d'identité et la position GPS.

**Scénario d'attaque :** un attaquant recherchait une adresse email ou un numéro de téléphone et récupérait des données privées appartenant à un autre utilisateur.

**Correction appliquée :**

- authentification obligatoire pour les deux recherches ;
- chargement et validation de l'utilisateur connecté côté backend ;
- remplacement de l'entité complète par `UtilisateurSearchResponse` ;
- réponse limitée à l'identifiant, au nom, au prénom, à l'email, au téléphone, à l'image, au rôle et au statut.

**Preuves techniques :**

- `SecurityConfig.java`, règles des routes `/api/utilisateur/search/email` et `/api/utilisateur/search/numero` ;
- `UtilisateurController.java`, méthodes `chercherParNumero` et `chercherParEmail` ;
- `dto/UtilisateurSearchResponse.java`.

### SEC-02 — Accès d'un client aux transporteurs en panne et à leurs commandes

**Risque initial : critique**

Un simple compte client pouvait appeler `/api/utilisateur/transporteurs/panne/commandes` et obtenir les transporteurs en panne, leurs téléphones, positions GPS, commandes, produits et informations de livraison.

**Correction appliquée :**

- accès refusé aux comptes clients ;
- endpoint limité aux transporteurs et administrateurs avec `requireTransporteurOrAdmin` ;
- compte banni également bloqué par le filtre global de statut.

**Preuves techniques :**

- `UtilisateurController.java`, méthode `getTransporteursEnPanneAvecCommandes` ;
- `AuthorizationService.java`, méthode `requireTransporteurOrAdmin`.

**Limite connue :** cette correction empêche l'accès des clients, mais un transporteur peut encore voir trop d'informations sur d'autres transporteurs. Ce point figure dans les risques restant ouverts.

### SEC-03 — Consultation du statut de présence d'un utilisateur arbitraire

**Risque initial : élevé**

Tout utilisateur connecté pouvait consulter le statut en ligne d'un identifiant arbitraire.

**Correction appliquée :**

- récupération du compte connecté ;
- autorisation seulement pour soi-même, un administrateur, un ami accepté ou un participant autorisé à une commande commune ;
- refus HTTP 403 dans les autres cas.

**Preuves techniques :**

- `PresenceController.java`, endpoint `GET /api/presence/{id}` ;
- `AuthorizationService.java`, méthode `canAccessUserData`.

### SEC-04 — Documents d'identité et GPS exposés par la consultation d'un profil

**Risque initial : critique**

Lorsqu'un utilisateur était ami ou participant à une commande, `GET /api/utilisateur/id/{id}` pouvait retourner presque tout le profil cible. Seul le mot de passe était masqué.

**Correction appliquée :**

- le profil complet n'est retourné qu'à son propriétaire ou à un administrateur ;
- les amis et participants reçoivent uniquement `UtilisateurSearchResponse` ;
- documents, adresse détaillée, GPS et hash du mot de passe absents de cette réponse.

**Preuves techniques :**

- `UtilisateurController.java`, méthode `getUtilisateurById` ;
- `dto/UtilisateurSearchResponse.java`.

### SEC-05 — Entités utilisateur complètes retournées par les fonctionnalités d'amis

**Risque initial : élevé**

Les listes d'amis, invitations et recherches parmi les amis pouvaient retourner l'entité `Utilisateur` complète.

**Correction appliquée :**

- toutes ces réponses utilisent maintenant `UtilisateurSearchResponse` ;
- ajout d'une conversion centralisée avec `safeUsers` ;
- conservation des contrôles `requireSelfOrAdmin` sur l'identifiant demandé.

**Preuve technique :** `AmiController.java`.

### SEC-06 — Droits trop larges sur les commandes

**Risque initial : critique**

Client, ami, transporteur principal et transporteur de secours étaient traités comme des participants équivalents. Un participant pouvait donc atteindre des opérations qui ne correspondaient pas à son rôle.

**Correction appliquée :**

- séparation entre lecture générale, écriture du propriétaire et opérations du transporteur affecté ;
- `requireCommandeOwnerOrAdmin` pour les modifications appartenant au client ;
- `requireAssignedTransporteurOrAdmin` pour les étapes appartenant au transporteur ;
- accès aux produits déterminé selon la relation réelle avec la commande ;
- récupération des listes client ou transporteur limitée à soi-même ou à un administrateur.

**Preuves techniques :**

- `AuthorizationService.java` ;
- `CommandeController.java` ;
- `ProduitController.java`.

**Limite connue :** la séparation générale est corrigée, mais les états métier, les vrais QR codes et la distinction entre transporteur principal et secours doivent encore être renforcés.

### SEC-07 — Mass assignment lors de la création ou modification d'une commande

**Risque initial : critique**

Un client pouvait envoyer des champs internes dans le JSON d'une commande : statut, transporteur, source B2C, règlement, prix internes, identifiants partenaires ou indicateurs de scan.

**Correction appliquée :**

- l'identifiant fourni à la création est toujours supprimé ;
- le backend impose l'identifiant du client connecté ;
- nettoyage des champs contrôlés par le serveur avec `sanitizeClientControlledFields` ;
- création client forcée en source `C2C` et statut `en_cours` ;
- transitions de statut retirées des mises à jour génériques client ;
- PATCH redirigé vers une copie explicite de champs au lieu d'une copie par réflexion ;
- suppression de la copie générique des champs de scan ;
- Angular et Flutter appellent l'endpoint métier de confirmation après la mise à jour autorisée.

**Preuves techniques :**

- `CommandeController.java`, méthodes de création, PUT, PATCH et `sanitizeClientControlledFields` ;
- `CommandeService.java`, méthodes `updateCommande` et `updateCommandePatch` ;
- tests `CommandeControllerSecurityTest.java`.

### SEC-08 — Contournement du bannissement

**Risque initial : critique**

Un compte banni disposant encore d'un JWT valide pouvait continuer à appeler certaines routes. Certains parcours Google ou de mise à jour du statut pouvaient également réactiver incorrectement un compte.

**Correction appliquée :**

- ajout d'`AccountStatusFilter` après l'authentification JWT ;
- lecture du compte en base à chaque requête authentifiée ;
- réponse HTTP 403 pour un compte banni ou supprimé ;
- défense supplémentaire dans `AuthorizationService.currentUser` ;
- refus du renouvellement des refresh tokens d'un compte banni ;
- refus de réactivation d'un compte banni via Google ;
- seul un administrateur peut attribuer le statut `banni`.

**Preuves techniques :**

- `security/AccountStatusFilter.java` ;
- `SecurityConfig.java` ;
- `AuthorizationService.java` ;
- `AuthTokenService.java` ;
- tests `AccountStatusFilterTest.java`.

### SEC-09 — Course entre vérification email et finalisation de l'inscription

**Risque initial : critique**

Le simple état `isEmailVerified` du compte ne liait pas suffisamment la vérification à une tentative d'inscription précise. Des requêtes concurrentes ou réutilisées pouvaient créer un comportement ambigu.

**Correction appliquée :**

- création d'une session d'inscription dédiée ;
- génération d'un token client aléatoire de 256 bits ;
- stockage uniquement du hash SHA-256 du token ;
- expiration automatique après 30 minutes avec index TTL MongoDB ;
- liaison entre session, utilisateur et email normalisé ;
- marquage vérifié par mise à jour MongoDB atomique ;
- consommation atomique, vérifiée et utilisable une seule fois ;
- inscription finale protégée par l'en-tête `X-Signup-Token` ;
- adaptation d'Angular et des deux clients Flutter.

**Preuves techniques :**

- `model/SignupVerificationSession.java` ;
- `repository/SignupVerificationSessionRepository.java` ;
- `service/SignupVerificationService.java` ;
- `UtilisateurController.java` ;
- clients Angular et Flutter d'inscription.

### SEC-10 — Manipulation des factures par un livreur

**Risque initial : élevé**

Un livreur pouvait essayer d'imposer l'identifiant de la facture, l'identifiant d'un autre livreur, le type de versement, l'état de confirmation ou une date arbitraire.

**Correction appliquée :**

- création limitée aux transporteurs et administrateurs ;
- suppression de l'identifiant fourni ;
- identifiant livreur imposé depuis le compte connecté ;
- type imposé à `LIVREUR_VERSE_ENTREPRISE` pour un livreur ;
- statut imposé à `NON_TRAITER` ;
- date générée par le serveur ;
- validation du montant et limitation de la consultation à soi-même ou à l'administrateur.

**Preuve technique :** `FactureController.java`.

### SEC-11 — Upload de fichiers insuffisamment validé

**Risque initial : élevé**

Les uploads reposaient principalement sur le type MIME déclaré par le client et pouvaient recevoir des fichiers trop volumineux ou des destinations de stockage arbitraires.

**Correction appliquée :**

- taille maximale par type d'opération ;
- vérification du type image ;
- vérification de la signature binaire JPEG, PNG, GIF, WebP ou HEIF ;
- liste blanche des destinations `produits` et `profile` ;
- noms uniques et absence d'écrasement Cloudinary ;
- quotas sur les routes multipart et d'analyse.

**Preuves techniques :**

- `security/ImageUploadValidator.java` ;
- `ProduitController.java` ;
- `UtilisateurController.java` ;
- `DemandeController.java` ;
- `FactureController.java`.

**Limite connue :** les documents sensibles Cloudinary doivent encore passer en mode privé/authentifié.

### SEC-12 — Politique de mot de passe et exposition du hash

**Risque initial : élevé**

Les mots de passe faibles et le retour accidentel du hash dans les réponses augmentaient le risque de compromission.

**Correction appliquée :**

- politique de 8 à 128 caractères avec majuscule, minuscule et chiffre ;
- hash avec BCrypt via `DelegatingPasswordEncoder` ;
- refus des mots de passe absents ou faibles à l'inscription ;
- retrait du mot de passe des réponses de connexion et de profil ;
- DTO publics ne contenant aucun champ de mot de passe.

**Preuves techniques :**

- `security/PasswordPolicy.java` ;
- `SecurityConfig.java` ;
- `UtilisateurController.java`.

### SEC-13 — Sessions longues et refresh tokens réutilisables

**Risque initial : critique**

Une session reposant uniquement sur un JWT long augmente fortement l'impact d'un token volé.

**Correction appliquée :**

- JWT d'accès court, avec audience, issuer, identifiant unique et purpose `access` ;
- refresh token opaque aléatoire ;
- stockage du hash seulement en base ;
- rotation atomique à chaque renouvellement ;
- détection de réutilisation et révocation de toute la famille ;
- révocation au logout et pour les comptes bannis ;
- refresh token web placé dans un cookie `HttpOnly`, `Secure` et `SameSite` ;
- contrôle de l'origine pour refresh et logout navigateur ;
- conservation en mémoire du JWT Angular au lieu d'un stockage JavaScript persistant.

**Preuves techniques :**

- `service/AuthTokenService.java` ;
- `controller/AuthSessionController.java` ;
- `models/RefreshTokenFamily.java` et `RefreshTokenSession.java` ;
- `src/app/services/auth.service.ts` dans Angular.

### SEC-14 — Fuite d'informations dans les erreurs et journaux

**Risque initial : élevé**

Des messages de fournisseurs externes, réponses réseau et traces complètes pouvaient révéler des informations techniques ou sensibles.

**Correction appliquée :**

- retrait du corps brut des erreurs du fournisseur email ;
- réponses génériques pour les erreurs d'upload, de localisation et d'analyse ;
- suppression des appels `printStackTrace` dans les contrôleurs concernés ;
- masquage des emails dans les journaux ;
- suppression des tokens, mots de passe, secrets et coordonnées dans la télémétrie applicative ;
- limitation de la taille et de la profondeur des métadonnées d'erreur.

**Preuves techniques :**

- `MailService.java` ;
- `UtilisateurController.java` ;
- `ApplicationErrorService.java`.

### SEC-15 — Absence de limitation sur les opérations coûteuses

**Risque initial : élevé**

Connexion, inscription, recherche, uploads, routage, renouvellement de session et télémétrie pouvaient être appelés de façon répétée.

**Correction appliquée :**

- filtre de quotas par IP et par utilisateur ;
- quotas spécifiques pour connexion, inscription, recherche, présence, routage, refresh, provisioning, email, médias et télémétrie ;
- limite de 64 Ko sur les requêtes de télémétrie ;
- en-tête `Retry-After` avec réponse HTTP 429 ;
- seconde couche Nginx avec `limit_req`, `limit_conn` et limite de taille.

**Preuves techniques :**

- `security/ApiAbuseProtectionFilter.java` ;
- `service/RequestRateLimiter.java` ;
- `deploy/nginx-yemchi.conf`.

### SEC-16 — Écrasement silencieux lors de mises à jour concurrentes

**Risque initial : élevé**

Deux requêtes pouvaient modifier simultanément la même commande, la dernière sauvegarde écrasant silencieusement la première.

**Correction appliquée :**

- ajout de `@Version` à `Commande` ;
- migration des commandes historiques sans version ;
- réponse HTTP 409 lors d'un conflit optimiste ;
- conservation des transitions atomiques déjà utilisées pour les refresh tokens et sessions d'inscription.

**Preuves techniques :**

- `model/Commande.java` ;
- `config/CommandeVersionMigration.java` ;
- `controller/ApiConcurrencyExceptionHandler.java`.

### SEC-17 — Données sensibles conservées dans le navigateur

**Risque initial : élevé**

Le frontend Angular pouvait conserver un objet utilisateur trop complet et un ancien JWT dans `localStorage`.

**Correction appliquée :**

- liste blanche pour l'utilisateur conservé localement ;
- documents, adresse détaillée, mot de passe et GPS exclus ;
- ancien JWT lu une fois puis supprimé de `localStorage` ;
- nouveaux JWT conservés uniquement en mémoire ;
- refresh token web conservé dans un cookie HttpOnly.

**Preuve technique :** `src/app/services/auth.service.ts` du projet Angular.

### SEC-18 — Dépendances Angular présentant des vulnérabilités connues

**Risque initial : élevé**

Angular 18, Express 4 et plusieurs dépendances transitives présentaient des avis de sécurité connus.

**Correction appliquée :**

- migration vers Angular 20 ;
- migration Angular Material/CDK vers la version 20 ;
- migration vers Express 5 ;
- mise à jour TypeScript, SSR et dépendances transitives ;
- adaptation du moteur SSR à `@angular/ssr/node`.

**Résultat :** `npm audit --omit=dev` retourne **0 vulnérabilité de production**.

### SEC-19 — Protection insuffisante du frontend contre l'injection et l'encadrement

**Risque initial : élevé**

Sans en-têtes de sécurité, une injection frontend ou l'affichage du site dans une frame hostile peut augmenter le risque de vol de session ou de clickjacking.

**Correction appliquée :**

- politique CSP dans `vercel.json` ;
- `frame-ancestors 'none'` ;
- `object-src 'none'` ;
- restriction des scripts, connexions, images et frames aux sources nécessaires ;
- ajout de `Referrer-Policy`, `Permissions-Policy` et `X-Content-Type-Options`.

## 3. Vérifications réalisées

| Vérification | Résultat |
|---|---:|
| Tests backend hors test de contexte MongoDB distant | 53 réussis, 0 échec |
| Tests ciblés comptes bannis | Réussis |
| Tests ciblés mass assignment des commandes | Réussis |
| Compilation Angular de production | Réussie |
| Audit npm des dépendances de production | 0 vulnérabilité |
| Analyse Flutter principal | Aucune erreur bloquante |
| Analyse Flutter Ahmed | Aucune erreur bloquante |

Le test `TransportApplicationTests` nécessitant une connexion à la base MongoDB distante n'a pas été inclus dans cette exécution locale.

## 4. Risques non couverts par ce rapport de correction

Les points suivants ont été découverts lors du dernier audit mais **ne sont pas encore corrigés** :

1. modification, suppression ou reconfirmation d'une commande après son affectation ;
2. absence de validation réelle du QR code et possibilité de déclarer une livraison sans preuve ;
3. amplification MongoDB et déni de service via `/api/utilisateur/positions` ;
4. affectation insuffisamment contrôlée des transporteurs principal et de secours ;
5. exposition excessive des commandes et positions aux transporteurs via les routes zone, sous-zone et panne ;
6. envoi du JWT utilisateur directement vers un webhook n8n par un client Flutter ;
7. documents Cloudinary encore accessibles par URL publique ;
8. stockage des tokens dans `SharedPreferences` pour Flutter Web ;
9. trafic HTTP autorisé dans un manifeste Android et APK de production encore signé avec une clé de debug ;
10. validation insuffisante de la robustesse du secret JWT et compatibilité permanente avec certains anciens JWT.

Ces éléments nécessitent une phase de correction distincte. Ils ne doivent pas être considérés comme résolus par les mesures décrites dans la section 2.

## 5. Conclusion

Les corrections déjà réalisées ferment les fuites directes les plus importantes sur les utilisateurs, les recherches, les amis, la présence, les comptes bannis, les sessions, les inscriptions, les factures et les champs internes des commandes. Elles améliorent également la résistance aux uploads malveillants, aux abus de volume, aux erreurs concurrentes et aux dépendances vulnérables.

La sécurité globale reste toutefois incomplète tant que les risques listés dans la section 4 ne sont pas corrigés, en particulier la machine d'état des commandes, la preuve QR, le déni de service MongoDB, l'appel direct à n8n et la confidentialité des documents Cloudinary.
