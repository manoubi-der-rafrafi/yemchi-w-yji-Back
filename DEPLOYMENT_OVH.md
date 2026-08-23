# Déploiement OVHcloud VPS

Cette configuration exécute l'API Spring Boot dans Docker, en utilisateur non-root.
Le port applicatif est lié uniquement à `127.0.0.1:8081` afin qu'un reverse proxy
Nginx ou Caddy puisse être le seul point d'entrée public.

## Instance de production

- VPS : `vps-94d1a9bf.vps.ovh.net` (`57.131.49.233`)
- Application : `/opt/yemchi`
- API publique : `https://api.yemchi-w-yji.tn`
- Accès SSH : utilisateur `ubuntu`, clé locale `~/.ssh/ovh_yemchi`
- Secrets : `/opt/yemchi/.env.production` avec permissions `0600`

## 1. Préparer le VPS

- Mettre Ubuntu à jour.
- Installer Docker Engine et le plugin Docker Compose depuis le dépôt officiel Docker.
- Activer un pare-feu autorisant uniquement SSH, HTTP et HTTPS.
- Créer un utilisateur d'administration avec une clé SSH et désactiver ensuite
  l'authentification SSH par mot de passe.

## 2. Configurer les secrets

Dans le dossier du projet sur le serveur :

```bash
cp .env.production.example .env.production
chmod 600 .env.production
```

Modifier `.env.production` et remplacer toutes les valeurs `CHANGE_ME`. Les secrets
JWT et de provisioning doivent être différents et générés aléatoirement.

## 3. Construire et démarrer

```bash
APP_ENV_FILE=.env.production docker compose -f compose.production.yml build
APP_ENV_FILE=.env.production docker compose -f compose.production.yml up -d
APP_ENV_FILE=.env.production docker compose -f compose.production.yml ps
curl --fail http://127.0.0.1:8081/health
```

## 4. Exposer l'API en HTTPS

Configurer Nginx ou Caddy avec le domaine de l'API, puis rediriger le trafic vers
`http://127.0.0.1:8081`. Ne pas ouvrir directement le port 8081 sur Internet.

La production utilise `deploy/nginx-yemchi.conf`. Certbot gère le certificat
Let's Encrypt et son renouvellement automatique via `certbot.timer`.

## 5. Mise à jour de l'application

```bash
git pull --ff-only
APP_ENV_FILE=.env.production docker compose -f compose.production.yml build
APP_ENV_FILE=.env.production docker compose -f compose.production.yml up -d
docker image prune -f
```

Vérifier `/health` après chaque mise à jour et conserver une sauvegarde MongoDB
indépendante de la sauvegarde quotidienne du VPS.
