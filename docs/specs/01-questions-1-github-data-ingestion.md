# 01 Questions Round 1 - All Four Specs (Pre-Spec Gaps)

These questions cover the remaining gaps across all four specs before generation begins.
Please answer each question below (select one or more options, or add your own notes).

---

## Spec 01 – GitHub Data Ingestion

## 1. GitHub Scope

What is the minimum viable scope for the repo/org input?

- [X] (A) Single repository only (owner/repo) — org support is a stretch goal
- [ ] (B) Single repository or full GitHub org — both supported from day one
- [ ] (C) Other (describe)

## 2. Data Caching

Should the app store/cache GitHub API responses to avoid re-fetching on every page load?

- [X] (A) Yes — cache in H2 (dev) / PostgreSQL (prod) with a TTL (e.g., 15 min)
- [ ] (B) Yes — in-memory cache only (Redis or Caffeine), no DB needed
- [ ] (C) No — always fetch live from GitHub on demand, no cache
- [ ] (D) Other (describe)

## 3. GitHub API Rate Limiting

How should the app handle hitting GitHub's rate limit (5,000 req/hr for authenticated)?

- [ ] (A) Surface a clear error message to the user with time-until-reset
- [ ] (B) Automatic retry with backoff (transparent to user)
- [X] (C) Both — retry silently, surface error only if retries exhausted
- [ ] (D) Out of scope for now — just let it fail

---

## Spec 02 – DORA Metrics Engine

## 4. Change Failure Rate Data Source

GitHub doesn't have a native "incident" concept. How should CFR be calculated?

- [X] (A) PRs/commits with label `hotfix`, `revert`, or `bug` merged to main
- [X] (B) GitHub Issues labeled `incident` or `outage`, correlated to deployments
- [X] (C) Workflow runs with a specific name pattern (e.g., "rollback")
- [ ] (D) Skip CFR for MVP — surface it as "not enough data" placeholder
- [ ] (E) Other (describe)

## 5. MTTR Data Source

Mean Time to Restore — how should the app detect "incident started" and "incident resolved"?

- [X] (A) GitHub Issue open time → close time for issues labeled `incident`
- [X] (B) Failed deployment → next successful deployment time
- [X] (C) Skip MTTR for MVP — placeholder only
- [ ] (D) Other (describe)

## 6. Time Window Default

What time window should the dashboard default to on first load?

- [X] (A) Last 30 days
- [ ] (B) Last 90 days
- [ ] (C) Last 180 days
- [ ] (D) Other (describe)

---

## Spec 03 – Dashboard UI

## 7. DORA Performance Bands

Should each metric card show a performance band (Elite / High / Medium / Low) based on DORA benchmarks?

- [X] (A) Yes — color-coded badge on each metric card (green/yellow/orange/red)
- [ ] (B) No — just show the raw number, no judgment
- [ ] (C) Other (describe)

## 8. Chart Type for Trends

What chart type should show metric trends over the selected time window?

- [ ] (A) Line chart (continuous trend over time)
- [ ] (B) Bar chart (grouped by week or sprint)
- [X] (C) Both — line for frequency metrics, bar for time-based metrics
- [ ] (D) Other (describe)

---

## Spec 04 – AI Insights Panel

## 9. AI Insight Trigger

When should the AI insights be generated?

- [X] (A) Automatically on every dashboard load (after metrics are calculated)
- [ ] (B) On demand — user clicks "Generate Insights" button
- [ ] (C) Other (describe)

## 10. AI Streaming

Should the Claude API response stream in word-by-word (like a chat experience), or appear all at once?

- [X] (A) Stream in progressively (better UX, more impressive demo)
- [ ] (B) Show a loading spinner, then display all at once
- [ ] (C) Other (describe)

---

## Deployment (All Specs)

## 11. ECS Deployment Details

For the AWS ECS deployment, which services go in containers?

- [ ] (A) Two containers: Spring Boot backend + React frontend (served via nginx)
- [ ] (B) Three containers: backend + frontend + PostgreSQL (DB in ECS too)
- [X] (C) Two containers (backend + frontend) with RDS PostgreSQL managed separately
- [ ] (D) Only one container — backend serves the built React app as static files
- [ ] (E) Other (describe)

## 12. Proof Artifacts — What Counts as Done?

For the capstone demo, what proof do you want for each spec?

- [ ] (A) Working demo against a real GitHub repo (e.g., liatrio/liatrio or a public repo)
- [ ] (B) Passing unit + integration tests (CI green)
- [ ] (C) Deployed and accessible URL on ECS
- [X] (D) All of the above
- [ ] (E) Other (describe)
