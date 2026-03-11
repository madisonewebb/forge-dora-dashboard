# Spec 05 — CI/CD Deployment: Proof Artifacts

---

## Task 1.0 — Health Check Endpoint and Nginx Proxy Rule

### Test Results

`ActuatorHealthTest` — 2 tests, 0 failures:

```
[INFO] Running com.liatrio.dora.actuator.ActuatorHealthTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.644 s
```

Tests verify:
- `GET /actuator/health` → HTTP 200 with `{"status":"UP"}`
- `GET /actuator/health` → response includes `components.db.status = "UP"`

### Configuration Diff

`application.properties` additions:
```properties
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-components=always
```

`pom.xml` addition:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Nginx Proxy Rule

`frontend/nginx/nginx.conf.template` — `/actuator/` location added alongside `/api/`:
```nginx
location /actuator/ {
    proxy_pass http://${BACKEND_HOST}:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

---

## Task 2.0 — Production Spring Profile with PostgreSQL

### Configuration File

`src/main/resources/application-prod.properties` (no hardcoded credentials):
```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.h2.console.enabled=false
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

### .env.example Update

```
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://<rds-endpoint>:5432/postgres
DB_USERNAME=dora
DB_PASSWORD=your-rds-password-here
```

### Test Suite — All Passing

```
[WARNING] Tests run: 98, Failures: 0, Errors: 0, Skipped: 4
[INFO] BUILD SUCCESS
```

*(The 4 skipped tests are GitHub API integration tests that require live credentials — expected.)*

---

## Task 3.0 — CI Pipeline (GitHub Actions)

### ESLint — Clean Pass

```
> dora-dashboard-frontend@0.0.1 lint
> eslint src --ext .ts,.tsx --max-warnings 0

(no output = no errors or warnings)
```

### Frontend Tests — All Passing

```
Test Files  5 passed (5)
Tests  20 passed (20)
```

### CI Workflow File

`.github/workflows/ci.yml` — 4 parallel jobs:
- `backend-test` — `./mvnw test` on Java 17
- `frontend-test` — `npm test` on Node 20
- `frontend-lint` — `npm run lint` on Node 20
- `docker-build` — builds both `Dockerfile.backend` and `frontend/Dockerfile`

Triggers: `pull_request` targeting `main` and `push` to `main`.

---

## Task 4.0 — AWS Infrastructure (Manual Setup)

> ⚠️ This task requires manual execution in the AWS console/CLI.
> Proof artifacts (ECR, ECS, RDS, ALB screenshots) will be added here after infrastructure is provisioned.

**Checklist:**
- [ ] ECR: `dora-backend` and `dora-frontend` repositories created
- [ ] RDS: PostgreSQL db.t3.micro instance `dora-db` in `available` state
- [ ] ECS: `dora-cluster` Fargate cluster created
- [ ] ALB: `dora-alb` load balancer in `active` state with DNS name noted
- [ ] IAM: `dora-ci` user with least-privilege policy and access key generated
- [ ] GitHub Secrets: all 8 secrets added to repository

---

## Task 5.0 — CD Pipeline and Live Deployment

### nginx Template

`frontend/nginx/nginx.conf.template` — `${BACKEND_HOST}` env var used for backend hostname:
- Local dev (docker-compose): `BACKEND_HOST=backend` → resolves via Docker DNS
- ECS Fargate (sidecar): `BACKEND_HOST=localhost` → resolves via shared loopback

### Dockerfile Update

`frontend/Dockerfile` — uses nginx templates directory for `envsubst` at startup:
```dockerfile
COPY nginx/nginx.conf.template /etc/nginx/templates/default.conf.template
```

### ECS Task Definition

`ecs-task-definition.json` — base task definition committed to repo with:
- Two containers: `backend` (port 8080) and `frontend` (port 80)
- `BACKEND_HOST=localhost` for ECS sidecar networking
- Secrets sourced from AWS SSM Parameter Store
- CloudWatch logging configured

### CD Workflow File

`.github/workflows/cd.yml` — triggered on push to `main`:
1. Configure AWS credentials
2. ECR login
3. Build + push `dora-backend:<sha>` to ECR
4. Build + push `dora-frontend:<sha>` to ECR
5. Render task definition with new backend image
6. Render task definition with new frontend image
7. Deploy to `dora-cluster` / `dora-service`, wait for stability

> Live deployment proof (public URL screenshot, curl health check) will be added after Task 4.0 infrastructure is provisioned and first CD run completes.
