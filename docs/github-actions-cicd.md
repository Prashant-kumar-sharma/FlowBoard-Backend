# GitHub Actions CI/CD

This repository now includes two GitHub Actions workflows:

- `backend-ci.yml`
  Runs on pushes to `main`, `dev`, and `feature/**`, plus pull requests to `main` and `dev`.
  It checks out the backend monorepo, installs Java 17, and runs `mvn clean verify`.

- `backend-cd.yml`
  Runs on pushes to `main` and on manual dispatch.
  It builds and pushes Docker images for every backend service to Docker Hub.
  If the deployment secrets are configured, it also deploys the updated stack on your EC2 machine with `docker compose pull` and `docker compose up -d --no-build`.

## Required GitHub Secrets

Add these repository secrets in `FlowBoard-Backend`:

- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`
- `EC2_HOST`
- `EC2_USERNAME`
- `EC2_SSH_KEY`
- `SERVER_APP_PATH`

Notes:

- `SERVER_APP_PATH` should be the absolute path on the server where the backend repo containing `docker-compose.yml` is checked out.
- The deploy job is skipped automatically if the EC2 secrets are not present.
- The workflow expects your Docker Hub namespace to be `prashar85211`, matching the image names already used in `docker-compose.yml`.

## Suggested Branch Flow

Your current backend feature work is on `feature/docker-version`.
If you want a dedicated CI/CD branch, create it from there:

```bash
git checkout feature/docker-version
git checkout -b feature/github-actions-cicd
git push -u origin feature/github-actions-cicd
```

## Deployment Behavior

The deploy step assumes:

- Docker and Docker Compose are already installed on the EC2 instance.
- The backend repo already exists on the server.
- The server can pull from GitHub and Docker Hub.
- The `.env` file required by `docker-compose.yml` is already present on the server.

## Frontend Coordination

The backend deploy pulls the frontend image too, because the same `docker-compose.yml` references `prashar85211/flowboard-frontend`.
For full end-to-end delivery, configure the frontend repository workflows as well.
