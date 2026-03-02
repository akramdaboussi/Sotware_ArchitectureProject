# 🧠 Comprendre le TP : Architecture et Messagerie Asynchrone

> **TL;DR** : Ce fichier explique pas-à-pas comment les services communiquent entre eux en utilisant **RabbitMQ**, et pourquoi nous ne faisons pas tout de manière "synchrone".

---

## 🏗️ L'Architecture Globale

Nous avons ici un système divisé logiquement en deux parties (même si elles vivent dans la même application pour l'instant) :
1. **L'authentification (`AuthService`)** : Son seul but est de s'occuper des inscriptions, des connexions et des mots de passe.
2. **La notification (`NotificationService`)** : Son seul but est d'envoyer des e-mails.

### Pourquoi les séparer ? (Le Découplage)
Imaginons que le serveur d'e-mail (MailHog) soit lent ou tombe en panne. Si nous envoyions l'e-mail directement dans la fonction d'inscription (de manière synchrone), l'utilisateur verrait un écran de chargement infini et son inscription échouerait complètement. 

Pour éviter cela, nous utilisons **RabbitMQ** (un *message broker*). L'inscription dépose simplement un "message" disant : *"Hé, un utilisateur vient de s'inscrire !"*, et termine immédiatement son travail. Le service d'envoi d'e-mail lira ce message quand il le voudra, à son propre rythme. 

---

## 📬 La Pipeline des Échanges : Comment ça marche ?

Voici le cycle complet de l'événement, depuis le clic jusqu'à la réception de l'e-mail :

### 1. Inscription (Le Producteur)
* **Action** : L'utilisateur appelle `POST /register` avec son nom, prénom et e-mail.
* **Ce qui se passe** :
  1. `AuthService` sauvegarde l'utilisateur (`verified=false`).
  2. Il génère un "Token" super secret composé de deux informations :
     * `tokenId` : un identifiant public.
     * `tokenHash` : la version brouillée (Bcrypt) du vrai secret (pour que personne ne puisse le lire dans la base de données).
  3. `AuthService` publie un événement `UserRegisteredEvent` dans **RabbitMQ** (sur "l'Exchange" `auth.events`).

### 2. RabbitMQ (Le Facteur)
* **L'Échange (`auth.events`)** : Reçoit le message de l'AuthService.
* **Le Routage (`auth.user-registered`)** : Grâce à cette clé, l'échange sait dans quelle boîte aux lettres (Queue) déposer le message.
* **La File d'attente (`notification.user-registered`)** : Stocke le message bien au chaud en attendant que le NotificationService vienne le lire.
* **La File d'attente Morte (DLQ - *Dead Letter Queue*)** : Si le message échoue après plusieurs tentatives (par exemple parce que le service d'e-mail ne marche plus du tout), il est déposé dans cette file d'attente "poubelle". L'administrateur pourra lire cette file plus tard pour relancer manuellement les envois ayant échoués.

### 3. L'Envoi d'E-mail (Le Consommateur)
* **Action** : Le `NotificationService` a une méthode `@RabbitListener` qui écoute constamment la file `notification.user-registered`.
* **Ce qui se passe** :
  1. Il "dépile" le message contenant l'e-mail et les tokens.
  2. Il construit le lien magique : `http://localhost:8080/api/auth/verify?tokenId=123&t=secret`.
  3. Il utilise `JavaMailSender` pour envoyer le mail via le serveur SMTP local (MailHog).

### 4. La Vérification (Le Retour)
* **Action** : L'utilisateur clique sur le lien reçu par e-mail (`GET /verify`).
* **Ce qui se passe** :
  1. Le contrôleur extrait `tokenId` et `tokenClear` (le "t" dans l'url).
  2. `AuthService` va chercher le token dans la base grâce au `tokenId`.
  3. Il vérifie que le token n'est pas expiré (15 min) et compare le secret en clair `tokenClear` avec la version hashée qui est en base de donnée (`Bcrypt.matches()`).
  4. Si c'est bon, l'utilisateur passe en `verified = true`, le token est supprimé de la base pour empêcher quelqu'un d'autre de l'utiliser.

---

## 🔍 Pourquoi vérifier l'E-mail avec un Token Crypté ?

Vous pourriez vous demander : *"Pourquoi ne pas juste envoyer l'ID de l'utilisateur dans l'e-mail ?"*

Parce que sinon, n'importe qui pourrait taper `http://localhost:8080/api/auth/verify?userId=2` et vérifier le compte de "Bob" sans sa permission ! 
Le `tokenClear` sert de clé privée (et temporaire). De la même manière que pour le mot de passe, stocker un *"Hash"* (Bcrypt) au lieu du vrai token dans la base de données garantit que même si un hacker vole la table des tokens, il ne pourra pas valider tous les comptes.

Grâce à cette séparation de messagerie (RabbitMQ) et cette sécurité des données (JPA + Bcrypt), votre système est désormais asynchrone, résilient et robuste ! 🎉
