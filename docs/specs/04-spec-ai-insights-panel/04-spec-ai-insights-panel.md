# 04-spec-ai-insights-panel

## Introduction/Overview

This spec covers the AI Insights Panel — the advanced feature that sets this capstone apart. After metrics are calculated, the app automatically calls the Claude API (via Spring AI) and streams a plain-English analysis directly into the dashboard. Claude receives all four DORA metrics and their trend data and returns: a current-state summary, trend analysis (improving/declining/stable per metric), and specific, actionable recommendations the team can act on. The response streams word-by-word so the UI feels alive.

## Goals

- Automatically generate AI insights every time the dashboard loads with new metrics data
- Stream Claude's response progressively into the UI using Server-Sent Events (SSE)
- Produce three structured sections: current state summary, trend analysis, and actionable recommendations
- Ground Claude's analysis in the actual metric values and DORA benchmarks (not generic advice)
- Handle API errors (missing key, quota exceeded, timeout) gracefully without breaking the rest of the dashboard

## User Stories

**As a developer**, I want Claude to summarize my team's DORA metrics in plain English so that I can share the analysis with non-technical stakeholders without interpreting numbers myself.

**As a developer**, I want Claude to identify which metrics are improving, declining, or stable so that I can understand the trend story behind the numbers.

**As a developer**, I want Claude to give me specific, actionable recommendations so that I know what to do next to improve my team's delivery performance.

**As a developer**, I want the AI insights to stream in progressively so that I don't stare at a blank panel waiting for a slow API call.

**As a developer**, I want to see a clear message if AI insights are unavailable (API key not configured, quota exceeded) so that the rest of the dashboard still works without the AI panel.

## Demoable Units of Work

### Unit 1: Insights Backend — Claude API Integration via Spring AI

**Purpose:** Builds the server-side service that constructs a DORA-grounded prompt, calls Claude via Spring AI, and streams the response as SSE.

**Functional Requirements:**
- The system shall expose `GET /api/insights` with query parameters: `owner`, `repo`, `token`, `days` (same params as `/api/metrics`).
- The endpoint shall first call `MetricsService` to retrieve the current metrics (using the cache if available).
- The system shall construct a structured prompt containing:
  - The four metric values, units, and DORA band classifications
  - The direction of each metric's trend (calculated from the `timeSeries`: first-half average vs second-half average)
  - The selected time window
  - Instructions for Claude to produce three sections: **Summary**, **Trend Analysis**, and **Recommendations**
- The system shall call the Claude API using Spring AI's `ChatClient` with the `claude-sonnet-4-6` model.
- The endpoint shall return a `text/event-stream` response using Spring's `SseEmitter` (or `Flux<String>` with Spring WebFlux), streaming each token as it arrives from Claude.
- The system shall configure Spring AI with the Anthropic API key from environment variable `ANTHROPIC_API_KEY`.
- The system shall return HTTP 503 with `{ "error": "AI insights unavailable", "reason": "<message>" }` if the Anthropic API key is not configured or if the Claude API call fails after one retry.

**Proof Artifacts:**
- Unit test: `InsightsServiceTest` mocks the Spring AI `ChatClient` and asserts that the constructed prompt contains the metric values, DORA bands, and trend directions.
- Unit test: asserts that a missing `ANTHROPIC_API_KEY` returns a 503 response with the correct JSON body.
- Integration test: `InsightsControllerIntegrationTest` with a mocked `ChatClient` asserts that the SSE endpoint returns `Content-Type: text/event-stream` and emits multiple events.

---

### Unit 2: Prompt Engineering — Grounded, Structured Analysis

**Purpose:** Ensures Claude produces consistently structured, high-quality output grounded in the actual metric data rather than generic DevOps advice.

**Functional Requirements:**
- The system prompt shall instruct Claude to act as a DevOps engineering effectiveness advisor with expertise in DORA metrics.
- The user prompt shall include a structured data block with all four metrics in this format:
  ```
  Metric: Deployment Frequency
  Value: 2.3 deploys/day | Band: ELITE | Trend: IMPROVING (+18% over 30 days)

  Metric: Lead Time for Changes
  Value: 6.2 hours | Band: HIGH | Trend: STABLE (±5% over 30 days)
  ...
  ```
- The prompt shall explicitly request three labeled sections in the response: `## Summary`, `## Trend Analysis`, `## Recommendations`.
- The `## Recommendations` section shall contain exactly 3–5 bullet points, each actionable and specific to the metric values provided (not generic advice).
- The prompt shall include the DORA benchmark thresholds so Claude can reference them: e.g., "Elite Lead Time is < 1 hour; this team is at 6.2 hours (High band)."
- The system shall calculate trend direction from `timeSeries` data before building the prompt: compare the average of the first half of weeks to the second half; if delta > 10% label "IMPROVING" or "DECLINING"; otherwise "STABLE".

**Proof Artifacts:**
- Unit test: `PromptBuilderTest` with controlled metric inputs asserts that the resulting prompt string contains the metric values, band names, trend labels, and benchmark thresholds.
- Unit test: `TrendDirectionCalculatorTest` asserts correct IMPROVING / DECLINING / STABLE labels for controlled `timeSeries` inputs including edge cases (all zeros, single data point).

---

### Unit 3: Streaming UI — AI Insights Panel Component

**Purpose:** Renders the streaming Claude response in the dashboard with progressive text reveal and proper section formatting.

**Functional Requirements:**
- The system shall render an "AI Insights" panel below (or alongside) the metric cards, always visible once the dashboard loads.
- As soon as the `/api/metrics` response is received, the frontend shall immediately open an SSE connection to `/api/insights` with the same parameters.
- Tokens from the SSE stream shall be appended to the panel's text content in real time, giving a typewriter effect.
- The panel shall display a pulsing "Analyzing your metrics..." placeholder while awaiting the first token.
- Markdown headings in the response (`## Summary`, `## Trend Analysis`, `## Recommendations`) shall be rendered as styled section headers (not raw `##` characters).
- Bullet points in the `## Recommendations` section shall be rendered as an HTML unordered list.
- On stream completion (`EventSource` close), the panel shall display a subtle "Generated by Claude" attribution line at the bottom.
- If the backend returns HTTP 503, the panel shall display: "AI insights are currently unavailable." (non-blocking — the metric cards continue to work).
- The panel shall have a "Regenerate" button that closes the current SSE connection, clears the panel, and opens a new connection.

**Proof Artifacts:**
- Screenshot: the AI Insights panel mid-stream, showing partial text appearing progressively alongside the metric cards.
- Screenshot: the completed AI panel with all three sections rendered (Summary, Trend Analysis, Recommendations as a bullet list).
- Test: `InsightsPanel.test.tsx` mocks `EventSource` and asserts that tokens are appended progressively, the "Analyzing" placeholder disappears after first token, and the 503 error state renders the unavailability message.

---

### Unit 4: End-to-End Integration and Demo Readiness

**Purpose:** Validates the full flow works against a real GitHub repo with real Claude API calls, as a capstone demo proof.

**Functional Requirements:**
- The system shall successfully generate AI insights for any public GitHub repository with at least 5 deployments or merged PRs in the last 30 days.
- The streamed response shall contain all three sections (`Summary`, `Trend Analysis`, `Recommendations`) in every successful call.
- The full dashboard load (metrics + streaming insights complete) shall finish within 30 seconds on a standard network connection.
- The `ANTHROPIC_API_KEY` shall be injected via environment variable in both Tilt (local) and ECS (prod) and shall never appear in any source file, commit, or log.

**Proof Artifacts:**
- Screen recording (or screenshots): full dashboard loaded with real GitHub data, AI insights panel fully rendered with all three sections, demonstrating the live streaming effect.
- CI passing: GitHub Actions workflow running `./mvnw test` (backend) and `npm test` (frontend) both green.
- Live URL: the app deployed to ECS is accessible at a public URL, and the AI insights panel generates successfully against a demo repo (e.g., `liatrio/liatrio`).

## Non-Goals (Out of Scope)

1. **Chat interface**: Users cannot ask follow-up questions. Insights are one-shot generated per dashboard load.
2. **Saving or sharing insights**: Generated text is not persisted; refreshing regenerates it.
3. **Custom prompts**: Users cannot edit the prompt or change the analysis focus.
4. **Model selection**: The model is hardcoded to `claude-sonnet-4-6`; no UI to switch models.
5. **Insights history or comparison**: No logging of past AI responses.
6. **Cost tracking / token counting**: No display of Claude API usage or cost.

## Design Considerations

- The AI Insights panel spans the full width below the 2×2 metric card grid.
- Background: a very subtle light-blue tint (e.g., `#f0f7ff`) to visually distinguish it from the metric cards.
- A small Claude/Anthropic logo or sparkle icon next to the "AI Insights" heading signals the AI provenance.
- Section headers (`## Summary`, `## Trend Analysis`, `## Recommendations`) render as `<h3>` styled in the same weight as metric card titles.
- The "Regenerate" button is secondary-styled (outlined, not filled) positioned top-right of the panel.
- The pulsing "Analyzing your metrics..." placeholder uses the same skeleton animation as the metric cards.

## Repository Standards

- Spring AI `ChatClient` for all Claude API calls — do not use the Anthropic SDK directly
- Spring AI dependency: `spring-ai-anthropic-spring-boot-starter`
- SSE streaming: use `SseEmitter` in Spring MVC or `Flux<ServerSentEvent<String>>` if the project uses WebFlux
- `InsightsService` handles prompt construction and Claude API calls; `InsightsController` handles SSE emission
- Frontend: use the browser native `EventSource` API (no polling library needed)
- Markdown rendering in React: use `react-markdown` library
- TDD applies: prompt builder and trend calculator are pure functions and must have unit tests before implementation

## Technical Considerations

- Spring AI configuration: set `spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY}` in `application.properties`; the key is injected at runtime from the environment.
- Use `claude-sonnet-4-6` model ID (current Sonnet model); configure via `spring.ai.anthropic.chat.options.model=claude-sonnet-4-6`.
- Spring AI's `ChatClient` streaming returns a `Flux<String>` — bridge this to `SseEmitter` by subscribing and calling `emitter.send()` on each token.
- The `/api/insights` endpoint should set `Cache-Control: no-store` since each response is generated fresh.
- SSE connection timeout: set `SseEmitter` timeout to 60 seconds (Claude streaming typically completes in 10–20 seconds for this prompt length).
- Trend direction calculation: a pure Java utility class `TrendDirectionCalculator` — no Spring context needed, making it easy to unit test.

## Security Considerations

- `ANTHROPIC_API_KEY` must be injected exclusively via environment variable; it must never appear in any source file, `application.properties` committed to git, or any log output.
- Add `ANTHROPIC_API_KEY` to `.gitignore`-protected `.env` files for local dev; inject via ECS task definition secrets for production.
- The GitHub PAT passed to `/api/insights` is forwarded to `MetricsService` only and is never logged or stored.
- The Claude API prompt must not include the GitHub PAT — only metric values and repo name.

## Success Metrics

1. **AI generation**: The insights panel successfully generates all three sections (Summary, Trend Analysis, Recommendations) for a real GitHub repo in 100% of successful API calls.
2. **Streaming UX**: The first token appears within 3 seconds of the SSE connection opening.
3. **Prompt quality**: The `## Recommendations` section contains 3–5 specific, non-generic bullet points referencing the actual metric values and bands (manually reviewed in demo).
4. **Graceful degradation**: A missing or invalid `ANTHROPIC_API_KEY` shows the "AI insights unavailable" message without affecting metric card rendering.
5. **Deployed**: The AI insights panel works in the live ECS deployment.

## Open Questions

No open questions at this time.
