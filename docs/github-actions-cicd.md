# GitHub Actions CI/CD

This repository now uses a single GitHub Actions workflow:

- `deploy.yml`
  Runs tests on pushes to `main`, `dev`, and `feature/**`, plus pull requests to `main` and `dev`.
  On `main`, the same workflow then builds and pushes Docker images for every backend service, and deploys to EC2 only after the test and image jobs succeed.

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
- Because CI and CD are in the same workflow, deployment cannot succeed when the test job fails.
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
