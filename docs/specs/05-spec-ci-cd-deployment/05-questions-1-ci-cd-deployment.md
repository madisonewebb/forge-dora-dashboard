# 05 Questions Round 1 - CI/CD Pipeline and Deployment

Please answer each question below (select one or more options, or add your own notes). Feel free to add additional context under any question.

## 1. Cloud Deployment Target

Where should the app be deployed when code merges to main?

- [ ] (A) **Fly.io** — free tier, Docker-native, easy CLI deploy, no credit card required for small apps
- [ ] (B) **Render** — free tier for web services, auto-deploys from GitHub, simple setup
- [ ] (C) **Railway** — free trial credits, Docker support, one-click GitHub deploys
- [X] (D) **AWS (EC2 or ECS)** — more realistic enterprise target, requires AWS account/credentials
- [ ] (E) **Google Cloud Run** — serverless containers, pay-per-use, requires GCP account
- [ ] (F) **Other** (describe)

## 2. Production Database

The app currently uses H2 (in-memory) for dev. What should production use?

- [ ] (A) **Keep H2 in prod too** — simplest path, no external DB needed, data resets on restart (fine for a demo app)
- [X] (B) **PostgreSQL** — persistent, realistic prod setup; most cloud providers offer a free managed tier
- [ ] (C) **SQLite** — file-based, no server needed, persistent across restarts
- [ ] (D) Other (describe)

## 3. CI Checks on Pull Requests

What should the CI pipeline run on every PR? Select all that apply.

- [X] (A) **Backend tests** — `./mvnw test`
- [X] (B) **Frontend tests** — `npm test`
- [X] (C) **Frontend lint** — `npm run lint` (if a lint script exists or we add one)
- [X] (D) **Docker build** — verify both images build successfully
- [ ] (E) Other (describe)

## 4. CD Trigger

When should the app automatically deploy to the cloud?

- [X] (A) **Every merge to main** — continuous deployment, always live on latest
- [ ] (B) **Manual trigger only** — a workflow_dispatch button in GitHub Actions
- [ ] (C) **On tagged releases** — deploy when a git tag like `v1.0.0` is pushed
- [ ] (D) Other (describe)

## 5. Secrets Management

The app needs `ANTHROPIC_API_KEY` at runtime. How should secrets be handled in the cloud deployment?

- [X] (A) **GitHub Actions Secrets → injected at deploy time** — secrets stored in GitHub, passed to the cloud provider via CLI/API during deploy
- [ ] (B) **Cloud provider's secret/env UI** — set directly in Fly.io / Render / Railway dashboard
- [ ] (C) **Both** — GitHub Actions Secrets for CI, cloud provider env UI for runtime
- [ ] (D) Other (describe)

## 6. Health Check Endpoint

Spring Boot Actuator can expose a `/actuator/health` endpoint. Should we add it?

- [X] (A) **Yes, add Spring Boot Actuator** — provides `/actuator/health` (and optionally `/actuator/info`), used by the cloud provider to verify the app is running
- [ ] (B) **Yes, but a simple custom endpoint** — a minimal `GET /api/health` that returns `{ "status": "ok" }` without pulling in Actuator
- [ ] (C) **No** — skip for now, cloud provider can use a TCP port check instead

## 7. Capstone Demo Priority

This is a capstone project for Liatrio. What matters most for the deployment?

- [X] (A) **Public URL** — the app must be accessible at a real URL (not just local Docker)
- [ ] (B) **Automated pipeline** — CI/CD running in GitHub Actions is the key deliverable
- [ ] (C) **Both** — public URL + automated pipeline
- [ ] (D) Other (describe)
