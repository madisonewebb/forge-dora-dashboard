# 05-spec-ci-cd-deployment.md

## Introduction/Overview

This spec covers automating the build, test, and deployment of the DORA Metrics Dashboard. Today the app runs locally via Docker Compose but has no automated pipeline and no cloud presence. The goal is a working GitHub Actions CI pipeline that validates every pull request, and a CD pipeline that deploys the app to AWS ECS Fargate on every merge to main — accessible at a public URL.

## Goals

- Every pull request runs automated tests, lint, and a Docker build before merge
- Every merge to `main` triggers an automatic deployment to AWS
- The app is accessible at a stable public URL backed by AWS ECS Fargate
- Production uses PostgreSQL on Amazon RDS instead of the in-memory H2 database
- The backend exposes a health check endpoint that AWS uses to verify the app is running

## User Stories

**As a developer**, I want every pull request to automatically run tests and a Docker build so that I catch regressions before they reach main.

**As a developer**, I want merging to main to automatically deploy the app so that I never have to manually push a release.

**As a Liatrio apprentice**, I want the app deployed at a real public URL so that I can demonstrate it live during the capstone presentation without needing a laptop running Docker.

**As a user of the dashboard**, I want the production app to use a persistent database so that cached GitHub API data is not lost every time the app restarts.

## Demoable Units of Work

### Unit 1: Health Check Endpoint

**Purpose:** Add a `/actuator/health` endpoint to the Spring Boot backend so AWS can verify the app is alive and route traffic to healthy instances.

**Functional Requirements:**
- The system shall expose `GET /actuator/health` returning HTTP 200 with `{"status":"UP"}` when the app is running
- The system shall expose `GET /actuator/info` with basic app metadata
- The system shall NOT expose sensitive actuator endpoints (env, beans, heapdump) publicly — only `health` and `info` shall be enabled
- The nginx reverse proxy shall forward `/actuator/*` requests to the backend

**Proof Artifacts:**
- `curl http://localhost:8080/actuator/health` returns `{"status":"UP"}` — demonstrates endpoint exists and responds
- Screenshot of browser hitting `/actuator/health` through the nginx proxy (port 3000) — demonstrates proxy forwarding works

---

### Unit 2: Production Spring Profile with PostgreSQL

**Purpose:** Wire a `prod` Spring profile that connects to PostgreSQL (RDS) instead of H2, so the production app has a persistent, real database.

**Functional Requirements:**
- The system shall define a `application-prod.properties` (or `application-prod.yml`) that sets the PostgreSQL datasource URL, username, and password via environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)
- The system shall run Flyway migrations automatically on startup against PostgreSQL
- The system shall NOT hardcode any database credentials in source code
- The system shall start successfully with `SPRING_PROFILES_ACTIVE=prod` when the three `DB_*` env vars are set
- The dev profile (H2) shall remain unchanged and continue to work locally

**Proof Artifacts:**
- App startup log showing `HikariPool` connecting to a PostgreSQL JDBC URL — demonstrates prod profile is active
- `curl /actuator/health` returning `{"status":"UP","components":{"db":{"status":"UP"}}}` — demonstrates database connectivity

---

### Unit 3: CI Pipeline (GitHub Actions)

**Purpose:** Run automated checks on every pull request so that broken code cannot be merged to main.

**Functional Requirements:**
- The system shall run a GitHub Actions workflow triggered on all pull requests targeting `main`
- The workflow shall run backend tests: `./mvnw test`
- The workflow shall run frontend tests: `npm test`
- The workflow shall run frontend lint: `npm run lint` (an ESLint script shall be added to `package.json` if not present)
- The workflow shall build the backend Docker image using `Dockerfile.backend`
- The workflow shall build the frontend Docker image using `frontend/Dockerfile`
- The workflow shall fail the pull request if any step exits non-zero
- All steps shall complete within a reasonable time (target: under 10 minutes)

**Proof Artifacts:**
- Screenshot of a GitHub PR with all CI checks passing (green checkmarks) — demonstrates the pipeline runs end-to-end
- Screenshot of a PR with a failing test showing the CI check blocked merge — demonstrates CI gates are enforced

---

### Unit 4: CD Pipeline and Live Deployment (AWS ECS Fargate)

**Purpose:** Automatically build, push, and deploy the app to AWS ECS Fargate on every merge to `main`, making it accessible at a public URL.

**Functional Requirements:**
- The system shall have two Amazon ECR repositories: one for the backend image, one for the frontend image
- The system shall have an ECS Fargate cluster with a task definition that runs both containers (frontend nginx + backend Spring Boot as sidecars in one task)
- The frontend nginx container shall proxy `/api/` and `/actuator/` requests to `localhost:8080` (the backend sidecar)
- The system shall have an Amazon RDS PostgreSQL instance (db.t3.micro, free tier) in the same VPC
- The system shall have an Application Load Balancer (ALB) with a public DNS name routing HTTP traffic to the ECS service
- The GitHub Actions CD workflow shall trigger on push to `main` (after CI passes)
- The CD workflow shall build and push both Docker images to ECR, tagged with the git SHA
- The CD workflow shall update the ECS task definition to use the new image tags and force a new deployment
- The following secrets shall be stored in GitHub Actions Secrets: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `ANTHROPIC_API_KEY`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- The ECS task shall receive `ANTHROPIC_API_KEY`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `SPRING_PROFILES_ACTIVE=prod` as environment variables
- The ALB health check shall target `GET /actuator/health` on the backend port (8080)

**Proof Artifacts:**
- Screenshot of the public ALB DNS URL loading the DORA dashboard in a browser — demonstrates successful end-to-end deployment
- Screenshot of GitHub Actions CD workflow run showing all steps green — demonstrates the automated pipeline works
- Screenshot of ECS service in AWS console showing running tasks — demonstrates containers are healthy in Fargate
- `curl http://<alb-url>/actuator/health` returning `{"status":"UP"}` — demonstrates the health check is reachable

## Non-Goals (Out of Scope)

1. **Custom domain / HTTPS**: No Route 53 domain or TLS certificate. The ALB public DNS (`*.elb.amazonaws.com`) is sufficient for a capstone demo.
2. **Infrastructure as Code (Terraform/CDK)**: The AWS resources (ECS cluster, RDS, ALB, ECR, IAM roles) will be created manually via the AWS console or CLI as a one-time setup. IaC is out of scope.
3. **Staging environment**: Only one environment (production). No separate staging or preview deployments.
4. **Auto-scaling**: The ECS service will run a fixed number of tasks (1). No autoscaling policies.
5. **Docker Compose changes**: The existing `docker-compose.yml` for local dev will not be changed.
6. **Monitoring and alerting**: No CloudWatch alarms, dashboards, or PagerDuty integration.
7. **Database migrations rollback**: Flyway migrations will run forward-only. No rollback scripts required.

## Design Considerations

No UI changes. This spec is entirely infrastructure and pipeline work. The app's user-facing behavior is unchanged.

The nginx configuration must be updated to proxy `/actuator/` to the backend, alongside the existing `/api/` proxy rule.

## Repository Standards

- GitHub Actions workflows live in `.github/workflows/` — the existing `claude.yml` is the reference format
- Secrets are never hardcoded; all credentials use `${{ secrets.SECRET_NAME }}` in workflows
- Workflow files follow the existing naming convention (lowercase, hyphen-separated)
- Backend tests run with `./mvnw test`; frontend tests run with `npm test` inside `frontend/`
- Commit messages follow the existing convention: `type: short description`
- TDD applies to the health check endpoint: write the test before adding the Actuator dependency

## Technical Considerations

- **Spring Boot Actuator**: Add `spring-boot-starter-actuator` to `pom.xml`. Restrict exposed endpoints in `application.properties` with `management.endpoints.web.exposure.include=health,info`.
- **Prod profile**: `application-prod.properties` sets `spring.datasource.url=${DB_URL}`, `spring.datasource.username=${DB_USERNAME}`, `spring.datasource.password=${DB_PASSWORD}`, and `spring.jpa.hibernate.ddl-auto=validate` (Flyway handles schema).
- **ECS sidecar pattern**: Both containers run in the same ECS task so nginx can proxy to `localhost:8080`. The backend is not exposed to the internet directly — only port 80 on the nginx container is mapped to the ALB target group.
- **ECR authentication in GitHub Actions**: Use `aws-actions/configure-aws-credentials@v4` with the `liatrio-forge` IAM credentials stored as GitHub Secrets, then `aws-actions/amazon-ecr-login@v2`.
- **ECS deployment in GitHub Actions**: Use `aws-actions/amazon-ecs-render-task-definition@v1` to inject new image URIs, then `aws-actions/amazon-ecs-deploy-task-definition@v1` to force a new deployment.
- **RDS connectivity**: The RDS instance must be in a security group that allows inbound port 5432 from the ECS task's security group.
- **Flyway + PostgreSQL**: The existing Flyway migrations (written for H2) must be verified to be PostgreSQL-compatible. H2 compatibility mode is not available in prod.
- **ESLint**: Add `eslint` and `@typescript-eslint/eslint-plugin` dev dependencies to `frontend/package.json` with a `lint` script if not already present.
- **AWS region**: `us-east-1` unless otherwise specified.

## Security Considerations

- `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` must be stored as GitHub Actions Secrets — never committed to the repository
- `ANTHROPIC_API_KEY`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` must also be GitHub Actions Secrets
- The IAM user for CI should follow least privilege: ECR push, ECS task definition registration, ECS service update, and nothing else
- RDS must not have a public endpoint — it should only be reachable from within the VPC
- Actuator endpoints beyond `health` and `info` must be disabled in production
- No proof artifacts (screenshots, curl output) should contain real credentials, API keys, or RDS connection strings

## Success Metrics

1. **CI on every PR**: Every pull request automatically runs and must pass all 4 checks (backend tests, frontend tests, lint, Docker builds) before merge is allowed
2. **CD on merge to main**: Every merge to `main` automatically deploys within 10 minutes, with zero manual steps
3. **Public URL**: The DORA dashboard is reachable at the ALB public DNS URL from any browser
4. **Health check passing**: `GET /actuator/health` returns HTTP 200 with `{"status":"UP"}` in both local and production environments

## Open Questions

1. Should the ALB listener use HTTP (port 80) only, or should we add a self-signed HTTPS cert for port 443 for a more realistic demo? (Recommendation: HTTP only — HTTPS requires a domain name for ACM certificate validation.)
2. Should the ECS task definition be committed to the repository as a JSON file, or generated dynamically by the CD workflow? (Recommendation: commit a base task definition JSON and let the GitHub Actions step patch image URIs at deploy time.)
