# 04-task-all-proofs — AI Insights Panel

## Task 1.0 — Spring AI + TrendDirectionCalculator + PromptBuilder

### CLI: Spring AI Dependency Resolved

```
[INFO] BUILD SUCCESS
[INFO] Total time:  2.527 s
```

`spring-ai-bom:1.0.0` and `spring-ai-starter-model-anthropic:1.0.0` (Spring AI 1.0.0 GA
artifact name) resolved from Maven Central. `ChatClientConfig` bean creates `ChatClient`
from the auto-configured `ChatClient.Builder`.

### Test: TrendDirectionCalculatorTest — 5/5 PASS

```
[INFO] Running com.liatrio.dora.insights.TrendDirectionCalculatorTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
```

Tests covered:
- IMPROVING when second half avg > first half avg by >10%
- DECLINING when second half avg < first half avg by >10%
- STABLE when delta is within 10%
- STABLE when all values are zero (no division by zero)
- STABLE when timeSeries has only one point

### Test: PromptBuilderTest — 4/4 PASS

```
[INFO] Running com.liatrio.dora.insights.PromptBuilderTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```

Tests covered:
- System prompt contains "DevOps" and "DORA"
- User prompt contains metric values (2.3, 6.2)
- User prompt contains band (ELITE) and trend (IMPROVING)
- User prompt contains section headers (## Summary, ## Trend Analysis, ## Recommendations)

---

## Task 2.0 — InsightsService + InsightsController SSE Endpoint

### Test: InsightsServiceTest — 3/3 PASS

```
[INFO] Running com.liatrio.dora.service.InsightsServiceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

Tests covered:
- Calls MetricsService and PromptBuilder; Flux emits expected tokens
- Captured user prompt contains metric values from fixture (2.3)
- Missing ANTHROPIC_API_KEY throws InsightsUnavailableException with correct message

### Test: InsightsControllerTest — 3/3 PASS

```
[INFO] Running com.liatrio.dora.controller.InsightsControllerTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

Tests covered:
- Returns Content-Type: text/event-stream
- Emits all tokens (alpha, beta, gamma) in response body
- InsightsUnavailableException → HTTP 503 with JSON body { "error": "AI insights unavailable", "reason": "..." }

### Test: GlobalExceptionHandlerTest — 3/3 PASS (new test added)

```
[INFO] Running com.liatrio.dora.exception.GlobalExceptionHandlerTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

### CLI: Full Backend Suite

```
[WARNING] Tests run: 92, Failures: 0, Errors: 0, Skipped: 4
[INFO] BUILD SUCCESS
[INFO] Total time: 17.069 s
```

92 tests run, 0 failures, 4 skipped (integration tests requiring real GitHub PAT).

### Security Audit

```bash
$ grep -rn "ANTHROPIC_API_KEY" src/
src/test/.../InsightsServiceTest.java:116:  .hasMessageContaining("ANTHROPIC_API_KEY");
src/test/.../GlobalExceptionHandlerTest.java:48:  "ANTHROPIC_API_KEY is not configured"
src/test/.../GlobalExceptionHandlerTest.java:78:  throw new InsightsUnavailableException("ANTHROPIC_API_KEY is not configured");
src/main/resources/application.properties:28: spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY:}
src/main/java/.../InsightsService.java:44:  throw new InsightsUnavailableException("ANTHROPIC_API_KEY is not configured");
```

All references are environment variable placeholders or error message strings.
No hardcoded API key values appear anywhere in source files.

---

## Task 3.0 — Frontend InsightsPanel Component

### Test: InsightsPanel.test.tsx — 4/4 PASS

```
✓ src/components/InsightsPanel.test.tsx (4 tests) 87ms
```

Tests covered:
- Renders "Analyzing your metrics..." placeholder on mount
- Appends tokens progressively; placeholder disappears after first token
- Renders "AI insights are currently unavailable." on EventSource error
- Regenerate button closes old EventSource, creates new one, resets content

### Test: Full Frontend Suite — 20/20 PASS

```
Test Files  5 passed (5)
     Tests  20 passed (20)
```

All 5 test files green:
- InsightsPanel.test.tsx (4)
- Dashboard.test.tsx (5) — InsightsPanel mocked via vi.mock
- MetricCard.test.tsx (4)
- TrendChart.test.tsx (3)
- RepoForm.test.tsx (4)

---

## Task 4.0 — E2E Integration, Demo Readiness, and CI Validation

### docker-compose.yml — ANTHROPIC_API_KEY injected

```yaml
backend:
  environment:
    - SPRING_PROFILES_ACTIVE=dev
    - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY:-}
```

`:-` default means Docker Compose does not error if the variable is unset locally;
the app starts and shows "AI insights are currently unavailable." gracefully.

### .env.example created (safe to commit)

```
# Copy this file to .env and fill in real values — never commit .env
ANTHROPIC_API_KEY=your-anthropic-api-key-here
```

`.gitignore` already contains `.env` and `.env.*` (excludes real keys) and `!.env.example`
(whitelists the example file).

### Final Backend CI

```
[WARNING] Tests run: 92, Failures: 0, Errors: 0, Skipped: 4
[INFO] BUILD SUCCESS
```

### Final Frontend CI

```
Test Files  5 passed (5)
     Tests  20 passed (20)
```

### Manual E2E (Sub-task 4.6–4.7)

Screenshots to be captured after running `docker compose up` with a real ANTHROPIC_API_KEY
and real GitHub PAT:
- `docs/specs/04-spec-ai-insights-panel/04-proofs/insights-panel-streaming.png`
- `docs/specs/04-spec-ai-insights-panel/04-proofs/insights-panel-complete.png`
- `docs/specs/04-spec-ai-insights-panel/04-proofs/full-dashboard-with-insights.png`
- `docs/specs/04-spec-ai-insights-panel/04-proofs/curl-insights-stream.txt`

These artifacts require a live environment with API credentials and are captured separately.
