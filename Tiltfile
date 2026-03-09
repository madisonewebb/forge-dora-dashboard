# DORA Metrics Dashboard — Local Development
#
# Spec 01: single backend service via local_resource.
# Full multi-container Tilt setup (backend + React frontend) is added in Spec 03.
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
