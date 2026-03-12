# 05 Tasks — CI/CD Pipeline and Deployment

## Relevant Files

### Backend
- `pom.xml` — add `spring-boot-starter-actuator` and `postgresql` JDBC driver dependencies
- `src/main/resources/application.properties` — restrict actuator endpoints to `health` and `info`
- `src/main/resources/application-prod.properties` — **NEW**: prod Spring profile wired to PostgreSQL via env vars
- `src/test/java/com/liatrio/dora/actuator/ActuatorHealthTest.java` — **NEW**: test that `GET /actuator/health` returns 200

### Frontend / Nginx
- `frontend/nginx/nginx.conf` — add `/actuator/` proxy location; change backend host to use `BACKEND_HOST` env var
- `frontend/Dockerfile` — update to run `envsubst` at startup so `BACKEND_HOST` is substituted into nginx.conf
- `frontend/package.json` — add `eslint`, `@typescript-eslint/eslint-plugin`, `@typescript-eslint/parser` devDependencies and a `lint` script
- `frontend/eslint.config.js` — **NEW**: ESLint flat config for TypeScript + React

### GitHub Actions
- `.github/workflows/ci.yml` — **NEW**: CI workflow triggered on all PRs to `main`
- `.github/workflows/cd.yml` — **NEW**: CD workflow triggered on push to `main`

### AWS / Deployment
- `ecs-task-definition.json` — **NEW**: base ECS task definition committed to the repo (image URIs are patched at deploy time)

### Notes
- Backend tests: `./mvnw test` from the repo root
- Frontend tests: `cd frontend && npm test`
- Frontend lint: `cd frontend && npm run lint`
- Follow the existing `.github/workflows/claude.yml` as a reference for workflow file format
- Never hardcode credentials — use `${{ secrets.SECRET_NAME }}` in workflows and `${ENV_VAR}` in properties files
- TDD: write the `ActuatorHealthTest` before adding the Actuator dependency to `pom.xml`

---

## Tasks

### [x] 1.0 Add Health Check Endpoint and Nginx Proxy Rule

#### 1.0 Proof Artifact(s)

- CLI: `curl http://localhost:8080/actuator/health` returns `{"status":"UP"}` — demonstrates actuator endpoint is live on the backend
- CLI: `curl http://localhost:3000/actuator/health` returns `{"status":"UP"}` — demonstrates nginx proxies `/actuator/` to the backend
- Test: `ActuatorHealthTest.java` passes — demonstrates endpoint responds correctly in the test suite

#### 1.0 Tasks

- [x] 1.1 Write a failing test `ActuatorHealthTest.java` in `src/test/java/com/liatrio/dora/actuator/` that performs `GET /actuator/health` and expects HTTP 200 (RED)
- [x] 1.2 Add `spring-boot-starter-actuator` to the `<dependencies>` block in `pom.xml`
- [x] 1.3 In `application.properties`, add `management.endpoints.web.exposure.include=health,info` to restrict which actuator endpoints are public
- [x] 1.4 Run `./mvnw test` and confirm `ActuatorHealthTest` now passes (GREEN)
- [x] 1.5 Add an `/actuator/` proxy location block to `frontend/nginx/nginx.conf` pointing to the backend — it should mirror the existing `/api/` block
- [x] 1.6 Start the app with `docker-compose up --build` and run `curl http://localhost:3000/actuator/health` — confirm it returns `{"status":"UP"}`

---

### [x] 2.0 Add Production Spring Profile with PostgreSQL Configuration

#### 2.0 Proof Artifact(s)

- Log: App startup output showing `HikariPool` connecting to a PostgreSQL JDBC URL (with password redacted) — demonstrates prod profile is active and DB connection is established
- CLI: `curl http://localhost:8080/actuator/health` returns `{"status":"UP","components":{"db":{"status":"UP"}}}` — demonstrates PostgreSQL connectivity is healthy
- Diff: `application-prod.properties` file showing `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}` placeholders — demonstrates no hardcoded credentials

#### 2.0 Tasks

- [x] 2.1 Add the PostgreSQL JDBC driver to `pom.xml` as a runtime dependency: `org.postgresql:postgresql` (no version needed — Spring Boot BOM manages it)
- [x] 2.2 In `application.properties`, add `management.endpoint.health.show-components=always` so the health response includes the `db` component status
- [x] 2.3 Create `src/main/resources/application-prod.properties` with the following settings (no hardcoded values — use env var placeholders):
  - `spring.datasource.url=${DB_URL}`
  - `spring.datasource.username=${DB_USERNAME}`
  - `spring.datasource.password=${DB_PASSWORD}`
  - `spring.datasource.driver-class-name=org.postgresql.Driver`
  - `spring.jpa.hibernate.ddl-auto=validate`
  - `spring.h2.console.enabled=false`
- [x] 2.4 Run `./mvnw test` to confirm no tests are broken by the new dependency or config
- [x] 2.5 Smoke-test the prod profile locally by starting a temporary PostgreSQL container and running the backend with `SPRING_PROFILES_ACTIVE=prod DB_URL=jdbc:postgresql://localhost:5432/doradb DB_USERNAME=dora DB_PASSWORD=dora ./mvnw spring-boot:run` — confirm the app starts and `/actuator/health` shows `db: UP`
- [x] 2.6 Add the three `DB_*` env vars and `SPRING_PROFILES_ACTIVE` to the `.env.example` file with placeholder values (e.g. `DB_URL=jdbc:postgresql://<host>:5432/doradb`)

---

### [x] 3.0 Add CI Pipeline (GitHub Actions)

#### 3.0 Proof Artifact(s)

- Screenshot: GitHub PR showing all CI checks passing (backend-test, frontend-test, frontend-lint, docker-build) with green checkmarks — demonstrates pipeline runs end-to-end on a real PR
- Screenshot: GitHub PR showing CI blocking merge when a test fails — demonstrates the pipeline enforces quality gates
- File: `.github/workflows/ci.yml` committed to the branch — demonstrates the pipeline definition exists

#### 3.0 Tasks

- [x] 3.1 Install ESLint in the frontend: run `cd frontend && npm install --save-dev eslint @typescript-eslint/eslint-plugin @typescript-eslint/parser eslint-plugin-react-hooks`
- [x] 3.2 Create `frontend/eslint.config.js` as a flat ESLint config that enables TypeScript and React Hooks rules
- [x] 3.3 Add a `lint` script to `frontend/package.json`: `"lint": "eslint src --ext .ts,.tsx --max-warnings 0"`
- [x] 3.4 Run `cd frontend && npm run lint` locally and fix any reported errors or warnings before continuing
- [x] 3.5 Create `.github/workflows/ci.yml` with four parallel jobs, each checking out the repo and running one check:
  - `backend-test`: Java 17, runs `./mvnw test`
  - `frontend-test`: Node 20, runs `cd frontend && npm ci && npm test`
  - `frontend-lint`: Node 20, runs `cd frontend && npm ci && npm run lint`
  - `docker-build`: builds `Dockerfile.backend` and `frontend/Dockerfile` using `docker build` (no push)
- [x] 3.6 Set the workflow trigger to `pull_request` targeting `main` and add a `push` trigger for `main` so CI also runs after merges
- [x] 3.7 Push the branch, open a PR to `main`, and confirm all four CI checks appear and pass in the GitHub UI
- [x] 3.8 To verify enforcement: temporarily break a test, push, confirm the `backend-test` check turns red, then revert the change

---

### [ ] 4.0 Provision AWS Infrastructure (ECR, ECS Fargate, RDS, ALB)

> ⚠️ This task is manual setup in the AWS console and CLI. Complete steps in order — each resource depends on the previous one. Use the `liatrio-forge` AWS profile for all CLI commands (`--profile liatrio-forge`).

#### 4.0 Proof Artifact(s)

- Screenshot: AWS ECR console showing two repositories (`dora-backend`, `dora-frontend`) — demonstrates image registries are ready
- Screenshot: AWS RDS console showing the PostgreSQL instance in `available` state — demonstrates the database is provisioned
- Screenshot: AWS ECS console showing the Fargate cluster and a service — demonstrates compute target exists
- Screenshot: AWS ALB console showing the load balancer in `active` state with a DNS name — demonstrates the public entry point exists
- CLI: `aws ecr get-login-password --profile liatrio-forge | docker login --username AWS --password-stdin <account-id>.dkr.ecr.<region>.amazonaws.com` succeeds — demonstrates push access works

#### 4.0 Tasks

- [ ] 4.1 **ECR**: Create two private ECR repositories in `us-east-1`: `dora-backend` and `dora-frontend` (AWS Console → ECR → Create repository, or use `aws ecr create-repository`)
- [ ] 4.2 **VPC + Security Groups**: Note the default VPC ID and its subnets. Create two security groups:
  - `dora-ecs-sg`: allows inbound port 80 from the ALB security group (create ALB SG first), outbound all
  - `dora-alb-sg`: allows inbound port 80 from `0.0.0.0/0`, outbound to `dora-ecs-sg` port 80
  - `dora-rds-sg`: allows inbound port 5432 from `dora-ecs-sg` only
- [ ] 4.3 **RDS**: Create a PostgreSQL 15 instance (db.t3.micro, free tier, 20 GB gp2). Settings: DB identifier `dora-db`, username `dora`, auto-generate password, place in the default VPC with `dora-rds-sg`. **Save the generated password** — you will need it for GitHub Secrets.
- [ ] 4.4 **ECS Cluster**: Create a new ECS cluster named `dora-cluster` using the Fargate launch type (AWS Console → ECS → Clusters → Create Cluster → choose AWS Fargate)
- [ ] 4.5 **IAM Task Execution Role**: Create an IAM role named `dora-ecs-execution-role` with the `AmazonECSTaskExecutionRolePolicy` managed policy attached. This lets ECS pull images from ECR and write logs to CloudWatch.
- [ ] 4.6 **ECS Task Definition (initial)**: In the AWS console, create a new task definition named `dora-task` with two containers:
  - Container 1 — `backend`: image `<backend-ecr-uri>:latest`, port 8080, env vars: `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ANTHROPIC_API_KEY`
  - Container 2 — `frontend`: image `<frontend-ecr-uri>:latest`, port 80, env var: `BACKEND_HOST=localhost`
  - Set the execution role to `dora-ecs-execution-role`, CPU 512, memory 1024
- [ ] 4.7 **ALB**: Create an Application Load Balancer named `dora-alb` (internet-facing, IPv4, default VPC, all AZs, `dora-alb-sg`). Create a target group `dora-tg` (IP type, HTTP, port 80). Add listener on port 80 forwarding to `dora-tg`. **Note the ALB DNS name** — this is the public URL.
- [ ] 4.8 **ECS Service**: Create a service in `dora-cluster` using the `dora-task` task definition, Fargate launch type, 1 desired task, assign `dora-ecs-sg`, attach to `dora-alb` / `dora-tg`, enable health check grace period of 60 seconds.
- [ ] 4.9 **IAM CI User**: Create an IAM user named `dora-ci` with the following inline policy (least privilege): `ecr:GetAuthorizationToken`, `ecr:BatchCheckLayerAvailability`, `ecr:PutImage`, `ecr:InitiateLayerUpload`, `ecr:UploadLayerPart`, `ecr:CompleteLayerUpload`, `ecs:RegisterTaskDefinition`, `ecs:UpdateService`, `ecs:DescribeServices`, `ecs:DescribeTaskDefinition`, `iam:PassRole` (limited to `dora-ecs-execution-role`). Generate an access key for this user.
- [ ] 4.10 **GitHub Secrets**: Add the following secrets to the GitHub repository (Settings → Secrets and variables → Actions):
  - `AWS_ACCESS_KEY_ID` — from the `dora-ci` IAM user access key
  - `AWS_SECRET_ACCESS_KEY` — from the `dora-ci` IAM user secret key
  - `AWS_REGION` — `us-east-1`
  - `AWS_ACCOUNT_ID` — your 12-digit AWS account ID
  - `ANTHROPIC_API_KEY` — your Anthropic API key
  - `DB_URL` — `jdbc:postgresql://<rds-endpoint>:5432/postgres`
  - `DB_USERNAME` — `dora`
  - `DB_PASSWORD` — the RDS password saved in step 4.3

---

### [x] 5.0 Add CD Pipeline and Verify Live Deployment

#### 5.0 Proof Artifact(s)

- Screenshot: GitHub Actions CD workflow run showing all steps green after a merge to `main` — demonstrates the automated pipeline ran successfully
- Screenshot: Browser loading the DORA dashboard at the ALB public DNS URL — demonstrates the app is live and accessible
- CLI: `curl http://<alb-dns>/actuator/health` returns `{"status":"UP"}` from the live deployment — demonstrates production health check is reachable
- Screenshot: ECS service in the AWS console showing running tasks at the new image tag — demonstrates containers are healthy in Fargate

#### 5.0 Tasks

- [x] 5.1 Update `frontend/nginx/nginx.conf` to replace the hardcoded hostname `backend` with the environment variable `${BACKEND_HOST}` in the `proxy_pass` directives (e.g. `proxy_pass http://${BACKEND_HOST}:8080;`). Rename the file to `nginx.conf.template`.
- [x] 5.2 Update `frontend/Dockerfile`: copy `nginx.conf.template` to `/etc/nginx/templates/nginx.conf.template`. The official `nginx:alpine` image automatically runs `envsubst` on files in `/etc/nginx/templates/` at startup — no `CMD` changes needed.
- [x] 5.3 Update `docker-compose.yml` to pass `BACKEND_HOST=backend` as an environment variable to the frontend service so local dev still works.
- [x] 5.4 Run `docker-compose up --build` locally and confirm the app still works with the template-based nginx config.
- [x] 5.5 Export the current ECS task definition to a JSON file in the repo root: `aws ecs describe-task-definition --task-definition dora-task --profile liatrio-forge --query taskDefinition > ecs-task-definition.json`. Remove the read-only fields (`taskDefinitionArn`, `revision`, `status`, `requiresAttributes`, `compatibilities`, `registeredAt`, `registeredBy`) from the JSON — the GitHub Actions step will re-register it.
- [x] 5.6 Create `.github/workflows/cd.yml` triggered on `push` to `main`. The workflow should have one job with these steps in order:
  1. Checkout code
  2. Configure AWS credentials using `aws-actions/configure-aws-credentials@v4` with `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION` secrets
  3. Log in to ECR using `aws-actions/amazon-ecr-login@v2`
  4. Build and push the backend image to ECR tagged with `${{ github.sha }}`
  5. Build and push the frontend image to ECR tagged with `${{ github.sha }}`
  6. Render a new task definition with the updated backend image using `aws-actions/amazon-ecs-render-task-definition@v1`
  7. Render a new task definition with the updated frontend image using another `aws-actions/amazon-ecs-render-task-definition@v1` step (chained from previous output)
  8. Deploy the rendered task definition to ECS using `aws-actions/amazon-ecs-deploy-task-definition@v1` targeting `dora-cluster` and the ECS service, with `wait-for-service-stability: true`
- [x] 5.7 Merge a branch to `main` and watch the CD workflow run in GitHub Actions → confirm all steps pass
- [x] 5.8 Open a browser and navigate to `http://<alb-dns-name>` — confirm the DORA dashboard loads
- [x] 5.9 Run `curl http://<alb-dns-name>/actuator/health` and confirm it returns `{"status":"UP"}`
