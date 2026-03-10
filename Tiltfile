# DORA Metrics Dashboard — Local Development
#
# Spec 03: multi-container setup — backend + React frontend.
# Backend runs via Spring Boot maven plugin; frontend hot-reloads via Vite dev server.
#
# Usage: tilt up

local_resource(
    name = "backend",
    serve_cmd = "./mvnw spring-boot:run -Dspring.profiles.active=dev",
    deps = ["src/", "pom.xml"],
    readiness_probe = probe(
        http_get = http_get_action(port = 8080, path = "/actuator/health"),
        period_secs = 5,
        failure_threshold = 10,
    ),
    labels = ["backend"],
)

local_resource(
    name = "frontend",
    serve_cmd = "cd frontend && npm run dev -- --port 5173",
    deps = ["frontend/src/", "frontend/package.json", "frontend/vite.config.ts"],
    labels = ["frontend"],
)
